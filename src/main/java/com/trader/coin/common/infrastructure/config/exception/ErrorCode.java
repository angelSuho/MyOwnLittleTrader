package com.trader.coin.common.infrastructure.config.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    /* 400 */
    BAD_REQUEST(40001, HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),

    /* 401 */
    UNAUTHORIZED(40101, HttpStatus.UNAUTHORIZED, "인증에 실패하였습니다."),
    INVALID_TOKEN(40102, HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),

    /* 404 */
    NOT_FOUND(40401, HttpStatus.NOT_FOUND, "페이지를 찾을 수 없습니다."),
    NOT_MATCH(40402, HttpStatus.NOT_FOUND, "일치하는 정보가 없습니다."),

    /* 403 */
    FORBIDDEN(40301, HttpStatus.FORBIDDEN, "권한이 없습니다."),

    /* 423 */
    LOCK_ALREADY_USED(42301, HttpStatus.LOCKED, "이미 사용중인 락입니다."),

    // 500
    INTERNAL_SERVER_ERROR(50001, HttpStatus.INTERNAL_SERVER_ERROR, "서버 에러입니다.");

    private final int code;
    private final HttpStatus status;
    private final String description;

    ErrorCode(int code, HttpStatus status, String description) {
        this.status = status;
        this.code = code;
        this.description = description;
    }
}
