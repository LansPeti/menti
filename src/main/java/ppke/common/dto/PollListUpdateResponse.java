package ppke.common.dto;
import java.util.List;
/** Válasz vagy értesítés, amely a felhasználó szavazásainak frissített listáját tartalmazza (pl. létrehozás vagy törlés után). */
public record PollListUpdateResponse(List<PollDTO> userPolls) implements Response { private static final long serialVersionUID = 1L; }
