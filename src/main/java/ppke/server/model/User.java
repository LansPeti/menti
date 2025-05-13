package ppke.server.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A rendszer egy felhasználóját reprezentálja.
 * Tárolja a felhasználói adatokat, a munkamenet információkat,
 * és a felhasználó által létrehozott szavazások join kódjait.
 * A jelszavakat salted hash formájában tárolja.
 */
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 3L;

    private final String username;
    private final byte[] hashedPassword;
    private final byte[] salt;
    private String sessionId;

    private final List<String> createdPollJoinCodes; // Felhasználó által létrehozott szavazások kódjai

    public User(String username, byte[] hashedPassword, byte[] salt) {
        this.username = username;
        this.hashedPassword = hashedPassword;
        this.salt = salt;
        this.createdPollJoinCodes = new ArrayList<>();
    }

    public String getUsername() {
        return username;
    }

    public byte[] getHashedPassword() {
        return hashedPassword;
    }

    public byte[] getSalt() {
        return salt;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public List<String> getCreatedPollJoinCodes() {
        return new ArrayList<>(createdPollJoinCodes); // Másolatot adunk vissza
    }

    public void addPollCode(String joinCode) {
        if (joinCode != null && !joinCode.trim().isEmpty() && !this.createdPollJoinCodes.contains(joinCode.toUpperCase())) {
            this.createdPollJoinCodes.add(joinCode.toUpperCase()); // Nagybetűsen tároljuk a konzisztencia érdekében
        }
    }

    /**
     * Eltávolít egy join kódot a felhasználó által létrehozott szavazások listájából.
     * @param joinCode Az eltávolítandó join kód (kis-nagybetű érzéketlen).
     * @return true, ha a kód szerepelt a listában és sikeresen eltávolításra került, false egyébként.
     */
    public boolean removePollCode(String joinCode) { // Visszatérési érték boolean-re változtatva
        if (joinCode != null) {
            return this.createdPollJoinCodes.remove(joinCode.toUpperCase());
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(username.toLowerCase(), user.username.toLowerCase());
    }

    @Override
    public int hashCode() {
        return Objects.hash(username.toLowerCase());
    }

    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", sessionId='" + (sessionId != null ? sessionId.substring(0, Math.min(sessionId.length(), 8)) + "..." : "null") + '\'' +
                ", createdPollsCount=" + createdPollJoinCodes.size() +
                '}';
    }
}
