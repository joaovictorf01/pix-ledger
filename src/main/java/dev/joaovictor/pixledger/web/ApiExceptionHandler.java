package dev.joaovictor.pixledger.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import dev.joaovictor.pixledger.domain.exception.AccountNotFoundException;
import dev.joaovictor.pixledger.domain.exception.InsufficientFundsException;
import dev.joaovictor.pixledger.domain.exception.InvalidTransactionException;
import dev.joaovictor.pixledger.domain.exception.PixLimitExceededException;

/** Domain errors as RFC 9457 problem details, with a readable message. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    ProblemDetail notFound(AccountNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Account not found", e);
    }

    @ExceptionHandler(InsufficientFundsException.class)
    ProblemDetail insufficientFunds(InsufficientFundsException e) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "Insufficient funds", e);
    }

    @ExceptionHandler(PixLimitExceededException.class)
    ProblemDetail limitExceeded(PixLimitExceededException e) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "Pix limit exceeded", e);
    }

    @ExceptionHandler({ InvalidTransactionException.class, IllegalArgumentException.class })
    ProblemDetail invalid(RuntimeException e) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", e);
    }

    private static ProblemDetail problem(HttpStatus status, String title, RuntimeException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, e.getMessage());
        problem.setTitle(title);
        return problem;
    }
}
