package ppke.server.model;

import ppke.common.dto.PollDTO;
import ppke.common.dto.VoteData;
import ppke.common.model.PollState;
import ppke.common.model.PollType;

import java.io.*;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Absztrakt ősosztály a szavazások közös tulajdonságaihoz és metódusaihoz a szerver oldalon.
 * Definiálja a közös adattagokat (létrehozó, név, kérdés, kód, állapot),
 * az állapotgépet ({@link #setState(PollState)}), a szavazatok tárolását ({@link #addVote(VoteData)}),
 * az eredmények nullázását ({@link #resetResults()}), és a DTO konverziót ({@link #toDTO()}).
 * Az alosztályoknak implementálniuk kell a típus-specifikus logikát (pl. {@link #isValidVote(VoteData)},
 * {@link #calculateResults()}, {@link #getFormattedResults()}).
 * A belső állapot konzisztenciáját és a szálbiztonságot egy {@link ReadWriteLock}
 * segítségével biztosítja a kritikus műveleteknél.
 * Implementálja a {@link Serializable} interfészt a perzisztenciához, különös tekintettel
 * a transient 'lock' mező újra-inicializálására deszerializáláskor ({@link #readObject(ObjectInputStream)}).
 */
public abstract class Poll implements Serializable {
    /** Szerializációs verzióazonosító. */
    private static final long serialVersionUID = 1L;

    /** A szavazást létrehozó felhasználó neve (végleges). */
    protected final String creatorUsername;
    /** A szavazás neve (szerkeszthető CLOSED állapotban). */
    protected String name;
    /** A szavazásban feltett kérdés (szerkeszthető CLOSED állapotban). */
    protected String question;
    /**
     * A szavazás egyedi, 8 karakteres, NAGYBETŰS alfanumerikus csatlakozási kódja (végleges).
     */
    protected final String joinCode;
    /**
     * A szavazás aktuális állapota ({@link PollState}). Volatile a jobb láthatóságért a szálak között.
     */
    protected volatile PollState currentState;

    /**
     * A leadott érvényes szavazatokat tároló lista ({@link VoteData} objektumok).
     * Szálbiztos lista ({@link Collections#synchronizedList(List)}) használata.
     */
    protected final List<VoteData> votes;

    /**
     * ReadWriteLock a szavazatok és az állapot konzisztens kezelésére.
     * Írási lock szükséges az állapotváltáshoz, szavazat hozzáadásához, reset-hez, szerkesztéshez.
     * Olvasási lock szükséges az állapot lekérdezéséhez, eredmények formázásához, DTO készítéshez.
     * Fair lock (`true`) használata az írók "éhezésének" elkerülésére.
     * `transient`, mert a lock objektumot nem szerializáljuk, deszerializáláskor újra kell inicializálni.
     */
    protected transient ReadWriteLock lock = new ReentrantReadWriteLock(true); // Fair lock

    /**
     * Konstruktor az alap szavazás adatokkal.
     * @param creatorUsername A létrehozó felhasználó neve (nem lehet null).
     * @param name A szavazás neve (nem lehet null vagy üres).
     * @param question A feltett kérdés (nem lehet null vagy üres).
     * @param joinCode Az egyedi, NAGYBETŰS join kód (nem lehet null, 8 alfanum karakter).
     * @throws NullPointerException ha bármelyik paraméter null.
     * @throws IllegalArgumentException ha a név, kérdés üres, vagy a join kód formátuma érvénytelen.
     */
    public Poll(String creatorUsername, String name, String question, String joinCode) {
        this.creatorUsername = Objects.requireNonNull(creatorUsername, "A létrehozó felhasználóneve nem lehet null");
        this.name = Objects.requireNonNull(name, "A szavazás neve nem lehet null");
        if (name.isBlank()) throw new IllegalArgumentException("A szavazás neve nem lehet üres");
        this.question = Objects.requireNonNull(question, "A szavazás kérdése nem lehet null");
        if (question.isBlank()) throw new IllegalArgumentException("A szavazás kérdése nem lehet üres");

        this.joinCode = Objects.requireNonNull(joinCode, "A join kód nem lehet null").toUpperCase(Locale.ROOT);
        if (!this.joinCode.matches("^[A-Z0-9]{8}$")) {
            throw new IllegalArgumentException("Belső hiba: Érvénytelen join kód formátum a Poll konstruktorban: " + joinCode);
        }

        this.currentState = PollState.CLOSED;
        this.votes = Collections.synchronizedList(new ArrayList<>());
        this.lock = new ReentrantReadWriteLock(true);
        System.out.println("Poll objektum létrehozva: " + this.joinCode);
    }

    /**
     * Megpróbálja beállítani a szavazás új állapotát a megengedett átmenetek szerint.
     * Írási lockot használ a művelethez. Ha az állapot RESULTS lesz, meghívja a calculateResults()-t.
     * Ha az állapot CLOSED lesz (RESULTS-ból vagy OPEN_FOR_JOINING-ból), törli a számított eredményeket.
     * @param newState A kívánt új állapot (nem lehet null).
     * @return true, ha az állapotváltás sikeres és megengedett volt, false egyébként.
     */
    public boolean setState(PollState newState) {
        Objects.requireNonNull(newState, "Az új állapot nem lehet null");
        System.out.println("Poll ("+joinCode+"): Állapotváltási kísérlet: " + currentState + " -> " + newState);
        lock.writeLock().lock();
        try {
            boolean validTransition = switch (currentState) {
                case CLOSED -> (newState == PollState.OPEN_FOR_JOINING);
                case OPEN_FOR_JOINING -> (newState == PollState.VOTING || newState == PollState.CLOSED);
                case VOTING -> (newState == PollState.RESULTS);
                case RESULTS -> (newState == PollState.CLOSED);
            };

            if (validTransition) {
                PollState oldState = this.currentState;
                this.currentState = newState;
                System.out.println("Poll (" + joinCode + "): Állapot sikeresen megváltoztatva: " + newState);
                if (newState == PollState.RESULTS) {
                    System.out.println("Poll (" + joinCode + "): Eredmények számítása...");
                    calculateResults();
                }
                if (newState == PollState.CLOSED && (oldState == PollState.RESULTS || oldState == PollState.OPEN_FOR_JOINING)) {
                    System.out.println("Poll (" + joinCode + "): Számított eredmények törlése (lezárás).");
                    clearCalculatedResults();
                }
                return true;
            } else {
                System.err.println("Poll (" + joinCode + "): Érvénytelen állapotátmenet: " + currentState + " -> " + newState);
                return false;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Hozzáad egy szavazatot a listához, ha a szavazás VOTING állapotban van
     * ÉS a szavazat érvényes a típushoz (isValidVote).
     * Írási lockot használ a lista módosításához.
     * @param voteData A leadott szavazat adatai (nem lehet null).
     * @return true, ha a szavazat sikeresen hozzáadásra került, false egyébként.
     */
    public boolean addVote(VoteData voteData) {
        Objects.requireNonNull(voteData, "A szavazat adatai nem lehetnek null");
        if (currentState != PollState.VOTING) {
            System.err.println("Poll ("+joinCode+"): Szavazat elutasítva. Érvénytelen állapot: " + currentState);
            return false;
        }
        if (voteData.isEmpty()) {
            System.err.println("Poll ("+joinCode+"): Szavazat elutasítva. Üres szavazat adat.");
            return false;
        }
        if (!isValidVote(voteData)) {
            System.err.println("Poll ("+joinCode+"): Szavazat elutasítva. Érvénytelen szavazat adat a(z) " + getPollType() + " típushoz: " + voteData);
            return false;
        }

        lock.writeLock().lock();
        try {
            if (currentState != PollState.VOTING) {
                System.err.println("Poll ("+joinCode+"): Szavazat elutasítva (lock alatt). Állapot közben megváltozott: " + currentState);
                return false;
            }
            this.votes.add(voteData);
            System.out.println("Poll ("+joinCode+"): Szavazat sikeresen hozzáadva. Összes szavazat: " + votes.size());
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Törli a begyűjtött szavazatokat és a számított eredményeket (az alosztály felelőssége).
     * Írási lockot használ.
     */
    public void resetResults() {
        System.out.println("Poll ("+joinCode+"): Eredmények nullázása...");
        lock.writeLock().lock();
        try {
            int oldVoteCount = this.votes.size();
            this.votes.clear();
            clearCalculatedResults();
            System.out.println("Poll (" + joinCode + "): Eredmények nullázva. " + oldVoteCount + " szavazat törölve.");
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** @return A létrehozó felhasználóneve. */
    public String getCreatorUsername() { return creatorUsername; }
    /** @return A szavazás aktuális neve (olvasási lockkal). */
    public String getName() { lock.readLock().lock(); try { return name; } finally { lock.readLock().unlock(); } }
    /** @return A szavazás aktuális kérdése (olvasási lockkal). */
    public String getQuestion() { lock.readLock().lock(); try { return question; } finally { lock.readLock().unlock(); } }
    /** @return A szavazás (nagybetűs) join kódja. */
    public String getJoinCode() { return joinCode; }
    /** @return A szavazás aktuális állapota. */
    public PollState getCurrentState() { return currentState; }

    /**
     * Visszaadja a leadott szavazatok listájának másolatát.
     * Szálbiztos módon kezeli a lista másolását.
     * @return A szavazatok listájának másolata ({@link List}<{@link VoteData}>).
     */
    public List<VoteData> getVotes() {
        synchronized (votes) {
            return new ArrayList<>(votes);
        }
    }

    /**
     * Visszaadja a szavazás konkrét típusát.
     * @return A szavazás típusa ({@link PollType}).
     */
    public abstract PollType getPollType();

    /**
     * Ellenőrzi, hogy a kapott szavazat adatai érvényesek-e erre a konkrét szavazástípusra.
     * Ezt a metódust lock nélkül hívjuk, nem módosíthatja a poll állapotát.
     * @param voteData A vizsgálandó szavazat adatai.
     * @return true, ha a szavazat érvényes a típushoz, false egyébként.
     */
    public abstract boolean isValidVote(VoteData voteData);

    /**
     * Kiszámítja és eltárolja az eredményeket a meglévő szavazatok alapján.
     * Ezt a metódust az ősosztály hívja meg írási lock alatt, amikor RESULTS állapotba lépünk.
     */
    protected abstract void calculateResults();

    /**
     * Visszaadja a formázott eredményeket a kliens számára (pl. DTO-ba csomagolva).
     * Ezt a metódust olvasási lock alatt hívjuk a {@link #toDTO()} metódusból.
     * A visszaadott objektum típusának konzisztensnek kell lennie a {@link PollDTO} `results` mezőjével.
     * @return Az eredmények (pl. {@code Map<String, Integer>} vagy {@code Map<String, Double>}), vagy null, ha nincsenek eredmények.
     */
    public abstract Object getFormattedResults();

    /**
     * Törli az alosztály-specifikus számított eredményeket (pl. a map-eket).
     * Ezt a metódust az ősosztály hívja meg írási lock alatt a {@link #resetResults()} vagy a CLOSED állapotba lépéskor.
     */
    protected abstract void clearCalculatedResults();


    /**
     * Beállítja a szavazás új nevét. Csak zárt állapotban engedélyezett. Írási lockot használ.
     * @param name Az új név. Nem lehet null vagy üres.
     * @return true, ha sikeres volt a beállítás (mert zárt állapotban volt), false egyébként.
     * @throws NullPointerException ha a név null.
     * @throws IllegalArgumentException ha a név üres.
     */
    public boolean setName(String name) {
        Objects.requireNonNull(name, "A név nem lehet null");
        if (name.isBlank()) throw new IllegalArgumentException("A név nem lehet üres");
        lock.writeLock().lock();
        try {
            if (currentState == PollState.CLOSED) {
                this.name = name;
                System.out.println("Poll ("+joinCode+"): Név frissítve.");
                return true;
            }
            System.err.println("Poll ("+joinCode+"): Név nem állítható be, állapot nem CLOSED: " + currentState);
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Beállítja a szavazás új kérdését. Csak zárt állapotban engedélyezett. Írási lockot használ.
     * @param question Az új kérdés. Nem lehet null vagy üres.
     * @return true, ha sikeres volt a beállítás (mert zárt állapotban volt), false egyébként.
     * @throws NullPointerException ha a kérdés null.
     * @throws IllegalArgumentException ha a kérdés üres.
     */
    public boolean setQuestion(String question) {
        Objects.requireNonNull(question, "A kérdés nem lehet null");
        if (question.isBlank()) throw new IllegalArgumentException("A kérdés nem lehet üres");
        lock.writeLock().lock();
        try {
            if (currentState == PollState.CLOSED) {
                this.question = question;
                System.out.println("Poll ("+joinCode+"): Kérdés frissítve.");
                return true;
            }
            System.err.println("Poll ("+joinCode+"): Kérdés nem állítható be, állapot nem CLOSED: " + currentState);
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }
    /**
     * Létrehoz egy {@link PollDTO} objektumot ebből a Poll objektumból.
     * Olvasási lockot használ az adatok konzisztens lekéréséhez.
     * @return A létrehozott {@link PollDTO}.
     */
    public PollDTO toDTO() {
        lock.readLock().lock();
        try {
            List<String> options = null; List<String> aspects = null; int min = 0; int max = 0;
            if (this instanceof MultipleChoicePoll mcp) { options = mcp.getOptions(); }
            else if (this instanceof ScalePoll sp) {
                aspects = sp.getAspects();
                min = sp.getScaleMin();
                max = sp.getScaleMax();
            }
            Object results = (currentState == PollState.RESULTS) ? getFormattedResults() : null;
            return new PollDTO(this.joinCode, this.name, this.question, this.getPollType(), this.currentState, options, aspects, min, max, results);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Speciális metódus a deszerializáláshoz (readObject).
     * Újra inicializálja a transient lock mezőt a megfelelő működéshez.
     * @param ois Az ObjectInputStream, amiből olvasunk.
     * @throws IOException Olvasási hiba esetén.
     * @throws ClassNotFoundException Osztály nem található hiba esetén.
     */
    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        this.lock = new ReentrantReadWriteLock(true);
        System.out.println("Poll objektum deszerializálva: " + this.joinCode);
    }

    /**
     * Visszaadja a szavazás szöveges reprezentációját (debuggolási célokra).
     * @return A szavazás string reprezentációja.
     */
    @Override
    public String toString() {
        return String.format("%s[Kód=%s, Név='%s', Állapot=%s, Létrehozó=%s, Szavazatok=%d]",
                getPollType().getDisplayName(), joinCode, name, currentState.getDisplayName(), creatorUsername, votes.size());
    }

    /**
     * Összehasonlítja ezt a szavazást egy másik objektummal.
     * Két szavazás akkor tekinthető azonosnak, ha a join kódjuk megegyezik.
     * @param o Az összehasonlítandó objektum.
     * @return {@code true}, ha az objektumok azonosak, {@code false} egyébként.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Poll poll = (Poll) o;
        return joinCode.equals(poll.joinCode);
    }

    /**
     * Visszaadja a szavazás hash kódját.
     * A hash kód csak a join kódon alapul, konzisztensen az {@code equals} metódussal.
     * @return A szavazás hash kódja.
     */
    @Override
    public int hashCode() {
        return Objects.hash(joinCode);
    }
}
