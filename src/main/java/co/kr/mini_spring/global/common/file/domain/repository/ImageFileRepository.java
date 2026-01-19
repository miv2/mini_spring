package co.kr.mini_spring.global.common.file.domain.repository;

import co.kr.mini_spring.global.common.file.domain.ImageFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageFileRepository extends JpaRepository<ImageFile, Long> {
}
