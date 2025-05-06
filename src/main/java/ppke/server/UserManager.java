package ppke.server;

import ppke.server.model.User;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.mindrot.jbcrypt.BCrypt; // BCrypt import

/**
 * Felhasználók kezeléséért felelős osztály a szerver oldalon.
 * Felelős a felhasználói fiókok tárolásáért (memóriában), új felhasználók regisztrálásáért
 * (egyedi felhasználónév ellenőrzésével és biztonságos jelszó hash-eléssel BCrypt segítségével),
 * valamint a meglévő felhasználók bejelentkeztetéséért a jelszó hash ellenőrzésével.
 * Szálbiztos implementációt valósít meg {@link ConcurrentHashMap} használatával.
 */
public class UserManager {

    /**
     * A regisztrált felhasználókat tároló map.
     * Kulcs: felhasználónév (String), Érték: User objektum.
     * {@link ConcurrentHashMap} biztosítja a szálbiztos műveleteket.
     */
    private final Map<String, User> users;

    /**
     * Konstruktor. Létrehoz egy üres felhasználókezelőt és inicializálja a felhasználókat tároló map-et.
     */
    public UserManager() {
        this.users = new ConcurrentHashMap<>();
        System.out.println("UserManager inicializálva.");
    }

    /**
     * Új felhasználó regisztrálása a rendszerbe.
     * Ellenőrzi, hogy a felhasználónév nem foglalt-e már. Ha szabad,
     * hash-eli a megadott jelszót BCrypt segítségével és eltárolja az új felhasználót.
     * A művelet atomikus a felhasználónév foglaltságának ellenőrzése és a beszúrás tekintetében.
     * Szálbiztos.
     *
     * @param username A regisztrálni kívánt felhasználónév (nem lehet null vagy üres).
     * @param password A felhasználó által megadott sima szöveges jelszó (nem lehet null vagy üres).
     * @return {@code true}, ha a regisztráció sikeres volt (a felhasználónév még nem létezett), {@code false} egyébként.
     * @throws NullPointerException Ha a username vagy password null.
     * @throws IllegalArgumentException Ha a username vagy password üres.
     * @throws RuntimeException Ha a BCrypt hash-elés során váratlan hiba történik.
     */
    public boolean registerUser(String username, String password) {
        Objects.requireNonNull(username, "A felhasználónév nem lehet null a regisztrációhoz");
        Objects.requireNonNull(password, "A jelszó nem lehet null a regisztrációhoz");
        if (username.isBlank()) {
            throw new IllegalArgumentException("A felhasználónév nem lehet üres a regisztrációhoz.");
        }
        if (password.isEmpty()) { // Jelszó lehet whitespace, de üres nem
            throw new IllegalArgumentException("A jelszó nem lehet üres a regisztrációhoz.");
        }

        // Hash-eljük a jelszót MIELŐTT megpróbálnánk beszúrni
        String hashedPassword = hashPassword(password); // Ez RuntimeException-t dobhat

        // A computeIfAbsent atomikusan ellenőrzi a létezést és végzi el a beszúrást, ha nincs.
        // Itt putIfAbsent-et használunk, mert a User objektumot előre létre kell hozni a hash miatt.
        User newUser = new User(username, hashedPassword);
        User existingUser = users.putIfAbsent(username, newUser); // Atomikus művelet

        if (existingUser == null) {
            System.out.println("Új felhasználó sikeresen regisztrálva: " + username);
            return true; // Sikeres regisztráció, nem volt még ilyen user
        } else {
            System.out.println("Regisztrációs kísérlet sikertelen, felhasználónév már foglalt: " + username);
            return false; // Sikertelen regisztráció, már létezik ilyen user
        }
    }

    /**
     * Megkísérli bejelentkeztetni a felhasználót a megadott felhasználónévvel és jelszóval.
     * Lekéri a felhasználót a tárolóból, majd ellenőrzi a megadott jelszót a tárolt BCrypt hash alapján.
     * Szálbiztos.
     *
     * @param username A bejelentkezni kívánó felhasználó neve.
     * @param password A felhasználó által megadott sima szöveges jelszó.
     * @return A bejelentkezett {@link User} objektum, ha a felhasználónév létezik és a jelszó helyes;
     * {@code null} egyébként (ha a user nem létezik, vagy a jelszó hibás, vagy a bemenet null).
     */
    public User loginUser(String username, String password) {
        if (username == null || password == null) {
            System.err.println("Bejelentkezési kísérlet null felhasználónévvel vagy jelszóval.");
            return null; // Érvénytelen bemenet
        }

        User user = users.get(username); // ConcurrentHashMap get művelete szálbiztos

        if (user != null && checkPassword(password, user.getPasswordHash())) {
            System.out.println("Sikeres bejelentkezés: " + username);
            return user; // Megtaláltuk a usert és a jelszó is stimmel
        } else {
            if (user == null) {
                System.out.println("Sikertelen bejelentkezés: Felhasználó nem található - " + username);
            } else {
                // Itt már tudjuk, hogy a user létezik, tehát a jelszó volt hibás
                System.out.println("Sikertelen bejelentkezés: Hibás jelszó - " + username);
            }
            return null; // Nem létezik a user vagy rossz a jelszó
        }
    }

