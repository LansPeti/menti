package ppke.common.dto;
/** Kérés egy szavazás eredményeinek (leadott szavazatok) nullázására. */
public record ResetResultsRequest(String joinCode) implements Request { private static final long serialVersionUID = 1L; }
