package com.trader.coin.common.infrastructure.config.exception;

public record ErrorResponse(int code, String message, String ipAddress, String timestamp, String path) {}
