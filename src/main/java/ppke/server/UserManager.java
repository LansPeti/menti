package ppke.server;

import ppke.server.model.User;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List; // Bár a perzisztencia Map-et használ, a belső tárolás lehet List
import java.util.Map;   // Map használata a perzisztencia felé
import java.util.HashMap; // HashMap implementáció
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap; // Szálbiztos Map a belső tároláshoz
import java.util.stream.Collectors;
import java.util.ArrayList; // getAllUsers() metódushoz

/**
 * A felhasználókkal kapcsolatos műveleteket kezeli.
 * A felhasználókat belsőleg egy Map-ben tárolja (felhasználónév -> User objektum).
 */
public class UserManager {
    // Belső tárolás ConcurrentHashMap-ben a szálbiztosságért
    private final Map<String, User> users;
    // A PersistenceManager-nek nincs itt közvetlen szerepe, ha a ServerMain kezeli a mentést/betöltést
    // De ha a UserManager konstruktora kapja, akkor használhatja.

    private static final int SALT_LENGTH = 16;
    private static final String HASH_ALGORITHM = "SHA-256";

    /**
     * UserManager konstruktor.
     * Üres felhasználói adatbázissal (Map) indul.
     * A perzisztenciából való betöltést a PersistenceManager.loadUsers() végzi,
     * ami utána meghívja a setUsers() metódust.
     */
    public UserManager() {
        this.users = new ConcurrentHashMap<>(); // Szálbiztos map
        System.out.println("UserManager INFO: UserManager inicializálva (kezdetben üres).");
    }

    /**
     * Új felhasználót regisztrál a rendszerbe.
     * A jelszót sózva és hash-elve tárolja.
     *
     * @param username A kívánt felhasználónév.
     * @param password A felhasználó jelszava.
     * @return true, ha a regisztráció sikeres volt, false, ha a felhasználónév már foglalt vagy hiba történt.
     */
    public synchronized boolean registerUser(String username, String password) { // Visszatérési érték boolean
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            System.out.println("UserManager WARN: Regisztrációs kísérlet üres felhasználónévvel vagy jelszóval.");
            return false;
        }
        // Kis-nagybetű érzéketlen ellenőrzés
        if (users.containsKey(username.toLowerCase())) {
            System.out.println("UserManager WARN: Foglalt felhasználónévvel történő regisztrációs kísérlet: " + username);
            return false; // Felhasználónév foglalt
        }

        byte[] salt = generateSalt();
        byte[] hashedPassword = hashPassword(password, salt);

        if (hashedPassword == null) {
            // A hashPassword már logolt, itt csak false-t adunk vissza
            return false;
        }

        User newUser = new User(username, hashedPassword, salt);
        users.put(username.toLowerCase(), newUser); // Kisbetűs kulccsal tároljuk
        System.out.println("UserManager INFO: Új felhasználó sikeresen regisztrálva: " + username);
        return true;
    }

    public synchronized User loginUser(String username, String password) {
        User user = users.get(username.toLowerCase()); // Kisbetűs kulccsal keressük

        if (user != null) {
            byte[] salt = user.getSalt();
            byte[] hashedPasswordAttempt = hashPassword(password, salt);

            if (hashedPasswordAttempt != null && Arrays.equals(hashedPasswordAttempt, user.getHashedPassword())) {
                String sessionId = generateSessionId();
                user.setSessionId(sessionId);
                System.out.println("UserManager INFO: Felhasználó '" + username + "' sikeresen bejelentkezett.");
                return user;
            } else {
                System.out.println("UserManager WARN: Sikertelen bejelentkezési kísérlet (hibás jelszó): " + username);
            }
        } else {
            System.out.println("UserManager WARN: Sikertelen bejelentkezési kísérlet (felhasználó nem található): " + username);
        }
        return null;
    }

    public synchronized void logoutUser(String sessionId) {
        if (sessionId == null) return;
        // Végig kell menni a felhasználókon, hogy megtaláljuk a session ID alapján
        for (User user : users.values()) {
            if (sessionId.equals(user.getSessionId())) {
                System.out.println("UserManager INFO: Felhasználó '" + user.getUsername() + "' kijelentkezik.");
                user.setSessionId(null);
                return; // Kilépünk, ha megtaláltuk és kijelentkeztettük
            }
        }
        System.out.println("UserManager WARN: Kijelentkezési kísérlet érvénytelen vagy lejárt session ID-vel.");
    }

    public synchronized User getUserBySessionId(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return null;
        }
        return users.values().stream()
                .filter(u -> sessionId.equals(u.getSessionId()))
                .findFirst()
                .orElse(null);
    }

    private String generateSessionId() {
        return UUID.randomUUID().toString();
    }

    private static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return salt;
    }

    private static byte[] hashPassword(String password, byte[] salt) {
        try {
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            md.update(salt);
            return md.digest(password.getBytes());
        } catch (NoSuchAlgorithmException e) {
            System.err.println("UserManager ERROR: Kritikus hiba a jelszó hash-elése közben: " + HASH_ALGORITHM + " algoritmus nem található!");
            e.printStackTrace();
            return null; // Jelzés a hívónak, hogy hiba történt
        }
    }

    /**
     * Visszaadja a felhasználók belső map-jének másolatát.
     * Ezt használja a PersistenceManager a mentéshez.
     * @return A felhasználók map-jének másolata (kulcs: kisbetűs felhasználónév).
     */
    public Map<String, User> getUsers() {
        return new HashMap<>(users); // Másolatot adunk vissza
    }

    /**
     * Beállítja a UserManager belső felhasználói listáját (map-jét) a betöltött adatok alapján.
     * Ezt a PersistenceManager.loadUsers() hívja meg.
     * @param loadedUsers A betöltött felhasználók map-je (kulcs: felhasználónév).
     */
    public synchronized void setUsers(Map<String, User> loadedUsers) {
        if (loadedUsers != null) {
            this.users.clear();
            // Biztosítjuk, hogy a kulcsok kisbetűsek legyenek
            loadedUsers.forEach((username, user) -> this.users.put(username.toLowerCase(), user));
            System.out.println("UserManager INFO: Felhasználók beállítva a PersistenceManager által (" + this.users.size() + " db).");
        }
    }

    // Ez a metódus akkor kell, ha valahol List<User>-re van szükség,
    // de a PersistenceManager Map-et kezel.
    public List<User> getAllUsersAsList() {
        return new ArrayList<>(users.values());
    }
}
