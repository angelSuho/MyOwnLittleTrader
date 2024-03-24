package com.trader.coin.common.infrastructure.config.exception;

import com.trader.coin.common.infrastructure.alert.discord.DiscordService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionAdvice {
    private final DiscordService discordService;

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleException(HttpServletRequest request, BaseException e) {
        String registeredTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
        String path = request.getRequestURI();
        String remoteIp = request.getRemoteAddr();

        discordService.sendDiscordErrorAlertLog(e, request);
        ErrorResponse response = new ErrorResponse(
                e.getErrorCode().getCode(), e.getMessage(),
                remoteIp, registeredTimeFormat, path);
        return ResponseEntity.status(e.getHttpStatus()).body(response);
    }
}
