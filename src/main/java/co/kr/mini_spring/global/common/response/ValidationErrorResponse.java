package co.kr.mini_spring.global.common.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Bean Validation 실패 시 필드 단위 에러 정보를 내려주기 위한 응답 DTO.
 */
@Schema(description = "검증 에러 공통 응답 객체")
public record ValidationErrorResponse(
        @Schema(description = "필드 단위 에러 목록") List<FieldError> errors
) {

    /**
     * 단일 필드 검증 에러.
     */
    @Schema(description = "단일 필드 검증 에러")
    public record FieldError(
            @Schema(description = "검증에 실패한 필드명", example = "email") String field,
            @Schema(description = "에러 메시지", example = "올바른 이메일 형식이 아닙니다.") String message
    ) {
    }
}
