package com.smartstock.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldReturnHttp401ForUnauthorizedBusinessException() {
        ResponseEntity<Result<Void>> response = handler.handleBusinessException(
                new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录"));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(ErrorCode.UNAUTHORIZED, response.getBody().getCode());
    }

    @Test
    void shouldReturnHttp404ForNotFoundBusinessException() {
        ResponseEntity<Result<Void>> response = handler.handleBusinessException(
                new BusinessException(ErrorCode.STOCK_NOT_FOUND, "股票不存在"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(ErrorCode.STOCK_NOT_FOUND, response.getBody().getCode());
    }

    @Test
    void shouldReturnHttp400ForGenericBusinessException() {
        ResponseEntity<Result<Void>> response = handler.handleBusinessException(
                new BusinessException(ErrorCode.INSUFFICIENT_FUNDS, "资金不足"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(ErrorCode.INSUFFICIENT_FUNDS, response.getBody().getCode());
    }
}
