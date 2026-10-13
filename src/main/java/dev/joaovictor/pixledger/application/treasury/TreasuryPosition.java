package dev.joaovictor.pixledger.application.treasury;

/**
 * Balances of the internal accounts and what they are measured against, in cents.
 *
 * @param customerTotal         sum of all customer balances
 * @param reserveAdvance        what the Financial Reserve has lent to Instant Payments
 * @param instantPaymentsTarget what Instant Payments must hold so no legitimate Pix fails
 */
public record TreasuryPosition(long customerTotal, long customerFunds, long instantPayments,
        long financialReserve, long reserveCapital, long reserveAdvance, long instantPaymentsTarget) {

    /**
     * The challenge's invariant, extended with the reserve advance: customer
     * money is in Customer Funds plus Instant Payments, minus what the
     * Reserve put there of its own.
     */
    public long customerMoneyDifference() {
        return customerFunds + instantPayments - reserveAdvance - customerTotal;
    }
}
