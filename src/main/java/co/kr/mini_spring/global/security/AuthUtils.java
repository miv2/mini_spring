package co.kr.mini_spring.global.security;

import co.kr.mini_spring.global.common.exception.BusinessException;
import co.kr.mini_spring.global.common.response.ResponseCode;

/**
 * 컨트롤러 계층에서 인증된 사용자 ID를 안전하게 추출하는 유틸리티입니다.
 */
public final class AuthUtils {

    private AuthUtils() {
    }

    public static Long currentUserId(MemberAdapter memberAdapter) {
        if (memberAdapter == null || memberAdapter.getMember() == null) {
            throw new BusinessException(ResponseCode.UNAUTHENTICATED);
        }
        return memberAdapter.getMember().getId();
    }
}
