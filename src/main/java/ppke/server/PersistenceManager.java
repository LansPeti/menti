package ppke.server;

import ppke.server.model.Poll;
import ppke.server.model.User;

import java.io.*; // Import File, FileInputStream, FileOutputStream, ObjectInputStream, ObjectOutputStream, IOException, FileNotFoundException
import java.util.Map;
import java.util.HashMap; // Import HashMap
import java.util.Objects; // Import Objects

/**
 * Perzisztencia kezeléséért felelős osztály a szerver oldalon.
 * Felhasználók ({@link User}) és szavazások ({@link Poll}) mentését és betöltését végzi
 * fájlból, Java standard szerializáció mechanizmusával.
 * A mentési és betöltési műveletek szinkronizáltak a konkurrens hozzáférés elkerülése érdekében.
 */
public class PersistenceManager {

    /** A felhasználói adatokat tároló fájl neve. */
    private static final String USERS_FILE = "mentimeter_users.dat";
    /** A szavazási adatokat tároló fájl neve. */
    private static final String POLLS_FILE = "mentimeter_polls.dat";

    /**
     * Elmenti a felhasználók adatait a {@value #USERS_FILE} nevű fájlba.
     * A {@link UserManager#getUsers()} által visszaadott map-et szerializálja.
     * Szinkronizált metódus.
     * @param userManager A felhasználókat kezelő objektum (nem lehet null).
     */
    public synchronized void saveUsers(UserManager userManager) {
        Objects.requireNonNull(userManager, "A UserManager nem lehet null a felhasználók mentéséhez.");
        System.out.println("PersistenceManager: Felhasználók mentése ide: " + USERS_FILE + "...");
        saveObject(userManager.getUsers(), USERS_FILE);
    }

    /**
     * Elmenti a szavazások adatait a {@value #POLLS_FILE} nevű fájlba.
     * A {@link PollManager#getAllPolls()} által visszaadott map-et szerializálja.
     * Szinkronizált metódus.
     * @param pollManager A szavazásokat kezelő objektum (nem lehet null).
     */
    public synchronized void savePolls(PollManager pollManager) {
        Objects.requireNonNull(pollManager, "A PollManager nem lehet null a szavazások mentéséhez.");
        System.out.println("PersistenceManager: Szavazások mentése ide: " + POLLS_FILE + "...");
        saveObject(pollManager.getAllPolls(), POLLS_FILE);
    }

    /**
     * Elmenti mind a felhasználók, mind a szavazások adatait a megfelelő fájlokba.
     * Hasznos lehet pl. szerver leállításakor egyetlen atomi (szinkronizált) műveletben.
     * Szinkronizált metódus.
     * @param userManager A felhasználókat kezelő objektum (nem lehet null).
     * @param pollManager A szavazásokat kezelő objektum (nem lehet null).
     */
    public synchronized void saveAll(UserManager userManager, PollManager pollManager) {
        System.out.println("PersistenceManager: Minden adat mentése...");
        saveUsers(userManager);
        savePolls(pollManager);
        System.out.println("PersistenceManager: Minden adat mentve.");
    }

    /**
     * Betölti a felhasználók adatait a {@value #USERS_FILE} fájlból.
     * Ha a fájl nem létezik vagy hiba történik a betöltés során, egy új, üres {@link UserManager}-t ad vissza.
     * Ellenőrzi a betöltött objektum típusát.
     * Szinkronizált metódus.
     * @return {@link UserManager} objektum a betöltött adatokkal, vagy új, üres UserManager hiba esetén.
     */
    public synchronized UserManager loadUsers() {
        System.out.println("PersistenceManager: Felhasználók betöltése innen: " + USERS_FILE + "...");
        UserManager userManager = new UserManager(); // Kezdjük egy üressel, erre töltünk rá
        Object loadedObject = loadObject(USERS_FILE);

        if (loadedObject instanceof Map<?,?> map) { // Ellenőrizzük, hogy Map-et kaptunk-e vissza
            try {
                @SuppressWarnings("unchecked") // Elnyomjuk a figyelmeztetést, mert ellenőriztük a típust
                Map<String, User> users = (Map<String, User>) map;
                userManager.setUsers(new HashMap<>(users)); // HashMap másolatot adunk át
                System.out.println("PersistenceManager: Felhasználók sikeresen betöltve: " + users.size() + " db.");
            } catch (ClassCastException e) {
                System.err.println("PersistenceManager: Hiba a felhasználói adatok típuskonverziója közben: " + e.getMessage());
            }
        } else if (loadedObject != null) {
            System.err.println("PersistenceManager: Váratlan objektumtípus a felhasználói adatfájlban: " + loadedObject.getClass().getName());
        }
        // Ha loadedObject null (fájl nem volt, vagy IO/ClassNotFound hiba), akkor az üres UserManager marad.
        return userManager;
    }

