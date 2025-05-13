package ppke.common.dto;
/** Kérés egy adott szavazás eseményeire (pl. állapotváltozás) való feliratkozáshoz (szavazóként). */
public record SubscribeToPollRequest(String joinCode) implements Request { private static final long serialVersionUID = 1L; }
