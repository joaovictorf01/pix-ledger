package dev.joaovictor.pixledger.application.pix;

import java.util.Objects;
import java.util.UUID;

/**
 * @param endToEndId the Pix identifier; the same id always means the same payment
 * @param amount     in cents, always positive
 */
public record ReceivePixCommand(UUID customerAccountId, long amount, String endToEndId) {

    public ReceivePixCommand {
        Objects.requireNonNull(customerAccountId, "customerAccountId must not be null");
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (endToEndId == null || endToEndId.isBlank()) {
            throw new IllegalArgumentException("endToEndId must not be blank");
        }
    }
}
