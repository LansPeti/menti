package ppke.common.model;

/**
 * A lehetséges szavazástípusokat definiáló enum.
 * Minden típusnak van egy ember által olvasható, magyar neve.
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
     * (Ez a típus a projektben NINCS implementálva.)
     */
    SCALE("Skála"); // Bár nincs implementálva, az enum tartalmazhatja

    /** A típus magyar, olvasható neve. */
    private final String displayName;

    /**
     * Enum konstruktor.
     * @param displayName A típushoz tartozó magyar megjelenítendő név.
     */
    PollType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Visszaadja a típus magyar, olvasható nevét (pl. GUI-ban való megjelenítéshez).
     * @return A típus magyar, olvasható neve.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Visszaadja a típus magyar, olvasható nevét (toString felüldefiniálása).
     * @return A típus magyar, olvasható neve.
     */
    @Override
    public String toString() {
        return displayName;
    }
}