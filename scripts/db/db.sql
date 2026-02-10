CREATE TABLE `comment` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '댓글 고유 ID',
  `content` tinytext NOT NULL,
  `post_id` bigint(20) NOT NULL COMMENT '게시글 ID',
  `parent_comment_id` bigint(20) DEFAULT NULL COMMENT '부모 댓글 ID',
  `depth` int(11) NOT NULL,
  `is_deleted` tinyint(1) DEFAULT 0 COMMENT '삭제 여부',
  `created_at` timestamp(3) NULL DEFAULT current_timestamp(3) COMMENT '생성 일시',
  `updated_at` timestamp(3) NULL DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3) COMMENT '수정 일시',
  `deleted_at` datetime(6) DEFAULT NULL,
  `author_id` bigint(20) DEFAULT NULL COMMENT '작성자 ID',
  PRIMARY KEY (`id`),
  KEY `idx_post_created` (`post_id`,`created_at` DESC) COMMENT '게시글별 최신 댓글용',
  KEY `idx_member_created` (`created_at` DESC) COMMENT '회원별 댓글용',
  KEY `idx_parent_depth` (`parent_comment_id`,`depth`) COMMENT '대댓글 계층 조회용',
  KEY `idx_deleted_at` (`deleted_at`),
  CONSTRAINT `fk_comment_parent` FOREIGN KEY (`parent_comment_id`) REFERENCES `comment` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_comment_post` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`) ON DELETE CASCADE,
  CONSTRAINT `chk_depth_range` CHECK (`depth` between 0 and 1),
  CONSTRAINT `chk_parent_depth` CHECK (`parent_comment_id` is null and `depth` = 0 or `parent_comment_id` is not null and `depth` = 1)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='댓글';
