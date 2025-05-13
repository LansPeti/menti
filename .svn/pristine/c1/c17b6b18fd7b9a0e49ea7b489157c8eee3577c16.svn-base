package ppke.server;

import ppke.common.dto.PollDTO;
import ppke.common.dto.PollData;
import ppke.common.dto.VoteData;
import ppke.common.model.PollState;
import ppke.server.model.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Szavazások kezeléséért felelős osztály a szerver oldalon.
 * Felelős a szavazások teljes életciklusának menedzseléséért: létrehozás,
 * állapotváltozások kezelése, szavazatok fogadása, eredmények számítása,
 * adatok perzisztálása (közvetve, a {@link PersistenceManager}en keresztül),
 * és jogosultságok ellenőrzése a {@link UserManager} segítségével.
 * Kezeli a join kódokat (kis-nagybetű érzéketlenül, nagybetűsen tárolva).
 * Szálbiztos implementációt céloz meg {@link ConcurrentHashMap} és
 * a {@link Poll} objektumok belső lockjai segítségével.
 */
public class PollManager {

    /**
     * A létrehozott szavazásokat tároló map.
     * Kulcs: A szavazás NAGYBETŰS join kódja (String).
     * Érték: A {@link Poll} objektum.
     * {@link ConcurrentHashMap} biztosítja a szálbiztos műveleteket.
     */
    private final Map<String, Poll> pollsByJoinCode;

    /** Referencia a felhasználókat kezelő {@link UserManager}-re. */
    private final UserManager userManager;

    /** Reguláris kifejezés a join kód formátumának ellenőrzésére (pontosan 8 NAGYBETŰS alfanumerikus karakter). */
    private static final Pattern JOIN_CODE_PATTERN = Pattern.compile("^[A-Z0-9]{8}$");
    /** Karakterkészlet a join kód generálásához (csak nagybetűk és számok). */
    private static final String ALPHANUMERIC_CHARS_UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    /** A join kód elvárt hossza. */
    private static final int JOIN_CODE_LENGTH = 8;
    /** Véletlenszám-generátor a join kódokhoz. */
    private final Random random = new Random();

    /**
     * Konstruktor. Létrehoz egy üres szavazáskezelőt.
     * @param userManager A felhasználókat kezelő objektum (nem lehet null).
     */
    public PollManager(UserManager userManager) {
        this.pollsByJoinCode = new ConcurrentHashMap<>();
        this.userManager = Objects.requireNonNull(userManager, "A UserManager nem lehet null");
        System.out.println("PollManager inicializálva.");
    }

