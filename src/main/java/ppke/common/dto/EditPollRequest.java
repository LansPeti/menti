package ppke.common.dto;
/** Kérés meglévő szavazás nevének és/vagy kérdésének szerkesztésére. */
public record EditPollRequest(String joinCode, PollData pollData) implements Request { private static final long serialVersionUID = 1L; }
