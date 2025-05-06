package ppke.common.dto;
/** Kérés egy szavazáshoz való csatlakozásra a join kód megadásával (szavazóként). */
public record JoinPollRequest(String joinCode) implements Request { private static final long serialVersionUID = 1L; }
