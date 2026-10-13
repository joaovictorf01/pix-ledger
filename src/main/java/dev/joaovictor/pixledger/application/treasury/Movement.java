package dev.joaovictor.pixledger.application.treasury;

import dev.joaovictor.pixledger.domain.SystemAccount;

/** One transfer between internal accounts made by a treasury job, in cents. */
public record Movement(SystemAccount from, SystemAccount to, long amount) {
}
