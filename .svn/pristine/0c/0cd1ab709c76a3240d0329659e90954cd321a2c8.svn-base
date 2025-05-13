package ppke.common.dto;
import ppke.common.model.PollType;
import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

/**
 * Adatátviteli Objektum (DTO) a szavazás létrehozásához vagy szerkesztéséhez szükséges adatok átadására.
 * Tartalmazza a szavazás általános adatait (típus, név, kérdés) és a típus-specifikus beállításokat
 * (pl. opciók Multiple Choice esetén, szempontok és skálaértékek Scale esetén).
 */
public class PollData implements Serializable {
    /** Szerializációs verzióazonosító. */
    private static final long serialVersionUID = 1L;

    private PollType type;
    private String name;
    private String question;

    // Multiple Choice specifikus
    private List<String> options;

    // Scale specifikus
    private List<String> aspects;
    private int scaleMin;
    private int scaleMax;

    /**
     * Alap konstruktor. Inicializálja a listákat üres ArrayList-ként a NullPointerException elkerülése végett,
     * és beállít alapértelmezett értékeket a skálához.
     */
    public PollData() {
        this.options = new ArrayList<>();
        this.aspects = new ArrayList<>();
        this.scaleMin = 0;  // Alapértelmezett minimum
        this.scaleMax = 10; // Alapértelmezett maximum
    }

    // --- GETTEREK ---
    /** @return A szavazás típusa. */
    public PollType getType() { return type; }
    /** @return A szavazás neve. */
    public String getName() { return name; }
    /** @return A szavazás kérdése. */
    public String getQuestion() { return question; }
    /** @return Az opciók listájának másolata (Multiple Choice). Soha nem null. */
    public List<String> getOptions() { return options == null ? new ArrayList<>() : new ArrayList<>(options); }
    /** @return A szempontok listájának másolata (Scale). Soha nem null. */
    public List<String> getAspects() { return aspects == null ? new ArrayList<>() : new ArrayList<>(aspects); }
    /** @return A skála minimum értéke (Scale). */
    public int getScaleMin() { return scaleMin; }
    /** @return A skála maximum értéke (Scale). */
    public int getScaleMax() { return scaleMax; }

    // --- SETTEREK (Fluent API stílusban) ---
    /** Beállítja a szavazás típusát. @param type A szavazás típusa. @return Ez az objektum (láncoláshoz). */
    public PollData setType(PollType type) { this.type = type; return this; }
    /** Beállítja a szavazás nevét. @param name A szavazás neve. @return Ez az objektum (láncoláshoz). */
    public PollData setName(String name) { this.name = name; return this; }
    /** Beállítja a szavazás kérdését. @param question A szavazás kérdése. @return Ez az objektum (láncoláshoz). */
    public PollData setQuestion(String question) { this.question = question; return this; }
    /** Beállítja az opciókat (Multiple Choice). A kapott lista másolatát tárolja. Null lista esetén üres listát hoz létre. @param options Az opciók listája. @return Ez az objektum (láncoláshoz). */
    public PollData setOptions(List<String> options) { this.options = (options != null) ? new ArrayList<>(options) : new ArrayList<>(); return this; }
    /** Beállítja a szempontokat (Scale). A kapott lista másolatát tárolja. Null lista esetén üres listát hoz létre. @param aspects A szempontok listája. @return Ez az objektum (láncoláshoz). */
    public PollData setAspects(List<String> aspects) { this.aspects = (aspects != null) ? new ArrayList<>(aspects) : new ArrayList<>(); return this; }
    /** Beállítja a skála minimum értékét (Scale). @param scaleMin A minimum érték. @return Ez az objektum (láncoláshoz). */
    public PollData setScaleMin(int scaleMin) { this.scaleMin = scaleMin; return this; }
    /** Beállítja a skála maximum értékét (Scale). @param scaleMax A maximum érték. @return Ez az objektum (láncoláshoz). */
    public PollData setScaleMax(int scaleMax) { this.scaleMax = scaleMax; return this; }

    /**
     * Visszaadja az objektum szöveges reprezentációját (debuggoláshoz).
     * @return Az objektum string formában.
     */
    @Override
    public String toString() {
        return "PollData{" +
                "type=" + type +
                ", name='" + name + '\'' +
                ", question='" + question + '\'' +
                ", options=" + options +
                ", aspects=" + aspects +
                ", scaleMin=" + scaleMin +
                ", scaleMax=" + scaleMax +
                '}';
    }
}
