package co.kr.mini_spring.global.common.exception;

import co.kr.mini_spring.global.common.response.ResponseCode;

/**
 * 파일 처리 관련 비즈니스 예외
 */
public class FileException extends BusinessException {
    
    public FileException(ResponseCode responseCode) {
        super(responseCode);
    }

    public FileException(ResponseCode responseCode, String message) {
        super(responseCode, message);
    }
}