    /**
     * Létrehoz egy új szavazást a megadott adatokkal és join kóddal.
     * A join kódot nagybetűssé alakítja és validálja.
     * Ellenőrzi a join kód egyediségét.
     * Hozzárendeli a szavazást (nagybetűs kóddal) a létrehozó felhasználóhoz.
     * Szálbiztos.
     * @param creator A szavazást létrehozó User objektum.
     * @param pollData A szavazás adatai (típus, név, kérdés, opciók stb.).
     * @param joinCode A választott join kód (kis- és nagybetű érzéketlen).
     * @return A létrehozott Poll objektum, vagy null, ha a join kód érvénytelen vagy már foglalt, vagy a pollData érvénytelen.
     * @throws NullPointerException Ha creator, pollData vagy joinCode null.
     */
    public Poll createPoll(User creator, PollData pollData, String joinCode) {
        Objects.requireNonNull(creator, "A létrehozó nem lehet null");
        Objects.requireNonNull(pollData, "A szavazás adatai nem lehetnek null");
        Objects.requireNonNull(joinCode, "A join kód nem lehet null");
        Objects.requireNonNull(pollData.getType(), "A szavazás típusa nem lehet null"); // Biztosítjuk, hogy a getType() ne adjon null-t

        String upperCaseJoinCode = joinCode.toUpperCase(Locale.ROOT);

        if (!isValidJoinCodeFormat(upperCaseJoinCode)) {
            System.err.println("Érvénytelen join kód formátum: " + upperCaseJoinCode + " (8 nagybetűs alfanum karakter szükséges)");
            return null;
        }

        Poll newPoll;
        try {
            newPoll = switch (pollData.getType()) {
                case WORD_CLOUD -> new WordCloudPoll(creator.getUsername(), pollData.getName(), pollData.getQuestion(), upperCaseJoinCode);
                case MULTIPLE_CHOICE -> {
                    if (pollData.getOptions() == null || pollData.getOptions().size() < 2 || pollData.getOptions().stream().anyMatch(o -> o == null || o.trim().isEmpty())) {
                        System.err.println("Kísérlet MULTIPLE_CHOICE típusú szavazás létrehozására érvénytelen opciókkal.");
                        throw new IllegalArgumentException("A Multiple Choice szavazástípushoz legalább két, nem üres opció megadása kötelező.");
                    }
                    yield new MultipleChoicePoll(creator.getUsername(), pollData.getName(), pollData.getQuestion(), upperCaseJoinCode, pollData.getOptions());
                }
                case SCALE -> {
                    if (pollData.getAspects() == null || pollData.getAspects().isEmpty() || pollData.getAspects().stream().anyMatch(a -> a == null || a.trim().isEmpty())) {
                        System.err.println("Kísérlet SCALE típusú szavazás létrehozására szempontok nélkül vagy üres szemponttal.");
                        throw new IllegalArgumentException("A Scale szavazástípushoz legalább egy nem üres szempont megadása kötelező.");
                    }
                    if (pollData.getScaleMin() >= pollData.getScaleMax()) {
                        System.err.println("Kísérlet SCALE típusú szavazás létrehozására érvénytelen skálahatárokkal: min=" + pollData.getScaleMin() + ", max=" + pollData.getScaleMax());
                        throw new IllegalArgumentException("A skála minimum értékének (" + pollData.getScaleMin() + ") kisebbnek kell lennie a maximum értéknél (" + pollData.getScaleMax() + ").");
                    }
                    yield new ScalePoll(creator.getUsername(), pollData.getName(), pollData.getQuestion(), upperCaseJoinCode, pollData.getAspects(), pollData.getScaleMin(), pollData.getScaleMax());
                }
                default -> {
                    System.err.println("PollManager.createPoll: Nem támogatott vagy ismeretlen szavazás típus: " + pollData.getType());
                    throw new IllegalArgumentException("Nem támogatott szavazás típus: " + pollData.getType());
                }
            };
        } catch (IllegalArgumentException | NullPointerException e) {
            System.err.println("Hiba a Poll objektum létrehozásakor (" + upperCaseJoinCode + "): " + e.getMessage());
            return null;
        }

        Poll existingPoll = pollsByJoinCode.putIfAbsent(upperCaseJoinCode, newPoll);

        if (existingPoll == null) {
            try {
                creator.addPollCode(upperCaseJoinCode);
                System.out.println("Új szavazás létrehozva (" + upperCaseJoinCode + ") Típus: " + newPoll.getPollType() + ", Létrehozó: " + creator.getUsername());
                return newPoll;
            } catch (Exception e) {
                System.err.println("Hiba a szavazás kódjának hozzáadásakor a felhasználóhoz (" + creator.getUsername() + "), visszavonás: " + upperCaseJoinCode + " - " + e.getMessage());
                pollsByJoinCode.remove(upperCaseJoinCode, newPoll);
                return null;
            }
        } else {
            System.err.println("A(z) " + upperCaseJoinCode + " join kód már foglalt.");
            return null;
        }
    }

