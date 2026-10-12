package dev.joaovictor.pixledger.domain.exception;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String account) {
        super("account not found: " + account);
    }
}
