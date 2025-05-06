package ppke.common.dto;
import ppke.common.model.PollState;
/** Válasz sikeres szavazáshoz csatlakozás esetén. Tartalmazza a szavazás kódját, nevét és aktuális állapotát. */
public record JoinSuccessResponse(String joinCode, String pollName, PollState initialState) implements Response { private static final long serialVersionUID = 1L; }
