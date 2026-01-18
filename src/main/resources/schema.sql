CREATE TABLE member
(
    id                BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '회원 고유 ID',
    email             VARCHAR(255)                                               NOT NULL UNIQUE COMMENT '이메일 (로그인 아이디)',
    password_hash     VARCHAR(255)                                               NULL COMMENT '비밀번호 해시 (소셜 유저는 NULL 가능)',
    name              VARCHAR(100)                                               NOT NULL COMMENT '실명',
    nickname          VARCHAR(100)                                               NOT NULL UNIQUE COMMENT '닉네임',
    oauth_provider    ENUM ('LOCAL', 'GOOGLE', 'KAKAO', 'NAVER') DEFAULT 'LOCAL' NOT NULL COMMENT '로그인 제공자',
    oauth_id          VARCHAR(255)                                               NULL UNIQUE COMMENT '소셜 서비스의 고유 유저 ID',
    profile_image_url VARCHAR(500)                                               NULL COMMENT '프로필 이미지 URL',
    role              ENUM ('USER', 'ADMIN')                     DEFAULT 'USER' COMMENT '회원 역할',
    status            ENUM ('ACTIVE', 'SUSPENDED', 'WITHDRAWN')  DEFAULT 'ACTIVE' COMMENT '계정 상태',
    created_at        TIMESTAMP(3)                               DEFAULT CURRENT_TIMESTAMP(3) COMMENT '가입 일시',
    updated_at        TIMESTAMP(3)                               DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '정보 수정 일시',

    -- 인덱스 추가
    INDEX idx_oauth_lookup (oauth_provider, oauth_id) COMMENT '소셜 유저 로그인 조회용',
    INDEX idx_status_created (status, created_at),
    INDEX idx_nickname (nickname)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='회원 정보';

CREATE TABLE post
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '게시글 고유 ID',
    title         VARCHAR(200) NOT NULL COMMENT '게시글 제목',
    content       LONGTEXT     NOT NULL COMMENT '게시글 본문',
    view_count    INT          DEFAULT 0 COMMENT '조회수',
    like_count    INT          DEFAULT 0 COMMENT '좋아요 수 (캐시)',
    comment_count INT          DEFAULT 0 COMMENT '댓글 수 (캐시)',
    is_published  BOOLEAN      DEFAULT TRUE COMMENT '공개 여부',
    member_id     BIGINT       NULL COMMENT '작성자 ID (탈퇴 시 NULL)', -- NULL 허용으로 수정
    created_at    TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '생성 일시',
    updated_at    TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 일시',

    CONSTRAINT fk_post_member
        FOREIGN KEY (member_id) REFERENCES member (id)
            ON DELETE SET NULL,                                   -- SET NULL로 수정

    INDEX idx_member_published (member_id, is_published) COMMENT '사용자의 게시글 관리용',
    INDEX idx_published_created (is_published, created_at DESC) COMMENT '메인 피드 조회용',
    INDEX idx_published_like (is_published, like_count DESC, created_at DESC) COMMENT '인기글 조회용',
    FULLTEXT INDEX idx_title_content (title, content) COMMENT '검색용'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='게시글';

-- hashtag, post_hashtag 테이블은 기존과 동일하므로 생략 가능 (필요 시 포함)

CREATE TABLE comment
(
    id                BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '댓글 고유 ID',
    content           TEXT   NOT NULL COMMENT '댓글 내용',
    member_id         BIGINT NULL COMMENT '작성자 ID (탈퇴 시 NULL)', -- NULL 허용으로 수정
    post_id           BIGINT NOT NULL COMMENT '게시글 ID',
    parent_comment_id BIGINT NULL COMMENT '부모 댓글 ID',
    depth             TINYINT      DEFAULT 0 COMMENT '댓글 깊이 (0: 일반, 1: 대댓글)',
    is_deleted        BOOLEAN      DEFAULT FALSE COMMENT '삭제 여부',
    created_at        TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '생성 일시',
    updated_at        TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '수정 일시',

    INDEX idx_post_created (post_id, created_at DESC) COMMENT '게시글별 최신 댓글용',
    INDEX idx_member_created (member_id, created_at DESC) COMMENT '회원별 댓글용',
    INDEX idx_parent_depth (parent_comment_id, depth) COMMENT '대댓글 계층 조회용',

    CONSTRAINT fk_comment_member
        FOREIGN KEY (member_id) REFERENCES member (id)
            ON DELETE SET NULL,                                 -- SET NULL로 수정
    CONSTRAINT fk_comment_post
        FOREIGN KEY (post_id) REFERENCES post (id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_parent
        FOREIGN KEY (parent_comment_id) REFERENCES comment (id) ON DELETE CASCADE,

    CONSTRAINT chk_depth_range CHECK (depth BETWEEN 0 AND 1),
    CONSTRAINT chk_parent_depth
        CHECK ((parent_comment_id IS NULL AND depth = 0) OR
               (parent_comment_id IS NOT NULL AND depth = 1))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='댓글';


CREATE TABLE hashtag
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '해시태그 고유 ID',
    name         VARCHAR(50)  NOT NULL UNIQUE COMMENT '해시태그 이름',
    usage_count  INT          DEFAULT 0 COMMENT '사용 횟수',
    created_at   TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '생성 일시',
    last_used_at TIMESTAMP(3) NULL COMMENT '마지막 사용 일시',

    INDEX idx_name (name) COMMENT '태그 검색용',
    INDEX idx_popularity (usage_count DESC, last_used_at DESC) COMMENT '인기 태그 조회용',
    INDEX idx_recent (last_used_at DESC) COMMENT '최근 사용 태그'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='해시태그';

CREATE TABLE post_hashtag
(
    post_id    BIGINT NOT NULL COMMENT '게시글 ID',
    hashtag_id BIGINT NOT NULL COMMENT '해시태그 ID',
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '태그 추가 일시',

    PRIMARY KEY (post_id, hashtag_id) COMMENT '복합 기본키',
    INDEX idx_hashtag_id (hashtag_id) COMMENT '태그별 게시글 조회용',

    CONSTRAINT fk_post_hashtag_post
        FOREIGN KEY (post_id) REFERENCES post (id) ON DELETE CASCADE,
    CONSTRAINT fk_post_hashtag_hashtag
        FOREIGN KEY (hashtag_id) REFERENCES hashtag (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='게시글-해시태그 매핑';

CREATE TABLE post_like
(
    member_id  BIGINT NOT NULL COMMENT '회원 ID',
    post_id    BIGINT NOT NULL COMMENT '게시글 ID',
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '좋아요 일시',

    PRIMARY KEY (member_id, post_id) COMMENT '복합 기본키',
    INDEX idx_post_id (post_id) COMMENT '게시글별 좋아요 조회용',

    CONSTRAINT fk_post_like_member
        FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE CASCADE,
    CONSTRAINT fk_post_like_post
        FOREIGN KEY (post_id) REFERENCES post (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='게시글 좋아요';


CREATE TABLE post_view
(
    member_id      BIGINT       NOT NULL COMMENT '회원 ID',
    post_id        BIGINT       NOT NULL COMMENT '게시글 ID',
    last_viewed_at TIMESTAMP(3) NOT NULL COMMENT '마지막 조회 일시',

    view_count     INT DEFAULT 1 COMMENT '해당 회원의 누적 조회 횟수',

    PRIMARY KEY (member_id, post_id) COMMENT '복합 기본키',
    INDEX idx_post_id (post_id) COMMENT '게시글 조회 기록용',
    INDEX idx_last_viewed_at (last_viewed_at) COMMENT '오래된 기록 정리용',


    CONSTRAINT fk_post_view_member
        FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE CASCADE,
    CONSTRAINT fk_post_view_post
        FOREIGN KEY (post_id) REFERENCES post (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='게시글 조회 기록';

CREATE TABLE refresh_token
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '리프레시 토큰 고유 ID',
    member_id  BIGINT       NOT NULL COMMENT '회원 ID',
    token      VARCHAR(255) NOT NULL COMMENT '리프레시 토큰 값',
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '생성 일시',
    expires_at TIMESTAMP(3) NOT NULL COMMENT '만료 일시',
    is_revoked BOOLEAN      DEFAULT FALSE COMMENT '폐기 여부',

    UNIQUE KEY uk_token (token) COMMENT '토큰 중복 방지',
    INDEX idx_member_id (member_id) COMMENT '회원별 토큰 조회용',
    INDEX idx_expires_at (expires_at) COMMENT '만료된 토큰 정리용',
    INDEX idx_member_revoked (member_id, is_revoked) COMMENT '회원의 활성 토큰 조회용',

    CONSTRAINT fk_refresh_token_member
        FOREIGN KEY (member_id) REFERENCES member (id)
            ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='리프레시 토큰 저장소';
