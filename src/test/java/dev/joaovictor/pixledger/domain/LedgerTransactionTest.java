package dev.joaovictor.pixledger.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.joaovictor.pixledger.domain.exception.InvalidTransactionException;

class LedgerTransactionTest {

    private final UUID customerFunds = UUID.randomUUID();
    private final UUID instantPayments = UUID.randomUUID();

    @Test
    void createsBalancedTransactionWithOneEntryPerPosting() {
        LedgerTransaction transaction = LedgerTransaction.create("op-1", Ledger.INTERNAL, List.of(
                new Posting(customerFunds, -50),
                new Posting(instantPayments, 50)));

        assertThat(transaction.getEntries()).hasSize(2);
        assertThat(transaction.getEntries())
                .allSatisfy(entry -> {
                    assertThat(entry.getTransactionId()).isEqualTo(transaction.getId());
                    assertThat(entry.getLedger()).isEqualTo(Ledger.INTERNAL);
                });
        assertThat(transaction.getEntries().stream().mapToLong(LedgerEntry::getAmount).sum()).isZero();
    }

    @Test
    void rejectsSinglePosting() {
        assertThatThrownBy(() -> LedgerTransaction.create("op-1", Ledger.INTERNAL, List.of(
                new Posting(instantPayments, 50))))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("at least 2");
    }

    @Test
    void rejectsPostingsThatDoNotAddUpToZero() {
        assertThatThrownBy(() -> LedgerTransaction.create("op-1", Ledger.INTERNAL, List.of(
                new Posting(customerFunds, -50),
                new Posting(instantPayments, 40))))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("got -10");
    }

    @Test
    void rejectsBlankIdempotencyKey() {
        assertThatThrownBy(() -> LedgerTransaction.create(" ", Ledger.INTERNAL, List.of(
                new Posting(customerFunds, -50),
                new Posting(instantPayments, 50))))
                .isInstanceOf(InvalidTransactionException.class);
    }

    @Test
    void rejectsZeroAmountPosting() {
        assertThatThrownBy(() -> LedgerTransaction.create("op-1", Ledger.INTERNAL, List.of(
                new Posting(customerFunds, 0),
                new Posting(instantPayments, 0))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsSumThatOverflows() {
        assertThatThrownBy(() -> LedgerTransaction.create("op-1", Ledger.INTERNAL, List.of(
                new Posting(customerFunds, Long.MAX_VALUE),
                new Posting(instantPayments, 1),
                new Posting(customerFunds, Long.MIN_VALUE))))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("overflow");
    }
}
