package co.kr.mini_spring.global.common.file.domain.repository;

import co.kr.mini_spring.global.common.file.domain.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageFileRepository extends JpaRepository<StoredFile, Long> {
}