    /**
     * Visszaadja a felhasználókat tároló belső map-et.
     * FIGYELEM: Bár a visszaadott map {@link ConcurrentHashMap}, ami szálbiztos,
     * a közvetlen módosítása kívülről nem ajánlott. Elsősorban mentési célokra szolgál.
     *
     * @return A felhasználókat tároló {@code Map<String, User>}.
     */
    public Map<String, User> getUsers() {
        // Közvetlenül visszaadhatjuk, mert a ConcurrentHashMap szálbiztos.
        // Ha a hívó módosítaná, az hatással lenne a UserManager állapotára!
        // Mentéshez ez megfelelő.
        return users;
    }

    /**
     * Beállítja a felhasználókat tároló map tartalmát a megadott map alapján.
     * A jelenlegi felhasználók törlődnek. Elsősorban betöltési célokra szolgál.
     * Szálbiztos.
     *
     * @param loadedUsers A betöltött felhasználók map-je (nem lehet null).
     * @throws NullPointerException ha a loadedUsers null.
     */
    public void setUsers(Map<String, User> loadedUsers) {
        Objects.requireNonNull(loadedUsers, "A betöltött felhasználók map nem lehet null");
        // Atomikusan lecseréljük a belső map tartalmát
        // A ConcurrentHashMap clear() és putAll() metódusai szálbiztosak.
        users.clear();
        users.putAll(loadedUsers);
        System.out.println(users.size() + " felhasználó betöltve a UserManager-be.");
    }


    // --- Jelszókezelő Segédmetódusok (BCrypt implementációval) ---

    /**
     * Hash-eli a megadott jelszót BCrypt segítségével, automatikusan generált salt használatával.
     * A work factor (jelenleg 12) határozza meg a hash-elés erősségét és idejét.
     *
     * @param plainPassword A hash-elendő sima szöveges jelszó (nem lehet null vagy üres).
     * @return A BCrypt hash string (salt-ot is tartalmazza).
     * @throws NullPointerException ha a plainPassword null.
     * @throws IllegalArgumentException ha a plainPassword üres.
     * @throws RuntimeException Ha a BCrypt hash-elés során váratlan hiba történik.
     */
    private String hashPassword(String plainPassword) {
        Objects.requireNonNull(plainPassword, "A jelszó nem lehet null a hasheléshez");
        if (plainPassword.isEmpty()) { // Üres jelszót nem engedünk hash-elni
            throw new IllegalArgumentException("A jelszó nem lehet üres a hasheléshez");
        }
        try {
            // A 12 a 'work factor' - ajánlott érték, jó kompromisszum biztonság és sebesség között.
            // Magasabb érték biztonságosabb, de jelentősen lassabb lehet.
            return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
        } catch (Exception e) {
            // Ez kritikus hiba, valószínűleg a BCrypt könyvtárral vagy a JVM-mel van gond.
            System.err.println("KRITIKUS HIBA: A BCrypt jelszó hashelés sikertelen! " + e.getMessage());
            // Dobjunk egy nem ellenőrzött kivételt, mert innen nehéz helyreállni.
            throw new RuntimeException("A jelszó hashelése váratlanul sikertelen volt", e);
        }
    }

    /**
     * Ellenőrzi, hogy a megadott sima szöveges jelszó megegyezik-e a tárolt BCrypt hash-sel.
     * A BCrypt {@code checkpw} metódusa automatikusan kinyeri a salt-ot a hash-ből az ellenőrzéshez.
     *
     * @param plainPassword A felhasználó által beírt jelszó (lehet null).
     * @param hashedPassword A felhasználóhoz tárolt BCrypt hash (lehet null vagy üres).
     * @return {@code true}, ha a jelszavak megegyeznek, {@code false} egyébként (beleértve az érvénytelen bemenetet vagy a hibás hash formátumot is).
     */
    private boolean checkPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null || hashedPassword.isBlank()) {
            return false; // Érvénytelen bemenet vagy hash nem lehet helyes
        }
        try {
            // A checkpw biztonságosan kezeli a null/üres plainPassword-öt is (false-t ad vissza).
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (IllegalArgumentException e) {
            // Ez akkor fordul elő, ha a hashedPassword nem valid BCrypt formátumú.
            // Ez lehet fejlesztési hiba, vagy adatbázis sérülés jele. Logoljuk.
            System.err.println("BCrypt ellenőrzési hiba: Érvénytelen jelszó hash formátum: " + hashedPassword + " Hiba: " + e.getMessage());
            return false;
        } catch (Exception e) { // Bármilyen más váratlan hiba a BCrypt-en belül
            System.err.println("Váratlan hiba a BCrypt jelszó ellenőrzésekor: " + e.getMessage());
            return false;
        }
    }
}