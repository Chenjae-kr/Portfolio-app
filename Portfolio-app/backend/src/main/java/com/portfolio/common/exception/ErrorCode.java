package com.portfolio.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Internal server error"),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "Invalid input"),
    VALIDATION_ERROR(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", "Validation failed"),

    // Auth
    AUTH_REQUIRED(HttpStatus.UNAUTHORIZED, "AUTH_REQUIRED", "Authentication required"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Invalid or expired token"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "Access denied"),

    // Portfolio
    PORTFOLIO_NOT_FOUND(HttpStatus.NOT_FOUND, "PORTFOLIO_NOT_FOUND", "Portfolio not found"),
    INVALID_TARGET_WEIGHTS(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_TARGET_WEIGHTS", "Target weights must sum to 1.0"),

    // Transaction
    TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "TRANSACTION_NOT_FOUND", "Transaction not found"),
    INVALID_TRANSACTION_LEGS(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_TRANSACTION_LEGS", "Transaction legs validation failed"),
    TRANSACTION_ALREADY_VOID(HttpStatus.CONFLICT, "TRANSACTION_ALREADY_VOID", "Transaction is already void"),

    // Instrument
    INSTRUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "INSTRUMENT_NOT_FOUND", "Instrument not found"),

    // Pricing
    PRICE_DATA_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "PRICE_DATA_UNAVAILABLE", "Price data is unavailable"),
    FX_DATA_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "FX_DATA_UNAVAILABLE", "FX rate data is unavailable"),

    // Backtest
    BACKTEST_NOT_FOUND(HttpStatus.NOT_FOUND, "BACKTEST_NOT_FOUND", "Backtest run not found"),
    BACKTEST_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "BACKTEST_FAILED", "Backtest execution failed");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
