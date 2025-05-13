package ppke.common.model;

/**
 * A lehetséges szavazástípusokat definiáló enum.
 * Minden típusnak van egy neve.
 */
public enum PollType {
    /**
     * Szófelhő: A válaszként kapott szavak gyakoriságát vizualizálja.
     */
    WORD_CLOUD("Szófelhő"),

    /**
     * Feleletválasztós: Előre megadott opciók közül lehet egyet választani.
     */
    MULTIPLE_CHOICE("Feleletválasztós"),

    /**
     * Skála: Egy vagy több szempont értékelése egy numerikus skálán.
     */
    SCALE("Skála");

    /** A típus neve. */
    private final String displayName;

    /**
     * Enum konstruktor.
     * @param displayName A típushoz tartozó név.
     */
    PollType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Visszaadja nevét (pl. GUI-ban való megjelenítéshez).
     * @return A típus neve.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Visszaadja a típus nevét (toString felüldefiniálása).
     * @return A típus neve.
     */
    @Override
    public String toString() {
        return displayName;
    }
}