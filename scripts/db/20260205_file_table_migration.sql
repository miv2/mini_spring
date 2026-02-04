-- 1) 신규 file 테이블 생성
CREATE TABLE IF NOT EXISTS `file` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '파일 고유 ID',
    `origin_name` VARCHAR(255) NOT NULL COMMENT '원본 파일명',
    `stored_name` VARCHAR(255) NOT NULL COMMENT '저장 파일명',
    `file_path` VARCHAR(255) NOT NULL COMMENT '저장 경로',
    `file_size` BIGINT NOT NULL COMMENT '파일 크기',
    `extension` VARCHAR(10) NOT NULL COMMENT '확장자',
    `content_type` VARCHAR(100) NULL COMMENT 'MIME 타입',
    `type` ENUM ('IMAGE', 'OTHER') NOT NULL DEFAULT 'OTHER' COMMENT '파일 타입',
    `created_at` DATETIME(6) NOT NULL COMMENT '생성 일시',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stored_name` (`stored_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='파일';

-- 2) image_file -> file 데이터 이관
INSERT IGNORE INTO `file` (id, origin_name, stored_name, file_path, file_size, extension, content_type, type, created_at)
SELECT
    id,
    origin_name,
    stored_name,
    file_path,
    file_size,
    extension,
    NULL AS content_type,
    'IMAGE' AS type,
    created_at
FROM image_file
;

-- 3) member FK 재설정 (image_file -> file)
ALTER TABLE `member` DROP FOREIGN KEY `fk_member_profile_image`;
ALTER TABLE `member`
    ADD CONSTRAINT `fk_member_profile_image`
    FOREIGN KEY (`profile_image_id`) REFERENCES `file` (`id`);

-- 필요 시 image_file 테이블은 검증 후 삭제
-- DROP TABLE image_file;
