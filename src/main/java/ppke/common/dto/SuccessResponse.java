package ppke.common.dto;
/** Általános válasz sikeres művelet esetén, opcionális szöveges üzenettel. */
public record SuccessResponse(String message) implements Response {
    private static final long serialVersionUID = 1L;
    /** Konstruktor üzenet nélküli sikeres válaszhoz. */
    public SuccessResponse() { this(null); }
}