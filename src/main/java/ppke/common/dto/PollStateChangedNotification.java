package ppke.common.dto;
import ppke.common.model.PollState;
/**
 * Értesítés egy szavazás állapotának megváltozásáról.
 * Elsősorban általános állapotfrissítésre használjuk (pl. a profil listában),
 * a részletesebb frissítésekhez (pl. képernyőváltás) a {@link PollUpdateNotification} ajánlott.
 * Tartalmazhatja az eredményeket is, ha az új állapot RESULTS, de ez redundáns lehet, ha PollUpdateNotification is van.
 * @param joinCode Az érintett szavazás kódja.
 * @param newState Az új állapot.
 * @param results Az eredmények (opcionális, csak RESULTS állapotnál).
 */
public record PollStateChangedNotification(String joinCode, PollState newState, Object results) implements Response { private static final long serialVersionUID = 1L; }
