package dev.joaovictor.pixledger.web;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.joaovictor.pixledger.application.pix.ReceivePixCommand;
import dev.joaovictor.pixledger.application.pix.ReceivePixUseCase;
import dev.joaovictor.pixledger.application.pix.SendPixCommand;
import dev.joaovictor.pixledger.application.pix.SendPixUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/pix")
public class PixController {

    private final SendPixUseCase sendPix;
    private final ReceivePixUseCase receivePix;

    public PixController(SendPixUseCase sendPix, ReceivePixUseCase receivePix) {
        this.sendPix = sendPix;
        this.receivePix = receivePix;
    }

    /**
     * @param amount     in cents
     * @param endToEndId identifies the payment; sending it again is a retry, not a new Pix
     */
    public record PixRequest(@NotNull UUID customerAccountId, @Positive long amount, @NotBlank String endToEndId) {
    }

    public record PixResponse(UUID transactionId, String endToEndId) {
    }

    @PostMapping("/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    public PixResponse send(@Valid @RequestBody PixRequest request) {
        UUID transactionId = sendPix.execute(
                new SendPixCommand(request.customerAccountId(), request.amount(), request.endToEndId()));
        return new PixResponse(transactionId, request.endToEndId());
    }

    /** Stands in for the event queue that delivers Pix from other institutions. */
    @PostMapping("/incoming")
    @ResponseStatus(HttpStatus.CREATED)
    public PixResponse receive(@Valid @RequestBody PixRequest request) {
        UUID transactionId = receivePix.execute(
                new ReceivePixCommand(request.customerAccountId(), request.amount(), request.endToEndId()));
        return new PixResponse(transactionId, request.endToEndId());
    }
}