    /**
     * Töröl egy szavazást a join kódja alapján (kis-nagybetű érzéketlenül).
     * Csak a létrehozó felhasználó törölheti.
     * Szálbiztos.
     * @param user A törlést kezdeményező felhasználó.
     * @param joinCode A törlendő szavazás join kódja.
     * @return true, ha a törlés sikeres volt, false egyébként.
     */
    public boolean deletePoll(User user, String joinCode) {
        Objects.requireNonNull(user, "A felhasználó nem lehet null a törléshez");
        if (joinCode == null || joinCode.isBlank()) {
            System.err.println("Törlési kísérlet érvénytelen join kóddal.");
            return false;
        }

        String upperCaseJoinCode = joinCode.toUpperCase(Locale.ROOT);

        Poll removedPoll = pollsByJoinCode.computeIfPresent(upperCaseJoinCode, (code, poll) -> {
            if (poll.getCreatorUsername().equals(user.getUsername())) {
                return null; // Eltávolítás
            } else {
                System.err.println("Jogosulatlan törlési kísérlet ("+code+") - Kérte: "+ user.getUsername() +", Létrehozó: "+poll.getCreatorUsername());
                return poll; // Marad
            }
        });

        if (removedPoll != null) {
            boolean removedFromUser = user.removePollCode(upperCaseJoinCode);
            if (!removedFromUser) {
                System.err.println("Figyelmeztetés: A(z) " + upperCaseJoinCode + " szavazás törölve a PollManagerből, de nem volt regisztrálva a(z) " + user.getUsername() + " felhasználónál.");
            }
            System.out.println("Szavazás törölve (" + upperCaseJoinCode + "). Eltávolítva a felhasználótól: " + removedFromUser);
            return true;
        } else {
            if (!pollsByJoinCode.containsKey(upperCaseJoinCode) && removedPoll == null) {
                System.err.println("Törlési kísérlet sikertelen: Szavazás (" + upperCaseJoinCode + ") nem található.");
            } else if (removedPoll == null && pollsByJoinCode.containsKey(upperCaseJoinCode)) {
                System.err.println("Törlési kísérlet sikertelen: Ismeretlen hiba a(z) " + upperCaseJoinCode + " szavazás törlésekor.");
            }
            return false;
        }
    }

    /**
     * Meglévő szavazás nevének és kérdésének szerkesztése (kis-nagybetű érzéketlen kód).
     * Csak a létrehozó szerkesztheti, és csak akkor, ha a szavazás CLOSED állapotban van.
     * Az opciókat/aspektusokat/skálaértékeket nem lehet módosítani ezen a metóduson keresztül.
     * Szálbiztos (a Poll objektum belső lockjai miatt).
     * @param user A szerkesztést végző felhasználó.
     * @param joinCode A szerkesztendő szavazás join kódja.
     * @param updatedData Az új adatokat tartalmazó DTO (csak a name és question mezőket használja). A type mezőnek egyeznie kell.
     * @return true, ha a szerkesztés sikeres volt, false egyébként.
     */
    public boolean editPoll(User user, String joinCode, PollData updatedData) {
        Objects.requireNonNull(user, "A felhasználó nem lehet null a szerkesztéshez");
        Objects.requireNonNull(updatedData, "A frissített adatok nem lehetnek null");
        if (joinCode == null || joinCode.isBlank()) {
            System.err.println("Szerkesztési kísérlet érvénytelen join kóddal.");
            return false;
        }

        String upperCaseJoinCode = joinCode.toUpperCase(Locale.ROOT);
        Poll poll = pollsByJoinCode.get(upperCaseJoinCode);

        if (poll == null) {
            System.err.println("Szerkesztési kísérlet sikertelen: Szavazás (" + upperCaseJoinCode + ") nem található.");
            return false;
        }

        if (!poll.getCreatorUsername().equals(user.getUsername())) {
            System.err.println("Jogosulatlan szerkesztési kísérlet ("+upperCaseJoinCode+") - Kérte: "+ user.getUsername());
            return false;
        }

        if (poll.getPollType() != updatedData.getType()) {
            System.err.println("Szerkesztési kísérlet sikertelen: Típus eltérés ("+upperCaseJoinCode+"). A szavazás típusa nem módosítható.");
            return false;
        }

        boolean success = false;
        try {
            boolean nameChanged = poll.setName(updatedData.getName());
            boolean questionChanged = poll.setQuestion(updatedData.getQuestion());
            success = nameChanged || questionChanged;

            if (success) {
                System.out.println("Szavazás sikeresen szerkesztve: " + upperCaseJoinCode);
            } else {
                System.out.println("Szavazás szerkesztése nem történt meg (" + upperCaseJoinCode + ") - lehetséges okok: nem volt változás az adatokban, vagy a szavazás nem volt megfelelő állapotban (pl. nem CLOSED).");
            }
        } catch (IllegalArgumentException | NullPointerException e) {
            System.err.println("Hiba a szavazás szerkesztésekor ("+upperCaseJoinCode+"): Érvénytelen név vagy kérdés - " + e.getMessage());
            return false;
        } catch (IllegalStateException e) {
            System.err.println("Hiba a szavazás szerkesztésekor ("+upperCaseJoinCode+"): " + e.getMessage());
            return false;
        }
        return success;
    }


