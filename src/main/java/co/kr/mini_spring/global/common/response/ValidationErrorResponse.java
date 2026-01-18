package co.kr.mini_spring.global.common.response;

import java.util.List;

/**
 * Bean Validation 실패 시 필드 단위 에러 정보를 내려주기 위한 응답 DTO.
 */
public record ValidationErrorResponse(List<FieldError> errors) {

    /**
     * 단일 필드 검증 에러.
     */
    public record FieldError(String field, String message) {
    }
}

