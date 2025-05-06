package ppke.server.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A felhasználói fiókot reprezentáló osztály a szerver oldalon.
 * Tárolja a felhasználó azonosításához és a hozzá tartozó szavazások kezeléséhez szükséges adatokat.
 * Implementálja a {@link Serializable} interfészt a perzisztencia (fájlba mentés) érdekében.
 */
public class User implements Serializable {
    /** Szerializációs verzióazonosító. */
    private static final long serialVersionUID = 1L;

    /**
     * A felhasználó egyedi azonosítója (felhasználónév). Létrehozás után nem módosítható.
     */
    private final String username;

    /**
     * A felhasználó jelszavának biztonságosan tárolt hash-e (pl. BCrypt).
     * Ezt a hash-t használjuk a bejelentkezéskor a megadott jelszó ellenőrzésére.
     */
    private String passwordHash;

    /**
     * A felhasználó által létrehozott szavazások egyedi join kódjainak listája.
     * Szálbiztos lista ({@link CopyOnWriteArrayList}) használata ajánlott, ha az olvasás gyakoribb,
     * mint az írás (pl. szavazások listázása), és a lista mérete nem extrém nagy.
     * A join kódok ebben a listában is nagybetűsen tárolódnak.
     */
    private final List<String> createdPollJoinCodes;

    /**
     * Konstruktor új felhasználó létrehozásához.
     * A jelszót már hash-elt formában kell átadni.
     *
     * @param username A felhasználónév (nem lehet null vagy üres).
     * @param passwordHash A BCrypt (vagy más biztonságos algoritmussal) hash-elt jelszó (nem lehet null).
     * @throws NullPointerException ha a username vagy passwordHash null.
     * @throws IllegalArgumentException ha a username üres.
     */
    public User(String username, String passwordHash) {
        this.username = Objects.requireNonNull(username, "A felhasználónév nem lehet null");
        if (username.isBlank()) {
            throw new IllegalArgumentException("A felhasználónév nem lehet üres");
        }
        this.passwordHash = Objects.requireNonNull(passwordHash, "A jelszó hash nem lehet null");
        this.createdPollJoinCodes = new CopyOnWriteArrayList<>();
        System.out.println("User objektum létrehozva: " + username);
    }

    // --- GETTEREK ---

    /**
     * Visszaadja a felhasználónevet.
     * @return A felhasználónév (String).
     */
    public String getUsername() {
        return username;
    }

    /**
     * Visszaadja a felhasználó tárolt jelszó hash-ét.
     * @return A jelszó hash (String).
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Visszaadja a felhasználó által létrehozott szavazások (nagybetűs) join kódjainak
     * módosíthatatlan listáját. A visszaadott lista egy másolat vagy egy módosíthatatlan nézet,
     * így a belső lista nem módosítható kívülről.
     *
     * @return A felhasználó szavazásainak join kódjai (módosíthatatlan {@link List}<{@link String}>).
     */
    public List<String> getCreatedPollJoinCodes() {
        return Collections.unmodifiableList(createdPollJoinCodes);
    }

    // --- LISTA MŰVELETEK ---

    /**
     * Hozzáad egy (nagybetűs) join kódot a felhasználó által létrehozott szavazások listájához.
     * A művelet szálbiztos a {@link CopyOnWriteArrayList} használata miatt.
     *
     * @param joinCode A hozzáadandó, nagybetűs join kód. Nem lehet null vagy üres.
     * @throws NullPointerException ha a joinCode null.
     * @throws IllegalArgumentException ha a joinCode üres.
     */
    public void addPollCode(String joinCode) {
        Objects.requireNonNull(joinCode, "A hozzáadandó join kód nem lehet null");
        if (joinCode.isBlank()) {
            throw new IllegalArgumentException("A hozzáadandó join kód nem lehet üres");
        }
        String upperCaseCode = joinCode.toUpperCase(); // A PollManager már így adja át
        this.createdPollJoinCodes.add(upperCaseCode);
        System.out.println("Szavazás kód (" + upperCaseCode + ") hozzáadva a felhasználóhoz: " + username);
    }

    /**
     * Eltávolít egy (nagybetűs) join kódot a felhasználó által létrehozott szavazások listájából.
     * A művelet szálbiztos a {@link CopyOnWriteArrayList} használata miatt.
     *
     * @param joinCode Az eltávolítandó, nagybetűs join kód. Lehet null (ekkor false-t ad vissza).
     * @return {@code true}, ha a kód szerepelt a listában és sikeresen eltávolításra került, {@code false} egyébként.
     */
    public boolean removePollCode(String joinCode) {
        if (joinCode == null) {
            return false;
        }
        String upperCaseCode = joinCode.toUpperCase();
        boolean removed = this.createdPollJoinCodes.remove(upperCaseCode);
        if (removed) {
            System.out.println("Szavazás kód (" + upperCaseCode + ") eltávolítva a felhasználótól: " + username);
        } else {
            System.out.println("Szavazás kód (" + upperCaseCode + ") nem található az eltávolításhoz a felhasználónál: " + username);
        }
        return removed;
    }

    // --- Object Metódusok ---

    /**
     * Összehasonlítja ezt a felhasználót egy másik objektummal.
     * Két felhasználó akkor tekinthető azonosnak, ha a felhasználónevük megegyezik
     * (kis- és nagybetű érzékenyen).
     *
     * @param o Az összehasonlítandó objektum.
     * @return {@code true}, ha az objektumok azonosak (User típusúak és a username egyezik), {@code false} egyébként.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return username.equals(user.username);
    }

    /**
     * Visszaadja a felhasználó hash kódját.
     * A hash kód csak a felhasználónéven alapul, konzisztensen az {@code equals} metódussal.
     *
     * @return A felhasználó hash kódja.
     */
    @Override
    public int hashCode() {
        return Objects.hash(username);
    }

    /**
     * Visszaadja a felhasználó szöveges reprezentációját (debuggolási célokra).
     * @return A felhasználó string reprezentációja.
     */
    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", createdPollsCount=" + createdPollJoinCodes.size() +
                '}';
    }
}