    /**
     * Visszaadja egy adott felhasználó által létrehozott összes szavazást (Poll objektumként),
     * név szerint rendezve.
     * @param user A felhasználó (nem lehet null).
     * @return A felhasználóhoz tartozó Poll objektumok listája. Lehet üres, de nem null.
     */
    public List<Poll> getPollsForUser(User user) {
        Objects.requireNonNull(user, "A felhasználó nem lehet null a szavazások lekéréséhez");
        List<Poll> userPolls = new ArrayList<>();
        List<String> codes = user.getCreatedPollJoinCodes();

        for (String joinCode : codes) {
            Poll poll = pollsByJoinCode.get(joinCode);
            if (poll != null) {
                if (poll.getCreatorUsername().equals(user.getUsername())) {
                    userPolls.add(poll);
                } else {
                    System.err.println("Adatkonzisztencia hiba? User " + user.getUsername() + " listájában szerepel a " + joinCode + " kód, de a PollManagerben tárolt szavazás létrehozója (" + poll.getCreatorUsername() + ") eltér.");
                }
            } else {
                System.err.println("Adatkonzisztencia hiba? User " + user.getUsername() + " listájában szerepel a " + joinCode + " kód, ami nincs a PollManager aktív szavazásai között. Lehet, hogy törölve lett, de a User objektum nem frissült?");
            }
        }
        userPolls.sort(Comparator.comparing(Poll::getName, String.CASE_INSENSITIVE_ORDER));
        return userPolls;
    }

    /**
     * Visszaadja egy adott felhasználó által létrehozott összes szavazást (PollDTO objektumként),
     * név szerint rendezve.
     * @param user A felhasználó (nem lehet null).
     * @return A felhasználóhoz tartozó PollDTO objektumok listája. Lehet üres, de nem null.
     */
    public List<PollDTO> getPollsForUserDTO(User user) {
        return getPollsForUser(user).stream()
                .map(Poll::toDTO)
                .collect(Collectors.toList());
    }


    /**
     * Visszaad egy szavazást a join kódja alapján (kis-nagybetű érzéketlenül).
     * @param joinCode A keresett join kód.
     * @return A {@link Poll} objektum, vagy null, ha nem található.
     */
    public Poll getPollByJoinCode(String joinCode) {
        if (joinCode == null || joinCode.isBlank()) return null;
        return pollsByJoinCode.get(joinCode.toUpperCase(Locale.ROOT));
    }

    /**
     * Validálja a join kódot csatlakozáshoz (kis-nagybetű érzéketlenül).
     * Ellenőrzi, hogy létezik-e ilyen kódú szavazás, és az {@link PollState#OPEN_FOR_JOINING} állapotban van-e.
     * @param joinCode A vizsgálandó join kód.
     * @return A {@link Poll} objektum, ha a kód érvényes és csatlakozásra nyitva áll, egyébként null.
     */
    public Poll validateJoinCodeForJoining(String joinCode) {
        if (joinCode == null || joinCode.isBlank()) return null;
        String upperCaseJoinCode = joinCode.toUpperCase(Locale.ROOT);
        Poll poll = pollsByJoinCode.get(upperCaseJoinCode);
        if (poll != null && poll.getCurrentState() == PollState.OPEN_FOR_JOINING) {
            System.out.println("Join kód (" + upperCaseJoinCode + ") sikeresen validálva csatlakozáshoz.");
            return poll;
        }
        if (poll != null) {
            System.out.println("Csatlakozás validálás sikertelen (" + upperCaseJoinCode + "): Érvénytelen állapot (" + poll.getCurrentState() + ")");
        } else {
            System.out.println("Csatlakozás validálás sikertelen (" + upperCaseJoinCode + "): Szavazás nem található");
        }
        return null;
    }

    /**
     * Fogad és feldolgoz egy szavazatot (kis-nagybetű érzéketlen kód).
     * Ellenőrzi, hogy a szavazás létezik-e, VOTING állapotban van-e,
     * és a szavazat adatai érvényesek-e a szavazás típusához.
     * A tényleges hozzáadást és validálást a {@link Poll#addVote(VoteData)} végzi.
     * Szálbiztos.
     * @param joinCode A szavazás join kódja.
     * @param voteData A leadott szavazat adatai (nem lehet null vagy üres).
     * @return true, ha a szavazat sikeresen fogadásra és rögzítésre került, false egyébként.
     */
    public boolean submitVote(String joinCode, VoteData voteData) {
        if (joinCode == null || joinCode.isBlank() || voteData == null || voteData.isEmpty()) {
            System.err.println("Szavazat leadási kísérlet érvénytelen adatokkal. Kód: " + joinCode + ", VoteData: " + voteData);
            return false;
        }

        Poll poll = getPollByJoinCode(joinCode);

        if (poll == null) {
            System.err.println("Szavazat leadási kísérlet nem létező szavazáshoz: " + joinCode.toUpperCase(Locale.ROOT));
            return false;
        }

        return poll.addVote(voteData);
    }

