package co.kr.mini_spring.global.common.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private final boolean success;
    private final String code;
    private final String message;
    private final T data;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private final LocalDateTime timestamp;

    // 성공 응답 (데이터 포함)
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
                true,
                ResponseCode.SUCCESS.getCode(),
                ResponseCode.SUCCESS.getMessage(),
                data,
                LocalDateTime.now()
        );
    }

    // 성공 응답 (데이터 없음)
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(
                true,
                ResponseCode.SUCCESS.getCode(),
                ResponseCode.SUCCESS.getMessage(),
                null,
                LocalDateTime.now()
        );
    }

    // 성공 응답 (커스텀 메시지)
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(
                true,
                ResponseCode.SUCCESS.getCode(),
                message,
                data,
                LocalDateTime.now()
        );
    }

    // 성공 응답 (커스텀 코드/메시지) - CREATED 등 성공 코드 지정
    public static <T> ApiResponse<T> success(ResponseCode responseCode, T data) {
        return new ApiResponse<>(
                true,
                responseCode.getCode(),
                responseCode.getMessage(),
                data,
                LocalDateTime.now()
        );
    }

    // 실패 응답
    public static <T> ApiResponse<T> fail(ResponseCode responseCode) {
        return fail(responseCode, responseCode.getMessage(), null);
    }

    // 실패 응답 (커스텀 메시지)
    public static <T> ApiResponse<T> fail(ResponseCode responseCode, String message) {
        return fail(responseCode, message, null);
    }

    // 실패 응답 (커스텀 메시지 + 데이터)
    public static <T> ApiResponse<T> fail(ResponseCode responseCode, String message, T data) {
        return new ApiResponse<>(
                false,
                responseCode.getCode(),
                message,
                data,
                LocalDateTime.now()
        );
    }

}
