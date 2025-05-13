package ppke.common.dto;
/** Kérés egy szavazat leadására egy adott szavazáshoz. */
public record SubmitVoteRequest(String joinCode, VoteData voteData) implements Request { private static final long serialVersionUID = 1L; }