    /**
     * Megváltoztatja egy szavazás állapotát (kis-nagybetű érzéketlen kód).
     * Csak a létrehozó felhasználó végezheti el.
     * A {@link Poll#setState(PollState)} metódust hívja. Szálbiztos.
     * @param user A műveletet végző felhasználó.
     * @param joinCode A módosítandó szavazás join kódja.
     * @param newState Az új, kívánt állapot.
     * @return true, ha az állapotváltoztatás sikeres volt, false egyébként.
     */
    public boolean changePollState(User user, String joinCode, PollState newState) {
        Objects.requireNonNull(user, "A felhasználó nem lehet null az állapotváltáshoz");
        Objects.requireNonNull(newState, "Az új állapot nem lehet null");
        if (joinCode == null || joinCode.isBlank()) {
            System.err.println("Állapotváltási kísérlet érvénytelen join kóddal.");
            return false;
        }

        Poll poll = getPollByJoinCode(joinCode);

        if (poll == null) {
            System.err.println("Állapotváltási kísérlet sikertelen: Szavazás (" + joinCode.toUpperCase(Locale.ROOT) + ") nem található.");
            return false;
        }

        if (!poll.getCreatorUsername().equals(user.getUsername())) {
            System.err.println("Jogosulatlan állapotváltási kísérlet ("+poll.getJoinCode()+") - Kérte: "+ user.getUsername());
            return false;
        }

        return poll.setState(newState);
    }

    /**
     * Nullázza egy szavazás eredményeit (kis-nagybetű érzéketlen kód).
     * Csak a létrehozó felhasználó végezheti el.
     * A {@link Poll#resetResults()} metódust hívja. Szálbiztos.
     * @param user A műveletet végző felhasználó.
     * @param joinCode A nullázandó szavazás join kódja.
     * @return true, ha a nullázás sikeres volt, false egyébként (pl. ha a szavazás nem létezik, vagy nem a useré, vagy hiba történt a nullázás során).
     */
    public boolean resetPollResults(User user, String joinCode) {
        Objects.requireNonNull(user, "A felhasználó nem lehet null az eredmény nullázáshoz");
        if (joinCode == null || joinCode.isBlank()) {
            System.err.println("Eredmény nullázási kísérlet érvénytelen join kóddal.");
            return false;
        }

        Poll poll = getPollByJoinCode(joinCode);

        if (poll == null) {
            System.err.println("Eredmény nullázási kísérlet sikertelen: Szavazás (" + joinCode.toUpperCase(Locale.ROOT) + ") nem található.");
            return false;
        }

        if (!poll.getCreatorUsername().equals(user.getUsername())) {
            System.err.println("Jogosulatlan eredmény nullázási kísérlet ("+poll.getJoinCode()+") - Kérte: "+ user.getUsername());
            return false;
        }

        try {
            poll.resetResults(); // Feltételezzük, hogy ez void és kivételt dobhat, ha nem sikerül (pl. rossz állapot)
            System.out.println("Szavazás (" + poll.getJoinCode() + ") eredményei sikeresen nullázva.");
            return true; // Sikeres, ha nem dobott kivételt
        } catch (IllegalStateException e) { // Specifikus kivétel elkapása, ha a Poll modell így jelzi a hibát
            System.err.println("Szavazás (" + poll.getJoinCode() + ") eredményeinek nullázása sikertelen (nem megfelelő állapot): " + e.getMessage());
            return false;
        } catch (Exception e) { // Általánosabb kivétel elkapása egyéb hibákra
            System.err.println("Váratlan hiba a szavazás (" + poll.getJoinCode() + ") eredményeinek nullázása közben: " + e.getMessage());

            return false;
        }
    }


