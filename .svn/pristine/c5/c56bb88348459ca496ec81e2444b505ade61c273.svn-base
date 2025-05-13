package ppke.common.dto;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

/**
 * Adatátviteli Objektum (DTO) a leadott szavazat adatainak átadására.
 * A konkrét tartalom a szavazás típusától függ (pl. szavak listája, kiválasztott opció).
 * Statikus factory metódusokat biztosít a különböző típusú szavazatok létrehozására.
 * A Scale típus adatai is szerepelnek, de a projektben nincsenek használva.
 */
public class VoteData implements Serializable {
    /** Szerializációs verzióazonosító. */
    private static final long serialVersionUID = 1L;

    /** Szavak listája (Word Cloud). */
    private List<String> words;
    /** A kiválasztott opció (Multiple Choice). */
    private String selectedOption;
    /** Szempontokhoz adott értékelések (Scale - NINCS HASZNÁLVA). */
    private Map<String, Integer> aspectRatings;

    /**
     * Privát alapértelmezett konstruktor.
     * A példányosítás a statikus factory metódusokon keresztül történik.
     */
    private VoteData() {}

    /**
     * Létrehoz egy VoteData objektumot Word Cloud szavazathoz.
     * @param words A beküldött szavak listája (nem lehet null). A lista másolatát tárolja.
     * @return Új VoteData objektum.
     * @throws NullPointerException ha a words lista null.
     */
    public static VoteData forWordCloud(List<String> words) {
        Objects.requireNonNull(words, "Words list cannot be null for WordCloud vote.");
        VoteData vd = new VoteData();
        vd.words = new ArrayList<>(words);
        return vd;
    }

    /**
     * Létrehoz egy VoteData objektumot Multiple Choice szavazathoz.
     * @param selectedOption A kiválasztott opció (nem lehet null vagy üres).
     * @return Új VoteData objektum.
     * @throws NullPointerException ha az opció null.
     * @throws IllegalArgumentException ha az opció üres vagy csak whitespace.
     */
    public static VoteData forMultipleChoice(String selectedOption) {
        Objects.requireNonNull(selectedOption, "Selected option cannot be null for MultipleChoice vote.");
        if (selectedOption.isBlank()) {
            throw new IllegalArgumentException("Selected option cannot be blank.");
        }
        VoteData vd = new VoteData();
        vd.selectedOption = selectedOption;
        return vd;
    }

    /**
     * Létrehoz egy VoteData objektumot Scale szavazathoz
     * @param aspectRatings A szempontokhoz adott értékelések (nem lehet null). A map másolatát tárolja.
     * @return Új VoteData objektum.
     * @throws NullPointerException ha a map null.
     */
    public static VoteData forScale(Map<String, Integer> aspectRatings) {
        Objects.requireNonNull(aspectRatings, "Aspect ratings map cannot be null for Scale vote.");
        VoteData vd = new VoteData();
        vd.aspectRatings = new HashMap<>(aspectRatings);
        return vd;
    }

    // --- GETTEREK (Defenzív másolatokkal) ---
    /** @return A szavak listájának másolata (Word Cloud), vagy null, ha nem ilyen típusú. */
    public List<String> getWords() { return words == null ? null : new ArrayList<>(words); }
    /** @return A kiválasztott opció (Multiple Choice), vagy null, ha nem ilyen típusú. */
    public String getSelectedOption() { return selectedOption; }
    /** @return A szempont értékelések mapjének másolata (Scale), vagy null, ha nem ilyen típusú. */
    public Map<String, Integer> getAspectRatings() { return aspectRatings == null ? null : new HashMap<>(aspectRatings); }

    /**
     * Ellenőrzi, hogy a szavazat tartalmaz-e érvényes adatot.
     * Üresnek számít, ha a típusának megfelelő adat hiányzik vagy érvénytelen (pl. csak üres szavak).
     * @return true, ha a szavazat üres, false egyébként.
     */
    public boolean isEmpty() {
        boolean wordsEmpty = (words == null || words.stream().allMatch(s -> s == null || s.trim().isEmpty()));
        boolean optionEmpty = (selectedOption == null || selectedOption.trim().isEmpty());
        boolean ratingsEmpty = (aspectRatings == null || aspectRatings.isEmpty());
        // Akkor üres, ha MINDEN lehetséges adattípus üres/hiányzik
        return wordsEmpty && optionEmpty && ratingsEmpty;
    }

    /**
     * Visszaadja az objektum szöveges reprezentációját (debuggoláshoz).
     * @return Az objektum string formában.
     */
    @Override
    public String toString() {
        if (words != null) return "VoteData{type=WC, words=" + words + '}';
        if (selectedOption != null) return "VoteData{type=MC, selectedOption='" + selectedOption + '\'' + '}';
        if (aspectRatings != null) return "VoteData{type=SCALE, aspectRatings=" + aspectRatings + '}';
        return "VoteData{empty}";
    }
}