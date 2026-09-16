package com.learningnotes.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.learningnotes.common.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void returnsTheBusinessExceptionStatus() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleBusiness(BusinessException.notFound("笔记不存在"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().code());
    }

    @Test
    void treatsDuplicateDataAsAConflict() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleBusiness(BusinessException.conflict("分类名称已存在"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().code());
    }
}
