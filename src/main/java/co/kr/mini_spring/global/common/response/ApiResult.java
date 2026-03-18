package co.kr.mini_spring.global.common.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "공통 응답 객체")
public class ApiResult<T> {
    @Schema(description = "성공 여부")
    private final boolean success;

    @Schema(description = "응답 코드 (SUCCESS, FAIL 등)")
    private final String code;

    @Schema(description = "응답 메시지")
    private final String message;

    @Schema(description = "데이터 결과")
    private final T data;

    @Schema(description = "응답 시간")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private final LocalDateTime timestamp;

    // 성공 응답 (데이터 포함)
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(
                true,
                ResponseCode.SUCCESS.getCode(),
                ResponseCode.SUCCESS.getMessage(),
                data,
                LocalDateTime.now()
        );
    }

    // 성공 응답 (데이터 없음)
    public static <T> ApiResult<T> success() {
        return new ApiResult<>(
                true,
                ResponseCode.SUCCESS.getCode(),
                ResponseCode.SUCCESS.getMessage(),
                null,
                LocalDateTime.now()
        );
    }

    // 성공 응답 (커스텀 메시지)
    public static <T> ApiResult<T> success(String message, T data) {
        return new ApiResult<>(
                true,
                ResponseCode.SUCCESS.getCode(),
                message,
                data,
                LocalDateTime.now()
        );
    }

    // 성공 응답 (커스텀 코드/메시지) - CREATED 등 성공 코드 지정
    public static <T> ApiResult<T> success(ResponseCode responseCode, T data) {
        return new ApiResult<>(
                true,
                responseCode.getCode(),
                responseCode.getMessage(),
                data,
                LocalDateTime.now()
        );
    }

    // 실패 응답
    public static <T> ApiResult<T> fail(ResponseCode responseCode) {
        return fail(responseCode, responseCode.getMessage(), null);
    }

    // 실패 응답 (커스텀 메시지)
    public static <T> ApiResult<T> fail(ResponseCode responseCode, String message) {
        return fail(responseCode, message, null);
    }

    // 실패 응답 (커스텀 메시지 + 데이터)
    public static <T> ApiResult<T> fail(ResponseCode responseCode, String message, T data) {
        return new ApiResult<>(
                false,
                responseCode.getCode(),
                message,
                data,
                LocalDateTime.now()
        );
    }

}
