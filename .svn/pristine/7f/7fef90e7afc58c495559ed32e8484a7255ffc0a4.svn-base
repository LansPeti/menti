package ppke.server.model;

import ppke.common.dto.VoteData;
import ppke.common.model.PollState;
import ppke.common.model.PollType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Multiple Choice (feleletválasztós) típusú szavazás implementációja.
 * A felhasználók előre megadott opciók közül választhatnak egyet.
 * Tárolja az opciókat és az egyes opciókra leadott szavazatok számát.
 */
public class MultipleChoicePoll extends Poll {
    /** Szerializációs verzióazonosító. */
    private static final long serialVersionUID = 1L;

    /**
     * A válaszlehetőségek listája. A lista tartalma szerkesztéskor módosítható
     * ({@link #setOptions(List)}), ami csak CLOSED állapotban lehetséges.
     * {@link CopyOnWriteArrayList} használata a szálbiztos olvasáshoz és a ritka íráshoz.
     */
    private final List<String> options;

    /**
     * Az egyes opciókra leadott szavazatok számát tároló map (Opció -> Szavazatszám).
     * {@link ConcurrentHashMap} a szálbiztos műveletekhez.
     */
    private Map<String, Integer> voteCounts;

    /** Maximálisan megengedett válaszlehetőségek száma. */
    private static final int MAX_OPTIONS = 10;

    /**
     * Konstruktor Multiple Choice szavazáshoz.
     * @param creatorUsername Létrehozó felhasználóneve.
     * @param name Szavazás neve.
     * @param question Feltett kérdés.
     * @param joinCode Csatlakozási kód (nagybetűs).
     * @param initialOptions A válaszlehetőségek listája. Nem lehet null, és legalább egy érvényes (nem üres, nem blank) opciót kell tartalmaznia. Max {@value #MAX_OPTIONS} opció engedélyezett. Az üres/null/duplikált elemek kiszűrésre kerülnek.
     * @throws IllegalArgumentException Ha az `initialOptions` lista érvénytelen.
     */
    public MultipleChoicePoll(String creatorUsername, String name, String question, String joinCode, List<String> initialOptions) {
        super(creatorUsername, name, question, joinCode);

        if (initialOptions == null) {
            throw new IllegalArgumentException("Az opciók listája nem lehet null Multiple Choice szavazásnál.");
        }
        // Érvényes opciók kiszűrése: nem null, nem üres, egyedi, max limit
        List<String> validOptions = initialOptions.stream()
                .filter(s -> s != null && !s.isBlank())
                .distinct() // Duplikátumok eltávolítása
                .limit(MAX_OPTIONS) // Max limit alkalmazása
                .collect(Collectors.toList());

        if (validOptions.isEmpty()) {
            throw new IllegalArgumentException("Legalább egy érvényes (nem üres) opció megadása kötelező.");
        }
        // Defenzív másolat készítése és szálbiztos tárolás
        this.options = new CopyOnWriteArrayList<>(validOptions);

        // ConcurrentHashMap használata az eredmények tárolására
        this.voteCounts = new ConcurrentHashMap<>();
        // Inicializáljuk a számlálókat nullára minden érvényes opcióhoz
        this.options.forEach(option -> voteCounts.put(option, 0));
        System.out.println("Poll ("+joinCode+"): MC poll created with options: " + this.options);
    }

    /**
     * {@inheritDoc}
     * @return Mindig {@link PollType#MULTIPLE_CHOICE}.
     */
    @Override
    public PollType getPollType() {
        return PollType.MULTIPLE_CHOICE;
    }

    /**
     * {@inheritDoc}
     * Ellenőrzi, hogy a {@link VoteData} tartalmaz-e kiválasztott opciót (`getSelectedOption()`),
     * és hogy ez az opció szerepel-e az érvényes válaszlehetőségek ({@link #options}) között.
     * @param voteData A validálandó szavazat.
     * @return true, ha a szavazat érvényes, false egyébként.
     */
    @Override
    public boolean isValidVote(VoteData voteData) {
        if (voteData == null || voteData.getSelectedOption() == null) {
            return false; // Nincs kiválasztott opció
        }
        String selectedOption = voteData.getSelectedOption();
        // CopyOnWriteArrayList contains művelete szálbiztos
        boolean valid = options.contains(selectedOption);
        if(!valid) {
            System.err.println("Poll ("+joinCode+"): MC vote rejected. Invalid option: '" + selectedOption + "'. Valid options: " + options);
        }
        return valid;
    }

