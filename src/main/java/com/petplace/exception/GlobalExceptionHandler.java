package com.petplace.exception;

import com.petplace.dto.response.ApiResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException; // 💡 추가
import org.springframework.web.multipart.MaxUploadSizeExceededException; // 💡 추가
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 1. 비즈니스 로직 예외 처리
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn("[Business Exception] : {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
    }

    /**
     * 2. @Valid 검증 실패 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        log.warn("[Validation Failure] : {}", errorMessage);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(errorMessage));
    }

    /**
     * 3. 잘못된 HTTP Method 호출 시 발생 (예: POST가 필요한데 GET으로 보냄)
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    protected ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("[Method Not Supported] : {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error("지원하지 않는 HTTP 메서드입니다."));
    }

    /**
     * 4. 일반적인 잘못된 인자 예외 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("[Illegal Argument] : {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
    }

    /**
     * 💡 [신규 추가] 5. 파일 업로드 용량 초과 예외 처리 (S3 이미지 연동 필수 방어선)
     * 최대 제한 용량을 넘는 대용량 파일 첨부 시 500 에러 분출을 막아줍니다.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("[Max Upload Size Exceeded] : {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("업로드 가능한 파일 크기를 초과했습니다."));
    }

    /**
     * 💡 [신규 추가] 6. 쿼리 파라미터 / 경로 변수 타입 불일치 예외 처리
     * 예: Long 자리에 String 데이터가 인입되었을 때 대응 ("abc" -> Long)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("[Type Mismatch] : {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("요청 파라미터의 타입이 올바르지 않습니다."));
    }

    /**
     * 💡 [신규 추가] S3 파일 업로드 등 IO 작업 중 발생한 예외 처리
     * FileService에서 완화한 IOException을 전역에서 일괄 관리합니다.
     */
    @ExceptionHandler(IOException.class)
    public ResponseEntity<ApiResponse<Void>> handleIOException(IOException e) {
        // 서버 로그에는 구체적인 스택트레이스를 남겨 디버깅을 쉽게 합니다.
        log.error("[IO Exception / S3 Storage Error] : ", e);

        // 유저에게는 500에러 대용으로 인지 가능한 친절한 메시지를 내려줍니다.
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("이미지 파일 처리 및 저장 중 오류가 발생했습니다."));
    }

    /**
     * 7. 기타 예외 처리 (500 에러)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        // 보안을 위해 상세한 에러 메시지는 서버 로그에만 남기고, 사용자에게는 추상적인 메시지를 전달합니다.
        log.error("[Internal Server Error] : ", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요."));
    }

    /**
     * 💡 [신규 추가] 만료된 JWT 토큰 예외 처리 (JwtFilter에서 위임됨)
     */
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ApiResponse<Void>> handleExpiredJwtException(ExpiredJwtException e) {
        // 💡 디버깅 및 분석을 위해 로그에 예외 객체 e를 통째로 전달하여 스택 트레이스 확보
        log.warn("[JWT Expired Summary] : ", e);
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED) // 401 에러
                .body(ApiResponse.error("토큰이 만료되었습니다. 다시 로그인해주세요."));
    }

    /**
     * 💡 [신규 추가] 유효하지 않은 위변조 JWT 토큰 예외 처리 (JwtFilter에서 위임됨)
     */
    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiResponse<Void>> handleJwtException(JwtException e) {
        // 💡 악성 요청 추적 및 분석이 가능하도록 스택 트레이스 완벽 보존
        log.error("[JWT Invalid Summary] : ", e);
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED) // 401 에러
                .body(ApiResponse.error("유효하지 않은 인증 토큰입니다."));
    }
}