    /**
     * Lekéri az összes tárolt szavazást (pl. mentéshez).
     * A kulcsok nagybetűsek.
     * @return Az összes szavazás map-jének másolata (a belső map módosítása nem ajánlott).
     */
    public Map<String, Poll> getAllPolls() {
        return new ConcurrentHashMap<>(pollsByJoinCode);
    }

    /**
     * Betölti a szavazásokat (pl. perzisztenciából).
     * Feltételezi, hogy a betöltött map kulcsai már nagybetűsek.
     * Ellenőrzi a betöltött adatok konzisztenciáját.
     * Felülírja a jelenlegi szavazásokat.
     * @param loadedPolls A betöltött szavazások map-je (nem lehet null).
     */
    public void setPolls(Map<String, Poll> loadedPolls) {
        Objects.requireNonNull(loadedPolls, "A betöltött szavazások map nem lehet null");
        Map<String, Poll> validatedPolls = new ConcurrentHashMap<>();
        int skipped = 0;
        for (Map.Entry<String, Poll> entry : loadedPolls.entrySet()) {
            String code = entry.getKey();
            Poll poll = entry.getValue();

            boolean creatorExists = false;
            if (poll != null && poll.getCreatorUsername() != null && !poll.getCreatorUsername().isBlank()) {
                // Javítás: userManager.getUsers().containsKey() használata a getUserByUsername helyett
                creatorExists = userManager.getUsers().containsKey(poll.getCreatorUsername());
            }

            if (code != null && poll != null &&
                    isValidJoinCodeFormat(code) && code.equals(poll.getJoinCode()) &&
                    poll.getCreatorUsername() != null && !poll.getCreatorUsername().isBlank() &&
                    creatorExists) {
                validatedPolls.put(code, poll);
            } else {
                skipped++;
                System.err.println("Érvénytelen vagy inkonzisztens szavazás bejegyzés kihagyva betöltéskor. Kód: " + code +
                        ", Poll Join Kód: " + (poll != null ? poll.getJoinCode() : "N/A") +
                        ", Létrehozó: " + (poll != null ? poll.getCreatorUsername() : "N/A") +
                        ", Létrehozó létezik: " + creatorExists );
            }
        }
        pollsByJoinCode.clear();
        pollsByJoinCode.putAll(validatedPolls);
        System.out.println(pollsByJoinCode.size() + " érvényes szavazás betöltve a PollManagerbe (" + skipped + " kihagyva).");
    }

    /**
     * Generál egy egyedi, 8 karakteres, NAGYBETŰS alfanumerikus join kódot.
     * Addig próbálkozik, amíg nem talál olyat, ami még nem létezik a rendszerben (max 1000 próba).
     * Szálbiztos.
     * @return Az egyedi, nagybetűs join kód.
     * @throws RuntimeException ha nem sikerül egyedi kódot generálni max próbálkozás után.
     */
    public String generateUniqueJoinCode() {
        String code;
        int maxTries = 1000;
        int tries = 0;
        do {
            if (tries++ >= maxTries) {
                System.err.println("KRITIKUS: Nem sikerült egyedi join kódot generálni " + maxTries + " próbálkozás után. Lehet, hogy a kódtér megtelt, vagy a random generátor gyenge.");
                throw new RuntimeException("Nem sikerült egyedi join kódot generálni " + maxTries + " próbálkozás után.");
            }
            StringBuilder sb = new StringBuilder(JOIN_CODE_LENGTH);
            for (int i = 0; i < JOIN_CODE_LENGTH; i++) {
                sb.append(ALPHANUMERIC_CHARS_UPPER.charAt(random.nextInt(ALPHANUMERIC_CHARS_UPPER.length())));
            }
            code = sb.toString();
        } while (pollsByJoinCode.containsKey(code));
        System.out.println("Generált egyedi join kód: " + code + " ("+tries+" próbálkozás)");
        return code;
    }

    /**
     * Ellenőrzi, hogy a megadott string megfelel-e a join kód formátumának
     * (pontosan 8 NAGYBETŰS alfanumerikus karakter).
     * @param code A vizsgálandó kód (feltételezzük, hogy már nagybetűs, vagy a hívó felelős érte).
     * @return true, ha a formátum érvényes, false egyébként.
     */
    private boolean isValidJoinCodeFormat(String code) {
        return code != null && JOIN_CODE_PATTERN.matcher(code).matches();
    }
}
