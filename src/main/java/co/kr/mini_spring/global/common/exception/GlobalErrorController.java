package co.kr.mini_spring.global.common.exception;

import co.kr.mini_spring.global.common.response.ApiResponse;
import co.kr.mini_spring.global.common.response.ResponseCode;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Spring Boot 기본 /error 응답을 프로젝트의 표준 응답(ApiResponse) 형태로 통일합니다.
 * - 404/405 등 전역 예외 핸들러로 잡히지 않는 케이스도 일관된 포맷으로 내려줍니다.
 */
@RestController
public class GlobalErrorController implements ErrorController {

    /**
     * /error 요청을 처리해 ApiResponse로 변환합니다.
     */
    @RequestMapping("/error")
    public ResponseEntity<ApiResponse<Void>> handleError(HttpServletRequest request) {
        int statusCode = resolveStatusCode(request);
        ResponseCode responseCode = mapToResponseCode(statusCode);

        ApiResponse<Void> body = ApiResponse.fail(responseCode);
        return ResponseEntity.status(responseCode.getHttpStatus()).body(body);
    }

    /**
     * request attribute에서 HTTP status code를 추출합니다.
     */
    private int resolveStatusCode(HttpServletRequest request) {
        Object value = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (value == null) {
            return 500;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ignored) {
            return 500;
        }
    }

    /**
     * HTTP status code를 프로젝트 ResponseCode로 매핑합니다.
     */
    private ResponseCode mapToResponseCode(int statusCode) {
        return switch (statusCode) {
            case 400 -> ResponseCode.INVALID_INPUT_VALUE;
            case 401 -> ResponseCode.UNAUTHENTICATED;
            case 403 -> ResponseCode.HANDLE_ACCESS_DENIED;
            case 404 -> ResponseCode.ENDPOINT_NOT_FOUND;
            case 405 -> ResponseCode.METHOD_NOT_ALLOWED;
            default -> ResponseCode.INTERNAL_SERVER_ERROR;
        };
    }
}

