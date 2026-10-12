package dev.joaovictor.pixledger.domain;

import java.util.UUID;

public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(UUID accountId, long balance, long amount) {
        super("account " + accountId + " has " + balance + " and cannot send " + amount);
    }
}