    /**
     * {@inheritDoc}
     * Kiszámítja és eltárolja az egyes opciókra leadott szavazatok számát.
     * Nullázza a korábbi számlálókat az *aktuális* opciók alapján, majd újra végigiterál
     * a leadott érvényes szavazatokon.
     * A számítás írási lock alatt fut.
     */
    @Override
    protected void calculateResults() {
        // Írási lock alatt fut az ősosztályból.
        Map<String, Integer> newCounts = new ConcurrentHashMap<>();
        // Az aktuális options lista alapján inicializálunk nullára
        // Ez fontos, ha szerkesztés után változtak az opciók
        options.forEach(option -> newCounts.put(option, 0));

        // Majd megszámoljuk az érvényes szavazatokat a 'votes' listából
        synchronized (votes) { // Szinkronizálás a votes listára az iteráció idejére
            for (VoteData vote : votes) {
                if (vote != null && vote.getSelectedOption() != null) {
                    String selected = vote.getSelectedOption();
                    // Csak akkor növeljük a számlálót, ha az opció még létezik az aktuális listában
                    // A computeIfPresent atomikusan kezeli a növelést, csak létező kulcsra fut le
                    newCounts.computeIfPresent(selected, (key, count) -> count + 1);
                }
            }
        }
        // Atomikusan lecseréljük a régi számlálókat az újakra
        this.voteCounts = newCounts;
        System.out.println("Poll (" + joinCode + "): MultipleChoice results recalculated.");
    }

    /**
     * {@inheritDoc}
     * Visszaadja a formázott eredményeket: az opciókhoz tartozó szavazatszámokat
     * tartalmazó map másolatát. Olvasási lock alatt fut.
     * @return {@code Map<String, Integer>} az opciók és szavazatszámaik, vagy üres map. Soha nem null.
     */
    @Override
    public Object getFormattedResults() {
        // Olvasási lock alatt fut az ősosztályból.
        return new HashMap<>(voteCounts); // Védelmi másolat
    }

    /**
     * {@inheritDoc}
     * Törli a kiszámított szavazatszámokat (nullázza a map értékeit az aktuális opciókhoz).
     * Írási lock alatt fut.
     */
    @Override
    protected void clearCalculatedResults() {
        // Írási lock alatt fut az ősosztályból.
        Map<String, Integer> clearedCounts = new ConcurrentHashMap<>();
        options.forEach(option -> clearedCounts.put(option, 0)); // Az aktuális options lista alapján nullázunk
        this.voteCounts = clearedCounts;
    }

    // --- Specifikus Getter/Setter ---

    /**
     * Visszaadja a válaszlehetőségek listájának másolatát.
     * @return A válaszlehetőségek listájának másolata ({@link List}<{@link String}>).
     */
    public List<String> getOptions() {
        // CopyOnWriteArrayList iterátora szálbiztos, de a másolat biztonságosabb
        return new ArrayList<>(options);
    }

    /**
     * Beállítja a válaszlehetőségeket (szerkesztéshez).
     * Csak zárt állapotban ({@link PollState#CLOSED}) engedélyezett.
     * Kiszűri az érvénytelen (null, üres, duplikált) opciókat és limitálja a számukat {@value #MAX_OPTIONS}-ra.
     * Ha a lista megváltozik, a korábbi szavazatok és eredmények törlődnek ({@link #resetResults()} hívásával).
     * Írási lockot használ.
     * @param newOptions Az új válaszlehetőségek listája. Nem lehet null, és legalább egy érvényes opciót kell tartalmaznia.
     * @return true, ha a beállítás sikeres volt (mert zárt állapotban volt), false egyébként.
     * @throws IllegalArgumentException Ha az `newOptions` lista érvénytelen.
     */
    public boolean setOptions(List<String> newOptions) {
        if (newOptions == null) {
            throw new IllegalArgumentException("Az új opciók listája nem lehet null.");
        }
        // Érvényes opciók kiszűrése
        List<String> validNewOptions = newOptions.stream()
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .limit(MAX_OPTIONS)
                .collect(Collectors.toList());
        if (validNewOptions.isEmpty()) {
            throw new IllegalArgumentException("Legalább egy érvényes (nem üres) opció szükséges a szűrés után.");
        }

        lock.writeLock().lock();
        try {
            if (currentState == PollState.CLOSED) {
                // Lecseréljük a belső listát (CopyOnWriteArrayList nem módosítható direkt, újat kell létrehozni)
                // Ez atomi művelet a CopyOnWriteArrayList-en belül
                this.options.clear(); // Régi elemek törlése
                this.options.addAll(validNewOptions); // Új elemek hozzáadása

                // Mivel az opciók megváltoztak, a régi szavazatok érvénytelenek lehetnek,
                // ezért töröljük az eredményeket és a szavazatokat is.
                resetResults(); // Ez törli a voteCounts-ot és a votes listát is.
                System.out.println("Poll ("+joinCode+"): MultipleChoice options updated, results reset. New options: " + this.options);
                return true;
            }
            System.err.println("Poll ("+joinCode+"): Cannot set options, state is not CLOSED: " + currentState);
            return false; // Nem zárt állapotban nem lehet módosítani
        } finally {
            lock.writeLock().unlock();
        }
    }
}