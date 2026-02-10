package co.kr.mini_spring.global.common.file.domain.repository;

import co.kr.mini_spring.global.common.file.domain.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ImageFileRepository extends JpaRepository<StoredFile, Long> {
    Optional<StoredFile> findByStoredName(String storedName);
}
