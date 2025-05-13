package ppke.common.dto;
/** Kérés új felhasználó regisztrálására. */
public record RegisterRequest(String username, String password) implements Request { private static final long serialVersionUID = 1L; }
