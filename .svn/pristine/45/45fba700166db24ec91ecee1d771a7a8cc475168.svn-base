package ppke.server;

import ppke.server.model.Poll;
import ppke.server.model.User;

import java.io.*;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;

/**
 * Perzisztencia kezeléséért felelős osztály.
 * A felhasználókat és szavazásokat Map formában menti/tölti be,
 * a UserManager és PollManager objektumokon keresztül.
 */
public class PersistenceManager {

    private static final String USERS_FILE = "mentimeter_users.dat";
    private static final String POLLS_FILE = "mentimeter_polls.dat";

    /**
     * Elmenti a felhasználók adatait a UserManager-ből.
     * A UserManager.getUsers() által visszaadott Map-et szerializálja.
     */
    public synchronized void saveUsers(UserManager userManager) {
        Objects.requireNonNull(userManager, "A UserManager nem lehet null a felhasználók mentéséhez.");
        System.out.println("PersistenceManager: Felhasználók mentése (" + userManager.getUsers().size() + " db)...");
        saveObject(userManager.getUsers(), USERS_FILE);
    }

    /**
     * Elmenti a szavazások adatait a PollManager-ből.
     * A PollManager.getAllPolls() által visszaadott Map-et szerializálja.
     */
    public synchronized void savePolls(PollManager pollManager) {
        Objects.requireNonNull(pollManager, "A PollManager nem lehet null a szavazások mentéséhez.");
        System.out.println("PersistenceManager: Szavazások mentése (" + pollManager.getAllPolls().size() + " db)...");
        saveObject(pollManager.getAllPolls(), POLLS_FILE);
    }

    public synchronized void saveAll(UserManager userManager, PollManager pollManager) {
        System.out.println("PersistenceManager: Minden adat mentése...");
        saveUsers(userManager);
        savePolls(pollManager);
        System.out.println("PersistenceManager: Minden adat mentve.");
    }

    /**
     * Betölti a felhasználók adatait és egy új UserManager példányt ad vissza.
     * A betöltött Map<String, User> adatokat a UserManager.setUsers() metódusával állítja be.
     */
    public synchronized UserManager loadUsers() {
        System.out.println("PersistenceManager: Felhasználók betöltése...");
        UserManager um = new UserManager(); // Új, üres UserManager
        Object loadedObject = loadObject(USERS_FILE);

        if (loadedObject instanceof Map<?,?> map) {
            try {
                Map<String, User> usersMap = new HashMap<>();
                boolean allEntriesValid = true;
                for(Map.Entry<?,?> entry : map.entrySet()){
                    if(entry.getKey() instanceof String && entry.getValue() instanceof User){
                        usersMap.put((String)entry.getKey(), (User)entry.getValue());
                    } else {
                        allEntriesValid = false;
                        System.err.println("PersistenceManager WARN: Érvénytelen bejegyzés a felhasználói adatfájlban: Kulcs=" + entry.getKey() + ", Érték=" + entry.getValue());
                        break;
                    }
                }
                if(allEntriesValid){
                    um.setUsers(usersMap); // UserManager.setUsers(Map<String, User>) hívása
                    System.out.println("PersistenceManager: Felhasználók sikeresen betöltve: " + usersMap.size() + " db.");
                } else {
                    System.err.println("PersistenceManager ERROR: A felhasználói adatfájl érvénytelen bejegyzéseket tartalmazott, nem minden adat lett betöltve.");
                }
            } catch (Exception e) { // Általánosabb hiba elkapása a setUsers körül
                System.err.println("PersistenceManager ERROR: Hiba a felhasználói adatok UserManager-be való beállítása közben: " + e.getMessage());
                e.printStackTrace();
            }
        } else if (loadedObject != null) {
            System.err.println("PersistenceManager ERROR: Váratlan objektumtípus a felhasználói adatfájlban (Map<String, User> helyett): " + loadedObject.getClass().getName());
        }
        return um;
    }

