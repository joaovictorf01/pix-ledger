package dev.joaovictor.pixledger.domain.exception;

public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String endToEndId) {
        super("endToEndId " + endToEndId + " was already used for a different Pix");
    }
}
