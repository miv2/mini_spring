package co.kr.mini_spring.member.exception;

import co.kr.mini_spring.global.common.exception.BusinessException;
import co.kr.mini_spring.global.common.response.ResponseCode;

/**
 * 이미 존재하는 이메일로 회원가입 시 발생
 */
public class EmailAlreadyExistsException extends BusinessException {
    public EmailAlreadyExistsException() {
        super(ResponseCode.DUPLICATE_MEMBER_EMAIL);
    }

    public EmailAlreadyExistsException(String message) {
        super(ResponseCode.DUPLICATE_MEMBER_EMAIL, message);
    }
}
