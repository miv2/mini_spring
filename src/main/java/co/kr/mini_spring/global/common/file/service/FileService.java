package co.kr.mini_spring.global.common.file.service;

import co.kr.mini_spring.global.common.exception.FileException;
import co.kr.mini_spring.global.common.file.domain.ImageFile;
import co.kr.mini_spring.global.common.file.domain.repository.ImageFileRepository;
import co.kr.mini_spring.global.common.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    private final ImageFileRepository imageFileRepository;

    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");

    /**
     * 이미지를 업로드하고 메타데이터를 DB에 저장합니다.
     */
    @Transactional
    public ImageFile uploadImage(MultipartFile file) {
        // 1. 유효성 검사
        validateImage(file);

        // 2. 경로 및 파일명 생성
        String datePath = createDatePath();
        String originalName = file.getOriginalFilename();
        String extension = extractExtension(originalName);
        String storedName = UUID.randomUUID().toString() + "." + extension;

        // 3. 물리적 디렉토리 생성
        Path targetDir = Paths.get(uploadDir, datePath);
        createDirectory(targetDir);

        // 4. 파일 물리 저장
        Path targetPath = targetDir.resolve(storedName);
        try {
            file.transferTo(targetPath.toFile());
            log.info("[파일 저장 성공] path={}", targetPath);
        } catch (IOException e) {
            log.error("[파일 저장 실패] error={}", e.getMessage());
            throw new FileException(ResponseCode.FILE_UPLOAD_ERROR, "파일 저장 중 오류가 발생했습니다.");
        }

        // 5. DB 메타데이터 저장
        ImageFile imageFile = ImageFile.builder()
                .originName(originalName)
                .storedName(storedName)
                .filePath("/uploads/" + datePath + "/")
                .fileSize(file.getSize())
                .extension(extension)
                .build();

        return imageFileRepository.save(imageFile);
    }

    /**
     * 이미지 파일 여부 및 유효성 검사
     */
    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileException(ResponseCode.INVALID_INPUT_VALUE, "업로드할 파일이 없습니다.");
        }

        String extension = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new FileException(ResponseCode.INVALID_FILE_TYPE);
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new FileException(ResponseCode.INVALID_FILE_TYPE, "이미지 파일만 업로드 가능합니다.");
        }
    }

    /**
     * 날짜별 디렉토리 경로 생성 (yyyy/MM/dd)
     */
    private String createDatePath() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
    }

    /**
     * 디렉토리가 없으면 생성
     */
    private void createDirectory(Path path) {
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            throw new FileException(ResponseCode.FILE_UPLOAD_ERROR, "디렉토리 생성에 실패했습니다.");
        }
    }

    /**
     * 파일 확장자 추출
     */
    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "unknown";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}