    /**
     * Betölti a szavazások adatait és egy új PollManager példányt ad vissza.
     * A UserManager szükséges a PollManager konstruktorához.
     * A betöltött Map<String, Poll> adatokat a PollManager.setPolls() metódusával állítja be.
     */
    public synchronized PollManager loadPolls(UserManager userManager) {
        Objects.requireNonNull(userManager, "A UserManager nem lehet null a szavazások betöltéséhez.");
        System.out.println("PersistenceManager: Szavazások betöltése...");
        PollManager pm = new PollManager(userManager); // Új, üres PollManager
        Object loadedObject = loadObject(POLLS_FILE);

        if (loadedObject instanceof Map<?,?> map) {
            try {
                Map<String, Poll> pollsMap = new HashMap<>();
                boolean allEntriesValid = true;
                for(Map.Entry<?,?> entry : map.entrySet()){
                    if(entry.getKey() instanceof String && entry.getValue() instanceof Poll){
                        pollsMap.put((String)entry.getKey(), (Poll)entry.getValue());
                    } else {
                        allEntriesValid = false;
                        System.err.println("PersistenceManager WARN: Érvénytelen bejegyzés a szavazási adatfájlban: Kulcs=" + entry.getKey() + ", Érték=" + entry.getValue());
                        break;
                    }
                }
                if(allEntriesValid){
                    pm.setPolls(pollsMap); // PollManager.setPolls(Map<String, Poll>) hívása
                    System.out.println("PersistenceManager: Szavazások sikeresen betöltve: " + pollsMap.size() + " db.");
                } else {
                    System.err.println("PersistenceManager ERROR: A szavazási adatfájl érvénytelen bejegyzéseket tartalmazott, nem minden adat lett betöltve.");
                }

            } catch (Exception e) {
                System.err.println("PersistenceManager ERROR: Hiba a szavazási adatok PollManager-be való beállítása közben: " + e.getMessage());
                e.printStackTrace();
            }
        } else if (loadedObject != null) {
            System.err.println("PersistenceManager ERROR: Váratlan objektumtípus a szavazási adatfájlban (Map<String, Poll> helyett): " + loadedObject.getClass().getName());
        }
        return pm;
    }

    private void saveObject(Object object, String filename) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(object);
            System.out.println("PersistenceManager INFO: Objektum (" + object.getClass().getSimpleName() + ") sikeresen mentve ide: " + filename);
        } catch (IOException e) {
            System.err.println("PersistenceManager ERROR: I/O Hiba történt mentés közben (" + filename + "): " + e.getMessage());
        }
    }

    private Object loadObject(String filename) {
        File file = new File(filename);
        if (!file.exists() || file.length() == 0) {
            System.out.println("PersistenceManager INFO: Adatfájl nem található vagy üres (normális első indításkor): " + filename);
            return null;
        }
        if (!file.canRead()) {
            System.err.println("PersistenceManager ERROR: Nem olvasható adatfájl: " + filename);
            return null;
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
            Object loaded = ois.readObject();
            System.out.println("PersistenceManager INFO: Objektum sikeresen betöltve innen: " + filename);
            return loaded;
        } catch (FileNotFoundException e) {
            System.err.println("PersistenceManager ERROR: Fájl nem található (nem várt): " + filename); return null;
        } catch (EOFException e) {
            System.err.println("PersistenceManager ERROR: Váratlan fájlvége (" + filename + "). Sérült fájl? " + e.getMessage()); return null;
        } catch (IOException e) { System.err.println("PersistenceManager ERROR: I/O Hiba betöltés közben (" + filename + "): " + e.getMessage()); return null;
        } catch (ClassNotFoundException e) { System.err.println("PersistenceManager ERROR: Osztály nem található (" + filename + "). " + e.getMessage()); return null;
        } catch (Exception e) { System.err.println("PersistenceManager ERROR: Váratlan hiba (" + filename + "): " + e.getMessage()); e.printStackTrace(); return null;
        }
    }
}
