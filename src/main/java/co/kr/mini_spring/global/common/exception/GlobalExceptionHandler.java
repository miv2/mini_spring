package co.kr.mini_spring.global.common.exception;

import co.kr.mini_spring.global.common.response.ApiResponse;
import co.kr.mini_spring.global.common.response.ResponseCode;
import co.kr.mini_spring.global.common.response.ValidationErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    /**
     * 비즈니스 로직 예외 처리
     */
    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn("[BusinessException] code={}, message={}", e.getResponseCode().getCode(), e.getMessage());
        ApiResponse<Void> body = ApiResponse.fail(e.getResponseCode(), e.getMessage());
        return new ResponseEntity<>(body, e.getResponseCode().getHttpStatus());
    }

    /**
     * Bean Validation 예외 처리 (@Valid, @Validated)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ApiResponse<ValidationErrorResponse>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        List<ValidationErrorResponse.FieldError> errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> new ValidationErrorResponse.FieldError(
                        fe.getField(),
                        fe.getDefaultMessage() == null ? "검증 오류" : fe.getDefaultMessage()
                ))
                .toList();

        log.warn("[Validation] fields={}", errors.stream().map(ValidationErrorResponse.FieldError::field).collect(Collectors.joining(", ")));

        ValidationErrorResponse data = new ValidationErrorResponse(errors);
        ApiResponse<ValidationErrorResponse> body = ApiResponse.fail(ResponseCode.INVALID_INPUT_VALUE, ResponseCode.INVALID_INPUT_VALUE.getMessage(), data);
        return new ResponseEntity<>(body, ResponseCode.INVALID_INPUT_VALUE.getHttpStatus());
    }

    /**
     * 파라미터 검증 실패(@Validated + @RequestParam/@PathVariable 등) 처리.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<ApiResponse<ValidationErrorResponse>> handleConstraintViolationException(ConstraintViolationException e) {
        List<ValidationErrorResponse.FieldError> errors = e.getConstraintViolations()
                .stream()
                .map(v -> new ValidationErrorResponse.FieldError(
                        v.getPropertyPath() == null ? "param" : v.getPropertyPath().toString(),
                        v.getMessage() == null ? "검증 오류" : v.getMessage()
                ))
                .toList();

        log.warn("[Validation] constraintViolations={}", errors.size());

        ValidationErrorResponse data = new ValidationErrorResponse(errors);
        ApiResponse<ValidationErrorResponse> body = ApiResponse.fail(ResponseCode.INVALID_INPUT_VALUE, ResponseCode.INVALID_INPUT_VALUE.getMessage(), data);
        return new ResponseEntity<>(body, ResponseCode.INVALID_INPUT_VALUE.getHttpStatus());
    }

    /**
     * JSON 파싱 실패(깨진 JSON 등) 처리.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    protected ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("[BadRequest] messageNotReadable={}", e.getMessage());
        ApiResponse<Void> body = ApiResponse.fail(ResponseCode.INVALID_INPUT_VALUE, "요청 본문(JSON)이 올바르지 않습니다.");
        return new ResponseEntity<>(body, ResponseCode.INVALID_INPUT_VALUE.getHttpStatus());
    }

    /**
     * 요청 파라미터 타입 미스매치 처리 (예: page=abc).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        String name = e.getName();
        log.warn("[BadRequest] typeMismatch param={}, value={}", name, e.getValue());
        ApiResponse<Void> body = ApiResponse.fail(ResponseCode.INVALID_INPUT_VALUE, "요청 파라미터 타입이 올바르지 않습니다: " + name);
        return new ResponseEntity<>(body, ResponseCode.INVALID_INPUT_VALUE.getHttpStatus());
    }

    /**
     * 필수 요청 파라미터 누락 처리.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    protected ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.warn("[BadRequest] missingParam name={}", e.getParameterName());
        ApiResponse<Void> body = ApiResponse.fail(ResponseCode.INVALID_INPUT_VALUE, "필수 요청 파라미터가 누락되었습니다: " + e.getParameterName());
        return new ResponseEntity<>(body, ResponseCode.INVALID_INPUT_VALUE.getHttpStatus());
    }

    /**
     * 지원하지 않는 HTTP Method 처리.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    protected ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("[MethodNotAllowed] method={}, supported={}", e.getMethod(), e.getSupportedHttpMethods());
        ApiResponse<Void> body = ApiResponse.fail(ResponseCode.METHOD_NOT_ALLOWED);
        return new ResponseEntity<>(body, ResponseCode.METHOD_NOT_ALLOWED.getHttpStatus());
    }

    /**
     * 파일 업로드 용량 초과 처리.
     */
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    protected ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(org.springframework.web.multipart.MaxUploadSizeExceededException e) {
        log.warn("[BadRequest] maxUploadSizeExceeded={}", e.getMessage());
        ApiResponse<Void> body = ApiResponse.fail(ResponseCode.FILE_SIZE_EXCEEDED);
        return new ResponseEntity<>(body, ResponseCode.FILE_SIZE_EXCEEDED.getHttpStatus());
    }

    /**
     * 그 외 처리하지 않은 모든 예외
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("[Uncaught] {}: {}", e.getClass().getSimpleName(), e.getMessage(), e);
        ApiResponse<Void> body = ApiResponse.fail(ResponseCode.INTERNAL_SERVER_ERROR);
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
