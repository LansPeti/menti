package ppke.server.model;

import ppke.common.dto.VoteData; // Importáljuk, hogy a metódus szignatúrák működjenek
import ppke.common.model.PollType; // Importáljuk, hogy a metódus szignatúrák működjenek
import java.io.Serializable; // Implementáljuk, hogy az ősosztály szerializálható maradjon
import java.util.Collections; // Import Collections
import java.util.HashMap; // Import HashMap
import java.util.List; // Import List
import java.util.Map; // Import Map

/**
 * Scale (skála) típusú szavazás implementációjának helye.
 * **FIGYELEM: Ez a típus a projekt jelenlegi állapotában NINCS IMPLEMENTÁLVA.**
 * Az osztály csak egy vázlat, hogy a kód forduljon, de funkcionalitást nem tartalmaz.
 */
public class ScalePoll extends Poll implements Serializable {
    /** Szerializációs verzióazonosító. */
    private static final long serialVersionUID = 1L;

    // TODO: Szükséges mezők hozzáadása:
    // /** A skálán értékelendő szempontok listája. */
    // private final List<String> aspects;
    // /** A skála megengedett minimum értéke. */
    // private final int scaleMin;
    // /** A skála megengedett maximum értéke. */
    // private final int scaleMax;
    // /** Az egyes szempontok átlagos értékelését tároló map. */
    // private Map<String, Double> averageRatings;

    /**
     * Konstruktor (NINCS IMPLEMENTÁLVA).
     * @param creatorUsername Létrehozó.
     * @param name Név.
     * @param question Kérdés.
     * @param joinCode Kód.
     * // @param aspects Szempontok.
     * // @param scaleMin Min.
     * // @param scaleMax Max.
     */
    public ScalePoll(String creatorUsername, String name, String question, String joinCode /*, List<String> aspects, int scaleMin, int scaleMax */) {
        super(creatorUsername, name, question, joinCode);
        // TODO: Mezők inicializálása, validálása
        // this.aspects = new CopyOnWriteArrayList<>(aspects);
        // this.scaleMin = scaleMin;
        // this.scaleMax = scaleMax;
        // this.averageRatings = new ConcurrentHashMap<>();
        System.err.println("FIGYELEM: ScalePoll nincs teljesen implementálva!");
        throw new UnsupportedOperationException("Scale poll type is not implemented yet."); // Dobjunk hibát, ha valaki mégis példányosítaná
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
     * (NINCS IMPLEMENTÁLVA) Ellenőrizné, hogy a {@link VoteData} tartalmaz-e érvényes
     * értékeléseket (`getAspectRatings()`) a definiált szempontokra a megadott skálán belül.
     * @param voteData A validálandó szavazat.
     * @return Mindig false.
     */
    @Override
    public boolean isValidVote(VoteData voteData) {
        System.err.println("ScalePoll.isValidVote - NEM IMPLEMENTÁLT");
        // TODO: Implementálni az ellenőrzést
        return false; // Placeholder
    }

    /**
     * {@inheritDoc}
     * (NINCS IMPLEMENTÁLVA) Kiszámítaná az egyes szempontok átlagos értékelését
     * a leadott szavazatok alapján és eltárolná az `averageRatings` map-ben.
     */
    @Override
    protected void calculateResults() {
        System.err.println("ScalePoll.calculateResults - NEM IMPLEMENTÁLT");
        // TODO: Implementálni az átlagszámítást
    }

    /**
     * {@inheritDoc}
     * (NINCS IMPLEMENTÁLVA) Visszaadná az `averageRatings` map másolatát.
     * @return Jelenleg egy üres map.
     */
    @Override
    public Object getFormattedResults() {
        System.err.println("ScalePoll.getFormattedResults - NEM IMPLEMENTÁLT");
        // TODO: Visszaadni az 'averageRatings' map másolatát
        return new HashMap<String, Double>(); // Placeholder
    }

    /**
     * {@inheritDoc}
     * (NINCS IMPLEMENTÁLVA) Törölné az `averageRatings` map tartalmát.
     */
    @Override
    protected void clearCalculatedResults() {
        System.err.println("ScalePoll.clearCalculatedResults - NEM IMPLEMENTÁLT");
        // TODO: Törölni az 'averageRatings' tartalmát
        // if (averageRatings != null) averageRatings.clear();
    }

    // --- Specifikus Getterek (ha kellenek) ---
    /** @return A szempontok listája (NINCS IMPLEMENTÁLVA). */
    public List<String> getAspects() {
        // TODO: Implementálni, ha kell
        return Collections.emptyList(); // Placeholder
    }
    /** @return A skála minimuma (NINCS IMPLEMENTÁLVA). */
    public int getScaleMin() {
        // TODO: Implementálni, ha kell
        return 0; // Placeholder
    }
    /** @return A skála maximuma (NINCS IMPLEMENTÁLVA). */
    public int getScaleMax() {
        // TODO: Implementálni, ha kell
        return 0; // Placeholder
    }
}
