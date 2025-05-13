package ppke.server.model;

import ppke.common.dto.VoteData;
import ppke.common.model.PollType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Scale (skála) típusú szavazás implementációja.
 * A felhasználók egy vagy több szempontot értékelhetnek egy megadott numerikus skálán.
 * Az eredmények az egyes szempontok átlagos értékelését mutatják.
 */
public class ScalePoll extends Poll implements Serializable {
    /** Szerializációs verzióazonosító. */
    private static final long serialVersionUID = 1L;

    /** A skálán értékelendő szempontok listája (nem módosítható a létrehozás után). Max 5 elem. */
    private final List<String> aspects;
    /** A skála megengedett minimum értéke. */
    private final int scaleMin;
    /** A skála megengedett maximum értéke. */
    private final int scaleMax;

    /**
     * Az egyes szempontok átlagos értékelését tároló map (Szempont -> Átlagérték).
     * {@link ConcurrentHashMap} a szálbiztos műveletekhez.
     */
    private Map<String, Double> averageRatings;

    /** Maximálisan megengedett szempontok száma. */
    private static final int MAX_ASPECTS = 5;
    /** Minimálisan kötelező szempontok száma. */
    private static final int MIN_ASPECTS = 1;


    /**
     * Konstruktor Scale szavazáshoz.
     * @param creatorUsername Létrehozó felhasználóneve.
     * @param name Szavazás neve.
     * @param question Feltett kérdés.
     * @param joinCode Csatlakozási kód (nagybetűs).
     * @param aspects A skálán értékelendő szempontok listája. Nem lehet null, legalább {@value #MIN_ASPECTS} és legfeljebb {@value #MAX_ASPECTS} egyedi, nem üres szempontot kell tartalmaznia.
     * @param scaleMin A skála minimum értéke.
     * @param scaleMax A skála maximum értéke. (scaleMin < scaleMax feltételnek teljesülnie kell)
     * @throws IllegalArgumentException Ha a szempontok vagy a skálaértékek érvénytelenek.
     */
    public ScalePoll(String creatorUsername, String name, String question, String joinCode, List<String> aspects, int scaleMin, int scaleMax) {
        super(creatorUsername, name, question, joinCode);

        Objects.requireNonNull(aspects, "A szempontok listája nem lehet null.");
        List<String> validAspects = aspects.stream()
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .collect(Collectors.toList());

        if (validAspects.size() < MIN_ASPECTS || validAspects.size() > MAX_ASPECTS) {
            throw new IllegalArgumentException("A szempontok száma " + MIN_ASPECTS + " és " + MAX_ASPECTS + " között kell legyen. Megadott érvényes szempontok: " + validAspects.size());
        }
        if (scaleMin >= scaleMax) {
            throw new IllegalArgumentException("A skála minimum értékének (" + scaleMin + ") kisebbnek kell lennie a maximum értéknél (" + scaleMax + ").");
        }

        this.aspects = new CopyOnWriteArrayList<>(validAspects); // Defenzív másolat és szálbiztos lista
        this.scaleMin = scaleMin;
        this.scaleMax = scaleMax;
        this.averageRatings = new ConcurrentHashMap<>();
        System.out.println("Poll ("+joinCode+"): Scale poll created. Aspects: " + this.aspects + ", Range: [" + scaleMin + "-" + scaleMax + "]");
    }

    /**
     * {@inheritDoc}
     * @return Mindig {@link PollType#SCALE}.
     */
    @Override
    public PollType getPollType() {
        return PollType.SCALE;
    }

