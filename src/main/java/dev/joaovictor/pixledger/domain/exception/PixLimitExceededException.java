package dev.joaovictor.pixledger.domain.exception;

import java.util.UUID;

public class PixLimitExceededException extends RuntimeException {

    public PixLimitExceededException(UUID accountId, long limit, long spentToday, long amount) {
        super("account " + accountId + " has a daily limit of " + limit + ", already sent " + spentToday
                + " today and cannot send " + amount);
    }
}
