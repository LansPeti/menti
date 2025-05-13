package ppke.server.model;

import ppke.common.dto.VoteData;
import ppke.common.model.PollType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Word Cloud (szófelhő) típusú szavazás implementációja.
 * Összegyűjti a szavazatokban kapott szavakat (max {@value #MAX_WORDS_PER_VOTE}/szavazat,
 * max {@value #MAX_WORD_LENGTH} karakter/szó) és megszámolja azok normalizált
 * (kisbetűs, trimmelt) gyakoriságát.
 */
public class WordCloudPoll extends Poll {
    /** Szerializációs verzióazonosító. */
    private static final long serialVersionUID = 1L;

    /**
     * A szógyakoriságokat tároló map (normalizált szó -> előfordulás száma).
     * {@link ConcurrentHashMap} a szálbiztos műveletekhez.
     */
    private Map<String, Integer> wordCounts;

    /** Maximálisan elfogadott szavak száma egyetlen szavazatban. */
    private static final int MAX_WORDS_PER_VOTE = 3;
    /** Maximálisan elfogadott karakterszám egyetlen szóban. */
    private static final int MAX_WORD_LENGTH = 25;

    /**
     * Konstruktor Word Cloud szavazáshoz.
     * @param creatorUsername Létrehozó felhasználóneve.
     * @param name Szavazás neve.
     * @param question Feltett kérdés.
     * @param joinCode Csatlakozási kód (nagybetűs).
     */
    public WordCloudPoll(String creatorUsername, String name, String question, String joinCode) {
        super(creatorUsername, name, question, joinCode);
        this.wordCounts = new ConcurrentHashMap<>();
    }

    /**
     * {@inheritDoc}
     * @return Mindig {@link PollType#WORD_CLOUD}.
     */
    @Override
    public PollType getPollType() {
        return PollType.WORD_CLOUD;
    }

    /**
     * {@inheritDoc}
     * Ellenőrzi, hogy a {@link VoteData} tartalmaz-e szavakat (`getWords()`),
     * legfeljebb {@value #MAX_WORDS_PER_VOTE} darabot,
     * és van-e közöttük legalább egy érvényes (nem null, nem üres, nem hosszabb mint {@value #MAX_WORD_LENGTH}).
     * Ha bármelyik szó túl hosszú, az egész szavazat érvénytelen.
     * @param voteData A validálandó szavazat.
     * @return true, ha a szavazat érvényes, false egyébként.
     */
    @Override
    public boolean isValidVote(VoteData voteData) {
        if (voteData == null || voteData.getWords() == null) return false;
        List<String> words = voteData.getWords();

        if (words.size() > MAX_WORDS_PER_VOTE) {
            System.err.println("Poll ("+joinCode+"): WC vote rejected. Too many words: " + words.size());
            return false;
        }

        boolean hasValidWord = false;
        for (String word : words) {
            if (word != null) {
                String trimmed = word.trim();
                if (!trimmed.isEmpty()) {
                    if (trimmed.length() > MAX_WORD_LENGTH) {
                        System.err.println("Poll ("+joinCode+"): WC vote rejected. Word too long ("+trimmed.length()+"): '" + trimmed + "'");
                        return false; // Túl hosszú szó érvényteleníti az egészet
                    }
                    hasValidWord = true;
                }
            }
        }
        if (!hasValidWord) {
            System.err.println("Poll ("+joinCode+"): WC vote rejected. No valid words found in: " + words);
        }
        return hasValidWord;
    }

    /**
     * {@inheritDoc}
     * Kiszámítja és eltárolja a szógyakoriságokat a leadott érvényes szavazatok alapján.
     * Normalizálja a szavakat (kisbetű, trim) és figyelmen kívül hagyja az érvényteleneket (túl hosszú).
     * A számítás írási lock alatt fut.
     */
    @Override
    protected void calculateResults() {
        // Írási lock alatt fut az ősosztályból.
        Map<String, Integer> newCounts = new ConcurrentHashMap<>();
        synchronized (votes) { // Iterációhoz lockoljuk a listát
            for (VoteData vote : votes) {
                if (vote != null && vote.getWords() != null) {
                    for (String word : vote.getWords()) {
                        if (word != null) {
                            String trimmedWord = word.trim().toLowerCase(Locale.ROOT);
                            // Csak az érvényes hosszúságúakat számoljuk
                            if (!trimmedWord.isEmpty() && trimmedWord.length() <= MAX_WORD_LENGTH) {
                                // compute atomikusan növeli a számlálót
                                newCounts.compute(trimmedWord, (key, count) -> (count == null) ? 1 : count + 1);
                            }
                        }
                    }
                }
            }
        }
        this.wordCounts = newCounts;
        System.out.println("Poll (" + joinCode + "): WordCloud results recalculated. Unique valid words: " + wordCounts.size());
    }

    /**
     * {@inheritDoc}
     * Visszaadja a formázott eredményeket: a szógyakoriságokat tartalmazó map másolatát.
     * A map kulcsai a normalizált (kisbetűs, trimmelt) szavak, az értékek a gyakoriságok.
     * Olvasási lock alatt fut.
     * @return {@code Map<String, Integer>} a szógyakoriságokkal, vagy üres map, ha nincs eredmény. Soha nem null.
     */
    @Override
    public Object getFormattedResults() {
        // Olvasási lock alatt fut az ősosztályból.
        return new HashMap<>(wordCounts); // Védelmi másolat
    }

    /**
     * {@inheritDoc}
     * Törli a kiszámított szógyakoriságokat. Írási lock alatt fut.
     */
    @Override
    protected void clearCalculatedResults() {
        // Írási lock alatt fut az ősosztályból.
        wordCounts.clear();
    }
}