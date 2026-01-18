package co.kr.mini_spring.global.common.exception;

import co.kr.mini_spring.global.common.response.ResponseCode;

/**
 * 비밀번호와 비밀번호 확인이 불일치할 때 발생
 */
public class PasswordMismatchException extends BusinessException {
    public PasswordMismatchException() {
        super(ResponseCode.INVALID_PASSWORD);
    }

    public PasswordMismatchException(String message) {
        super(ResponseCode.INVALID_PASSWORD, message);
    }
}
