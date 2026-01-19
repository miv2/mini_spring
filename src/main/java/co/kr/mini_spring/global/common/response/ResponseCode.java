package co.kr.mini_spring.global.common.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ResponseCode {
    // 2xx Success
    SUCCESS("S001", "요청이 성공적으로 처리되었습니다.", HttpStatus.OK),
    CREATED("S002", "리소스가 성공적으로 생성되었습니다.", HttpStatus.CREATED),

    // Common Client Error (4xx)
    INVALID_INPUT_VALUE("C001", "잘못된 입력 값입니다.", HttpStatus.BAD_REQUEST),
    METHOD_NOT_ALLOWED("C002", "허용되지 않은 메서드입니다.", HttpStatus.METHOD_NOT_ALLOWED),
    HANDLE_ACCESS_DENIED("C003", "접근이 거부되었습니다.", HttpStatus.FORBIDDEN),
    UNAUTHENTICATED("C004", "인증되지 않은 사용자입니다.", HttpStatus.UNAUTHORIZED),
    INTERNAL_SERVER_ERROR("C005", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    ENDPOINT_NOT_FOUND("C006", "요청한 API를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // Post
    POST_NOT_FOUND("P001", "게시글을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    NO_PERMISSION_TO_UPDATE_POST("P002", "게시글을 수정할 권한이 없습니다.", HttpStatus.FORBIDDEN),
    NO_PERMISSION_TO_DELETE_POST("P003", "게시글을 삭제할 권한이 없습니다.", HttpStatus.FORBIDDEN),

    // Comment
    COMMENT_NOT_FOUND("CM001", "댓글을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    NO_PERMISSION_TO_UPDATE_COMMENT("CM002", "댓글을 수정할 권한이 없습니다.", HttpStatus.FORBIDDEN),
    NO_PERMISSION_TO_DELETE_COMMENT("CM003", "댓글을 삭제할 권한이 없습니다.", HttpStatus.FORBIDDEN),
    COMMENT_NOT_BELONG_TO_POST("CM004", "댓글이 해당 게시글에 속하지 않습니다.", HttpStatus.BAD_REQUEST),

    // Member
    MEMBER_NOT_FOUND("M001", "회원을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    DUPLICATE_MEMBER_NAME("M002", "중복된 회원 이름입니다.", HttpStatus.CONFLICT),
    DUPLICATE_MEMBER_EMAIL("M003", "중복된 회원 이메일입니다.", HttpStatus.CONFLICT),
    INVALID_PASSWORD("M004", "잘못된 비밀번호입니다.", HttpStatus.BAD_REQUEST),

    // Auth
    INVALID_TOKEN("A001", "유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED),
    EXPIRED_TOKEN("A002", "만료된 토큰입니다.", HttpStatus.UNAUTHORIZED),
    UNSUPPORTED_TOKEN("A003", "지원되지 않는 토큰입니다.", HttpStatus.UNAUTHORIZED),
    TOKEN_NOT_FOUND("A004", "토큰을 찾을 수 없습니다.", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_NOT_FOUND("A005", "리프레시 토큰을 찾을 수 없습니다.", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_EXPIRED("A006", "리프레시 토큰이 만료되었습니다.", HttpStatus.UNAUTHORIZED),

    // File
    FILE_NOT_FOUND("F001", "파일을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INVALID_FILE_TYPE("F002", "허용되지 않은 파일 형식입니다.", HttpStatus.BAD_REQUEST),
    FILE_SIZE_EXCEEDED("F003", "파일 크기가 제한을 초과했습니다.", HttpStatus.PAYLOAD_TOO_LARGE),
    FILE_UPLOAD_ERROR("F004", "파일 업로드 중 서버 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_DELETE_ERROR("F005", "파일 삭제 중 서버 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    ;

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
