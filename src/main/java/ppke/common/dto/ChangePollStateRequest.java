package ppke.common.dto;
import ppke.common.model.PollState;
/** Kérés egy szavazás állapotának megváltoztatására (ezt csak a létrehozó teheti meg). */
public record ChangePollStateRequest(String joinCode, PollState newState) implements Request { private static final long serialVersionUID = 1L; }
