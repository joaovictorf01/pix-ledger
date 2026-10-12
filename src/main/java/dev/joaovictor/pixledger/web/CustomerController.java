package dev.joaovictor.pixledger.web;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.joaovictor.pixledger.application.account.GetBalanceUseCase;
import dev.joaovictor.pixledger.application.account.OpenCustomerAccountUseCase;
import dev.joaovictor.pixledger.application.limit.ChangePixLimitUseCase;
import dev.joaovictor.pixledger.application.limit.GetPixLimitUseCase;
import dev.joaovictor.pixledger.application.limit.PixLimitView;
import dev.joaovictor.pixledger.domain.Account;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final OpenCustomerAccountUseCase openAccount;
    private final GetBalanceUseCase getBalance;
    private final GetPixLimitUseCase getPixLimit;
    private final ChangePixLimitUseCase changePixLimit;

    public CustomerController(OpenCustomerAccountUseCase openAccount, GetBalanceUseCase getBalance,
            GetPixLimitUseCase getPixLimit, ChangePixLimitUseCase changePixLimit) {
        this.openAccount = openAccount;
        this.getBalance = getBalance;
        this.getPixLimit = getPixLimit;
        this.changePixLimit = changePixLimit;
    }

    public record OpenAccountRequest(@NotBlank String code) {
    }

    public record AccountResponse(UUID id, String code) {
    }

    public record BalanceResponse(UUID accountId, long balance) {
    }

    public record ChangeLimitRequest(@Positive long dailyLimit) {
    }

    @PostMapping
    public ResponseEntity<AccountResponse> open(@Valid @RequestBody OpenAccountRequest request) {
        Account account = openAccount.execute(request.code());
        return ResponseEntity.created(URI.create("/customers/" + account.getId()))
                .body(new AccountResponse(account.getId(), account.getCode()));
    }

    @GetMapping("/{id}/balance")
    public BalanceResponse balance(@PathVariable UUID id) {
        return new BalanceResponse(id, getBalance.execute(id));
    }

    @GetMapping("/{id}/pix-limit")
    public PixLimitView pixLimit(@PathVariable UUID id) {
        return getPixLimit.execute(id);
    }

    /** A decrease applies now; an increase waits 24 hours (status PENDING_INCREASE). */
    @PutMapping("/{id}/pix-limit")
    public PixLimitView changePixLimit(@PathVariable UUID id, @Valid @RequestBody ChangeLimitRequest request) {
        return changePixLimit.execute(id, request.dailyLimit());
    }
}
