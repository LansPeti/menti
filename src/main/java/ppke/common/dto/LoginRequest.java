package ppke.common.dto;
/** Kérés a felhasználó bejelentkeztetésére. */
public record LoginRequest(String username, String password) implements Request { private static final long serialVersionUID = 1L; }
