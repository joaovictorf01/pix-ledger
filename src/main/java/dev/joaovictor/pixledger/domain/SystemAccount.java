package dev.joaovictor.pixledger.domain;

/**
 * Accounts that exist from the start, created by migration V3.
 * WORLD stands for every other institution on the Pix network and exists in
 * both ledgers, because each ledger must balance on its own.
 */
public enum SystemAccount {

    CUSTOMER_WORLD(Ledger.CUSTOMER, "WORLD"),
    INTERNAL_WORLD(Ledger.INTERNAL, "WORLD"),
    CUSTOMER_FUNDS(Ledger.INTERNAL, "CUSTOMER_FUNDS"),
    INSTANT_PAYMENTS(Ledger.INTERNAL, "INSTANT_PAYMENTS"),
    FINANCIAL_RESERVE(Ledger.INTERNAL, "FINANCIAL_RESERVE");

    private final Ledger ledger;
    private final String code;

    SystemAccount(Ledger ledger, String code) {
        this.ledger = ledger;
        this.code = code;
    }

    public Ledger ledger() {
        return ledger;
    }

    public String code() {
        return code;
    }
}
