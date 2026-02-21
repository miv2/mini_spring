package co.kr.mini_spring.global.common.file.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "file")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoredFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "origin_name", nullable = false)
    private String originName;

    @Column(name = "stored_name", nullable = false, unique = true)
    private String storedName;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(nullable = false, length = 10)
    private String extension;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FileType type;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public StoredFile(String originName,
                      String storedName,
                      String filePath,
                      Long fileSize,
                      String extension,
                      String contentType,
                      FileType type) {
        this.originName = originName;
        this.storedName = storedName;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.extension = extension;
        this.contentType = contentType;
        this.type = type == null ? FileType.OTHER : type;
    }

    public String getFullUrl(String baseUrl) {
        if (storedName == null || storedName.isBlank()) return null;
        
        // baseUrl이 없으면 저장된 filePath 그대로 사용 (하위 호환)
        String base = (baseUrl == null || baseUrl.isBlank()) ? "" : baseUrl;
        if (!base.isEmpty() && !base.endsWith("/")) base += "/";
        
        // DB에 저장된 filePath가 이미 절대 경로(http)라면 그대로 반환
        if (filePath != null && filePath.startsWith("http")) {
            return getFullUrl();
        }

        String path = (filePath == null) ? "" : filePath;
        if (path.startsWith("/")) path = path.substring(1);
        if (!path.isEmpty() && !path.endsWith("/")) path += "/";

        return base + path + storedName;
    }

    public String getFullUrl() {
        if (filePath == null || filePath.isBlank()) {
            return storedName;
        }
        if (storedName == null || storedName.isBlank()) {
            return filePath;
        }
        String normalizedName = storedName.startsWith("/") ? storedName.substring(1) : storedName;
        return filePath.endsWith("/") ? filePath + normalizedName : filePath + "/" + normalizedName;
    }
}