CREATE TABLE `file` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '파일 고유 ID',
  `origin_name` varchar(255) NOT NULL COMMENT '원본 파일명',
  `stored_name` varchar(255) NOT NULL COMMENT '저장 파일명',
  `file_path` varchar(255) NOT NULL COMMENT '저장 경로',
  `file_size` bigint(20) NOT NULL COMMENT '파일 크기',
  `extension` varchar(10) NOT NULL COMMENT '확장자',
  `content_type` varchar(100) DEFAULT NULL COMMENT 'MIME 타입',
  `type` enum('IMAGE','OTHER') NOT NULL,
  `created_at` datetime(6) NOT NULL COMMENT '생성 일시',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stored_name` (`stored_name`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='파일';
CREATE TABLE `hashtag` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '해시태그 고유 ID',
  `name` varchar(50) NOT NULL COMMENT '해시태그 이름',
  `usage_count` int(11) DEFAULT 0 COMMENT '사용 횟수',
  `created_at` timestamp(3) NULL DEFAULT current_timestamp(3) COMMENT '생성 일시',
  `last_used_at` timestamp(3) NULL DEFAULT NULL COMMENT '마지막 사용 일시',
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`),
  KEY `idx_popularity` (`usage_count` DESC,`last_used_at` DESC) COMMENT '인기 태그 조회용',
  KEY `idx_recent` (`last_used_at` DESC) COMMENT '최근 사용 태그'
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='해시태그';
CREATE TABLE `post` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '게시글 고유 ID',
  `title` varchar(200) NOT NULL COMMENT '게시글 제목',
  `content` tinytext NOT NULL,
  `view_count` int(11) DEFAULT 0 COMMENT '조회수',
  `like_count` int(11) DEFAULT 0 COMMENT '좋아요 수 (캐시)',
  `comment_count` int(11) DEFAULT 0 COMMENT '댓글 수 (캐시)',
  `is_published` tinyint(1) DEFAULT 1 COMMENT '공개 여부',
  `created_at` timestamp(3) NULL DEFAULT current_timestamp(3) COMMENT '생성 일시',
  `updated_at` timestamp(3) NULL DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3) COMMENT '수정 일시',
  `deleted_at` datetime(6) DEFAULT NULL,
  `author_id` bigint(20) DEFAULT NULL COMMENT '작성자 ID',
  PRIMARY KEY (`id`),
  KEY `idx_member_published` (`is_published`) COMMENT '사용자의 게시글 관리용',
  KEY `idx_published_created` (`is_published`,`created_at` DESC) COMMENT '메인 피드 조회용',
  KEY `idx_published_like` (`is_published`,`like_count` DESC,`created_at` DESC) COMMENT '인기글 조회용',
  KEY `idx_created_at` (`created_at`),
  KEY `idx_published` (`is_published`),
  KEY `idx_deleted_at` (`deleted_at`),
  FULLTEXT KEY `idx_title_content` (`title`,`content`) COMMENT '검색용'
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='게시글';
CREATE TABLE `post_hashtag` (
  `post_id` bigint(20) NOT NULL COMMENT '게시글 ID',
  `hashtag_id` bigint(20) NOT NULL COMMENT '해시태그 ID',
  `created_at` timestamp(3) NULL DEFAULT current_timestamp(3) COMMENT '태그 추가 일시',
  PRIMARY KEY (`post_id`,`hashtag_id`) COMMENT '복합 기본키',
  KEY `idx_hashtag_id` (`hashtag_id`) COMMENT '태그별 게시글 조회용',
  CONSTRAINT `fk_post_hashtag_hashtag` FOREIGN KEY (`hashtag_id`) REFERENCES `hashtag` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_post_hashtag_post` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='게시글-해시태그 매핑';
CREATE TABLE `post_like` (
  `author_id` bigint(20) NOT NULL,
  `post_id` bigint(20) NOT NULL COMMENT '게시글 ID',
  `created_at` timestamp(3) NULL DEFAULT current_timestamp(3) COMMENT '좋아요 일시',
  PRIMARY KEY (`author_id`,`post_id`) COMMENT '복합 기본키',
  KEY `idx_post_id` (`post_id`) COMMENT '게시글별 좋아요 조회용',
  CONSTRAINT `fk_post_like_post` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='게시글 좋아요';
CREATE TABLE `post_view` (
  `last_viewed_at` datetime(6) NOT NULL,
  `view_count` int(11) DEFAULT 1,
  `post_id` bigint(20) NOT NULL,
  KEY `idx_last_viewed_at` (`last_viewed_at`),
  KEY `idx_post_id` (`post_id`),
  KEY `idx_post_view_post_id` (`post_id`),
  CONSTRAINT `FKaymjwcor4fcsxhgqgus7302a0` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
CREATE TABLE `refresh_token` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '리프레시 토큰 고유 ID',
  `author_id` bigint(20) NOT NULL,
  `token` varchar(255) NOT NULL COMMENT '리프레시 토큰 값',
  `created_at` timestamp(3) NULL DEFAULT current_timestamp(3) COMMENT '생성 일시',
  `expires_at` timestamp(3) NOT NULL COMMENT '만료 일시',
  `is_revoked` tinyint(1) DEFAULT 0 COMMENT '폐기 여부',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_token` (`token`) COMMENT '토큰 중복 방지',
  KEY `idx_member_id` (`author_id`) COMMENT '회원별 토큰 조회용',
  KEY `idx_expires_at` (`expires_at`) COMMENT '만료된 토큰 정리용',
  KEY `idx_member_revoked` (`author_id`,`is_revoked`) COMMENT '회원의 활성 토큰 조회용'
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='리프레시 토큰 저장소';
CREATE TABLE `social_member` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '회원 ID',
  `email` varchar(255) NOT NULL COMMENT '이메일 (로그인 ID)',
  `provider` varchar(20) NOT NULL COMMENT '소셜 로그인 제공자 (GOOGLE, KAKAO, NAVER)',
  `oauth_id` varchar(255) NOT NULL COMMENT '소셜 제공자 측의 고유 식별자',
  `name` varchar(100) NOT NULL COMMENT '사용자 이름',
  `nickname` varchar(100) NOT NULL COMMENT '시스템 내 닉네임',
  `profile_image_id` bigint(20) DEFAULT NULL COMMENT '프로필 이미지 ID (file 테이블 FK)',
  `role` varchar(10) NOT NULL DEFAULT 'USER' COMMENT '회원 권한 (USER, ADMIN)',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '계정 상태 (ACTIVE, SUSPENDED, WITHDRAWN)',
  `last_login_at` timestamp(6) NULL DEFAULT NULL COMMENT '마지막 로그인 일시',
  `created_at` timestamp(6) NOT NULL DEFAULT current_timestamp(6) COMMENT '생성 일시',
  `updated_at` timestamp(6) NOT NULL DEFAULT current_timestamp(6) ON UPDATE current_timestamp(6) COMMENT '수정 일시',
  `deleted_at` timestamp(6) NULL DEFAULT NULL COMMENT '탈퇴 일시 (Soft Delete)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`),
  UNIQUE KEY `nickname` (`nickname`),
  UNIQUE KEY `uk_provider_oauth_id` (`provider`,`oauth_id`),
  KEY `idx_email` (`email`),
  KEY `idx_nickname` (`nickname`),
  KEY `idx_status_created` (`status`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='소셜 회원 테이블';
