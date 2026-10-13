package dev.joaovictor.pixledger.domain.exception;

import java.time.LocalDate;
import java.time.LocalTime;

public class ClosingNotDueException extends RuntimeException {

    public ClosingNotDueException(LocalDate businessDay, LocalTime closingTime) {
        super("the closing of " + businessDay + " can only run from " + closingTime + " (Brasília)");
    }
}
