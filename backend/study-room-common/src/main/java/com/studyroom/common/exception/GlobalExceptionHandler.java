package com.studyroom.common.exception;

import com.studyroom.common.response.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<R<?>> handleBiz(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", java.nio.charset.StandardCharsets.UTF_8));
        return new ResponseEntity<>(R.fail(e.getCode(), e.getMessage()), headers, e.getCode() < 500 ? HttpStatus.valueOf(e.getCode()) : HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<?>> handle(Exception e) {
        log.error("系统异常", e);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", java.nio.charset.StandardCharsets.UTF_8));
        return new ResponseEntity<>(R.fail("服务器内部错误"), headers, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}