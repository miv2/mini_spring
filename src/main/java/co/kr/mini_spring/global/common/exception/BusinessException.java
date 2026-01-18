package co.kr.mini_spring.global.common.exception;

import co.kr.mini_spring.global.common.response.ResponseCode;
import lombok.Getter;

/**
 * 비즈니스 로직 처리 중 발생하는 예외의 공통 부모 클래스
 */
@Getter
public class BusinessException extends RuntimeException {
    private final ResponseCode responseCode;

    public BusinessException(ResponseCode responseCode) {
        super(responseCode.getMessage());
        this.responseCode = responseCode;
    }

    public BusinessException(ResponseCode responseCode, String message) {
        super(message);
        this.responseCode = responseCode;
    }
}
