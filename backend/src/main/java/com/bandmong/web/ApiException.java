package com.bandmong.web;

import org.springframework.http.HttpStatus;

/** 화면에 그대로 보여줄 메시지를 담은 예외. */
public class ApiException extends RuntimeException {
    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
