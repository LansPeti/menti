package ppke.common.dto;
/**
 * Értesítés egy szavazás adatainak vagy állapotának frissüléséről.
 * Tartalmazza a teljes, frissített {@link PollDTO}-t, beleértve az esetleges eredményeket is,
 * ha az állapot {@link ppke.common.model.PollState#RESULTS}.
 * Ezt küldi a szerver pl. állapotváltáskor (VOTING, RESULTS), szerkesztés vagy reset után.
 */
public record PollUpdateNotification(PollDTO updatedPoll) implements Response { private static final long serialVersionUID = 1L; }
