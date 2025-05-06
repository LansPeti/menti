package ppke.common.model;

/**
 * A szavazás lehetséges állapotait definiáló enum.
 */
public enum PollState {
    /**
     * A szavazás létrehozva, de nem aktív, nem lehet csatlakozni vagy szavazni.
     * Ebben az állapotban szerkeszthető.
     */
    CLOSED("Lezárva"),

    /**
     * A szavazás nyitva áll a csatlakozásra a join kód segítségével.
     * Szavazni még nem lehet.
     */
    OPEN_FOR_JOINING("Csatlakozásra nyitva"),

    /**
     * A szavazás aktív, lehet szavazni.
     * Új résztvevők már nem csatlakozhatnak.
     */
    VOTING("Szavazás folyamatban"),

    /**
     * A szavazás lezárult, az eredmények megtekinthetők.
     * Szavazni már nem lehet.
     */
    RESULTS("Eredmények");

    private final String displayName; // Az ember által olvasható név

    PollState(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Visszaadja az állapot olvasható nevét (pl. GUI-ban való megjelenítéshez).
     * @return Az állapot olvasható neve.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Visszaadja az állapot olvasható nevét.
     * @return Az állapot olvasható neve.
     */
    @Override
    public String toString() {
        return displayName;
    }
}