    /**
     * {@inheritDoc}
     * Ellenőrzi, hogy a {@link VoteData} tartalmaz-e érvényes értékeléseket (`getAspectRatings()`)
     * az összes definiált szempontra, és hogy ezek az értékelések a megadott skálán belül vannak-e.
     * @param voteData A validálandó szavazat.
     * @return true, ha a szavazat érvényes, false egyébként.
     */
    @Override
    public boolean isValidVote(VoteData voteData) {
        if (voteData == null || voteData.getAspectRatings() == null) {
            System.err.println("Poll ("+joinCode+"): Scale vote rejected. VoteData or aspectRatings is null.");
            return false;
        }
        Map<String, Integer> ratings = voteData.getAspectRatings();

        if (ratings.size() != aspects.size()) {
            System.err.println("Poll ("+joinCode+"): Scale vote rejected. Incorrect number of aspects rated. Expected: " + aspects.size() + ", Got: " + ratings.size());
            return false;
        }

        for (String aspect : aspects) {
            if (!ratings.containsKey(aspect)) {
                System.err.println("Poll ("+joinCode+"): Scale vote rejected. Missing rating for aspect: '" + aspect + "'");
                return false;
            }
            Integer value = ratings.get(aspect);
            if (value == null || value < scaleMin || value > scaleMax) {
                System.err.println("Poll ("+joinCode+"): Scale vote rejected. Invalid rating for aspect '" + aspect + "': " + value + ". Valid range: [" + scaleMin + "-" + scaleMax + "]");
                return false;
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     * Kiszámítja az egyes szempontok átlagos értékelését a leadott érvényes szavazatok alapján
     * és eltárolja az `averageRatings` map-ben. Ha egy szempontra nincs szavazat, az átlaga 0.0 lesz.
     * A számítás írási lock alatt fut.
     */
    @Override
    protected void calculateResults() {
        Map<String, List<Integer>> allRatingsForAspects = new HashMap<>();
        aspects.forEach(aspect -> allRatingsForAspects.put(aspect, new ArrayList<>()));

        synchronized (votes) {
            for (VoteData vote : votes) {
                if (vote != null && vote.getAspectRatings() != null) {
                    Map<String, Integer> currentVoteRatings = vote.getAspectRatings();
                    for (String aspect : aspects) {
                        Integer rating = currentVoteRatings.get(aspect);
                        if (rating != null) {
                            allRatingsForAspects.get(aspect).add(rating);
                        }
                    }
                }
            }
        }

        Map<String, Double> newAverageRatings = new ConcurrentHashMap<>();
        for (String aspect : aspects) {
            List<Integer> aspectSpecificRatings = allRatingsForAspects.get(aspect);
            if (aspectSpecificRatings.isEmpty()) {
                newAverageRatings.put(aspect, 0.0);
            } else {
                double average = aspectSpecificRatings.stream()
                        .mapToInt(Integer::intValue)
                        .average()
                        .orElse(0.0);
                newAverageRatings.put(aspect, average);
            }
        }
        this.averageRatings = newAverageRatings;
        System.out.println("Poll (" + joinCode + "): ScalePoll results recalculated. Averages: " + averageRatings);
    }

    /**
     * {@inheritDoc}
     * Visszaadja a formázott eredményeket: a szempontokhoz tartozó átlagértékeléseket
     * tartalmazó map másolatát. Olvasási lock alatt fut.
     * @return {@code Map<String, Double>} a szempontok és átlagértékeléseik, vagy üres map. Soha nem null.
     */
    @Override
    public Object getFormattedResults() {
        return new HashMap<>(averageRatings);
    }

    /**
     * {@inheritDoc}
     * Törli a kiszámított átlagértékeléseket (az `averageRatings` map tartalmát).
     * Írási lock alatt fut.
     */
    @Override
    protected void clearCalculatedResults() {
        if (averageRatings != null) {
            averageRatings.clear();
        }
        System.out.println("Poll (" + joinCode + "): ScalePoll calculated results cleared.");
    }

    /** @return A szempontok listájának másolata. */
    public List<String> getAspects() {
        return new ArrayList<>(aspects);
    }

    /** @return A skála minimuma. */
    public int getScaleMin() {
        return scaleMin;
    }

    /** @return A skála maximuma. */
    public int getScaleMax() {
        return scaleMax;
    }
}
