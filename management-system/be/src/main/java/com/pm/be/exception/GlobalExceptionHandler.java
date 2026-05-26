package com.pm.be.exception;


import com.pm.managementsystem.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Object>> handleRuntimeException(RuntimeException ex){
        ApiResponse<Object> response = new ApiResponse<>();
        response.setCode(ErrorCode.UNCATEGORIZED_ERROR.getCode());
        response.setMessage(ErrorCode.UNCATEGORIZED_ERROR.getMessage());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Object>> handleAppException(AppException ex){
        ErrorCode error = ex.getError();

        ApiResponse<Object> response = new ApiResponse<>();
        response.setCode(error.getCode());
        response.setMessage(error.getMessage());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex){
        ApiResponse<Object> response = new ApiResponse<>();

        String enumKey = ex.getFieldError().getDefaultMessage();


        ErrorCode errorCode = ErrorCode.INVALID_KEY;
        errorCode = ErrorCode.valueOf(enumKey);

        response.setCode(errorCode.getCode());
        response.setMessage(errorCode.getMessage());

        return ResponseEntity.badRequest().body(response);
    }


}
