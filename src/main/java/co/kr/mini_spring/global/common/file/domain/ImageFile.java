package co.kr.mini_spring.global.common.file.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "image_file")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImageFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String originName; // 원본 파일명 (예: cat.jpg)

    @Column(nullable = false, unique = true)
    private String storedName; // 서버 저장 파일명 (예: uuid_cat.jpg)

    @Column(nullable = false)
    private String filePath; // 저장된 상대 경로 (예: /uploads/2026/01/19/)

    @Column(nullable = false)
    private Long fileSize; // 파일 크기 (bytes)

    @Column(nullable = false, length = 10)
    private String extension; // 확장자

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public ImageFile(String originName, String storedName, String filePath, Long fileSize, String extension) {
        this.originName = originName;
        this.storedName = storedName;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.extension = extension;
    }

    /**
     * 클라이언트에게 제공할 수 있는 전체 경로 반환
     */
    public String getFullUrl() {
        return filePath + storedName;
    }
}