    /**
     * Betölti a szavazások adatait a {@value #POLLS_FILE} fájlból.
     * Ha a fájl nem létezik vagy hiba történik a betöltés során, egy új, üres {@link PollManager}-t ad vissza.
     * Ellenőrzi a betöltött objektum típusát és a benne lévő adatok konzisztenciáját.
     * Szinkronizált metódus.
     * @param userManager Szükséges a {@link PollManager} inicializálásához (nem lehet null).
     * @return {@link PollManager} objektum a betöltött adatokkal, vagy új, üres PollManager hiba esetén.
     */
    public synchronized PollManager loadPolls(UserManager userManager) {
        Objects.requireNonNull(userManager, "A UserManager nem lehet null a szavazások betöltéséhez.");
        System.out.println("PersistenceManager: Szavazások betöltése innen: " + POLLS_FILE + "...");
        PollManager pollManager = new PollManager(userManager); // Kezdjük egy üressel
        Object loadedObject = loadObject(POLLS_FILE);

        if (loadedObject instanceof Map<?,?> map) { // Ellenőrizzük, hogy Map-et kaptunk-e vissza
            try {
                @SuppressWarnings("unchecked")
                Map<String, Poll> polls = (Map<String, Poll>) map;
                // A setPolls ellenőrzi a betöltött pollok érvényességét és konzisztenciáját
                pollManager.setPolls(new HashMap<>(polls)); // HashMap másolatot adunk át
                System.out.println("PersistenceManager: Szavazások sikeresen betöltve: " + pollManager.getAllPolls().size() + " érvényes bejegyzés.");
            } catch (ClassCastException e) {
                System.err.println("PersistenceManager: Hiba a szavazási adatok típuskonverziója közben: " + e.getMessage());
            }
        } else if (loadedObject != null) {
            System.err.println("PersistenceManager: Váratlan objektumtípus a szavazási adatfájlban: " + loadedObject.getClass().getName());
        }
        // Ha loadedObject null, az üres PollManager marad.
        return pollManager;
    }


    /**
     * Segédfüggvény egy objektum szerializálására a megadott fájlba.
     * Try-with-resources szerkezetet használ a streamek automatikus lezárásához.
     * Kezeli az {@link IOException}-t.
     * @param object A menteni kívánt {@link Serializable} objektum.
     * @param filename A célfájl elérési útja.
     */
    private void saveObject(Object object, String filename) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(object);
            System.out.println("PersistenceManager: Objektum sikeresen mentve ide: " + filename);
        } catch (IOException e) {
            System.err.println("PersistenceManager: I/O Hiba történt mentés közben (" + filename + "): " + e.getMessage());
            // e.printStackTrace(); // Debuggoláshoz
        }
    }

    /**
     * Segédfüggvény egy objektum deszerializálására a megadott fájlból.
     * Try-with-resources szerkezetet használ.
     * Kezeli a főbb kivételeket ({@link FileNotFoundException}, {@link IOException}, {@link ClassNotFoundException}, {@link ClassCastException}).
     * @param filename A forrásfájl elérési útja.
     * @return A betöltött objektum, vagy {@code null}, ha a fájl nem található vagy hiba történt a betöltés/kasztolás során.
     */
    private Object loadObject(String filename) {
        File file = new File(filename);
        if (!file.exists()) {
            System.out.println("PersistenceManager: Adatfájl nem található (ez lehet normális első indításkor): " + filename);
            return null;
        }
        if (!file.canRead()) {
            System.err.println("PersistenceManager: Nem olvasható adatfájl: " + filename);
            return null;
        }
        if (file.length() == 0) {
            System.out.println("PersistenceManager: Adatfájl üres: " + filename);
            return null; // Üres fájlt nem próbálunk olvasni
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
            Object loaded = ois.readObject();
            System.out.println("PersistenceManager: Objektum sikeresen betöltve innen: " + filename);
            return loaded;
        } catch (FileNotFoundException e) {
            System.err.println("PersistenceManager: Fájl nem található kivétel (nem fordulhatna elő itt): " + filename); return null;
        } catch (IOException e) { System.err.println("PersistenceManager: I/O Hiba betöltés közben (" + filename + "): " + e.getMessage()); return null;
        } catch (ClassNotFoundException e) { System.err.println("PersistenceManager: Osztály nem található hiba (" + filename + "). Inkompatibilis verzió? " + e.getMessage()); return null;
        } catch (ClassCastException e) { System.err.println("PersistenceManager: Típuskonverziós hiba (" + filename + "). Sérült fájl? " + e.getMessage()); return null;
        } catch (Exception e) { System.err.println("PersistenceManager: Váratlan hiba betöltés közben (" + filename + "): " + e.getMessage()); e.printStackTrace(); return null;
        }
    }
}