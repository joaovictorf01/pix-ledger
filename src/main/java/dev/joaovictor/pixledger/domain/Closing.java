package dev.joaovictor.pixledger.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Record of one daily closing. The business day is the key, so a day closes once. */
@Entity
@Table(name = "closings")
public class Closing {

    @Id
    private LocalDate businessDay;
    @Column(nullable = false)
    private Instant closedAt;
    @Column(nullable = false)
    private long customerTotal;
    @Column(nullable = false)
    private long customerFunds;
    @Column(nullable = false)
    private boolean customerFundsMatch;

    protected Closing() {
    }

    public Closing(LocalDate businessDay, Instant closedAt, long customerTotal, long customerFunds) {
        Objects.requireNonNull(businessDay, "businessDay must not be null");
        Objects.requireNonNull(closedAt, "closedAt must not be null");

        this.businessDay = businessDay;
        this.closedAt = closedAt;
        this.customerTotal = customerTotal;
        this.customerFunds = customerFunds;
        this.customerFundsMatch = customerFunds == customerTotal;
    }

    public LocalDate getBusinessDay() {
        return businessDay;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public long getCustomerTotal() {
        return customerTotal;
    }

    public long getCustomerFunds() {
        return customerFunds;
    }

    public boolean isCustomerFundsMatch() {
        return customerFundsMatch;
    }
}
