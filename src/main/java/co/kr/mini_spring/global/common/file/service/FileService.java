package co.kr.mini_spring.global.common.file.service;

import co.kr.mini_spring.global.common.exception.FileException;
import co.kr.mini_spring.global.common.file.domain.FileType;
import co.kr.mini_spring.global.common.file.domain.StoredFile;
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

    @Value("${file.public-base-url:/uploads/}")
    private String publicBaseUrl;

    private final ImageFileRepository imageFileRepository;

    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");

    /**
     * 이미지를 업로드하고 메타데이터를 DB에 저장합니다.
     */
    @Transactional
    public StoredFile uploadImage(MultipartFile file) {
        // 1. 유효성 검사
        validateImage(file);

        // 2. 경로 및 파일명 생성
        String datePath = createDatePath();
        String originalName = file.getOriginalFilename();
        String extension = extractExtension(originalName);
        String storedName = UUID.randomUUID() + "." + extension;
        String publicPath = buildPublicPath(datePath);

        // 3. 물리적 디렉토리 생성
        Path targetDir = Paths.get(uploadDir, datePath).toAbsolutePath().normalize();
        createDirectory(targetDir);

        // 4. 파일 물리 저장
        Path targetPath = targetDir.resolve(storedName);
        try {
            file.transferTo(targetPath);
            log.info("[파일 저장 성공] path={}", targetPath);
        } catch (IOException e) {
            log.error("[파일 저장 실패] error={}", e.getMessage());
            throw new FileException(ResponseCode.FILE_UPLOAD_ERROR, "파일 저장 중 오류가 발생했습니다.");
        }

        // 5. DB 메타데이터 저장
        try {
            StoredFile imageFile = StoredFile.builder()
                    .originName(originalName)
                    .storedName(storedName)
                    .filePath(publicPath)
                    .fileSize(file.getSize())
                    .extension(extension)
                    .contentType(file.getContentType())
                    .type(FileType.IMAGE)
                    .build();

            return imageFileRepository.save(imageFile);
        } catch (RuntimeException e) {
            try {
                Files.deleteIfExists(targetPath);
            } catch (IOException ex) {
                log.warn("[파일 정리 실패] path={}, error={}", targetPath, ex.getMessage());
            }
            throw e;
        }
    }

    /**
     * 여러 이미지를 업로드하고 메타데이터를 DB에 저장합니다.
     */
    @Transactional
    public List<StoredFile> uploadImages(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new FileException(ResponseCode.INVALID_INPUT_VALUE, "업로드할 파일이 없습니다.");
        }

        return files.stream()
                .map(this::uploadImage)
                .toList();
    }

    /**
     * 물리적 파일과 DB 메타데이터를 삭제합니다.
     */
    @Transactional
    public void deleteFile(StoredFile storedFile) {
        if (storedFile == null) return;

        // 1. 물리적 파일 삭제 시도
        try {
            // public URL 경로에서 날짜 경로(yyyy/MM/dd) 추출 시도
            String fullPath = storedFile.getFilePath(); // /uploads/2026/02/14/ 형태
            String relativePath = fullPath.replace(publicBaseUrl, ""); // 2026/02/14/
            
            Path targetPath = Paths.get(uploadDir, relativePath, storedFile.getStoredName());
            Files.deleteIfExists(targetPath);
            log.info("[파일 물리 삭제 성공] path={}", targetPath);
        } catch (IOException e) {
            log.warn("[파일 물리 삭제 실패] error={}", e.getMessage());
            // 물리 파일 삭제 실패 시에도 DB 데이터는 삭제 시도 (필요에 따라 정책 조정 가능)
        }

        // 2. DB 메타데이터 삭제
        imageFileRepository.delete(storedFile);
        log.info("[파일 DB 레코드 삭제 성공] fileId={}", storedFile.getId());
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

        if (!isValidImageSignature(file, extension)) {
            throw new FileException(ResponseCode.INVALID_FILE_SIGNATURE);
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
     * 공개 경로를 생성합니다. (항상 / 로 끝나도록 보정)
     */
    private String buildPublicPath(String datePath) {
        String base = publicBaseUrl == null ? "/uploads/" : publicBaseUrl.trim();
        
        // http로 시작하지 않을 때만 맨 앞에 /가 없으면 붙여줌
        if (!base.startsWith("http") && !base.startsWith("/")) {
            base = "/" + base;
        }
        
        if (!base.endsWith("/")) {
            base = base + "/";
        }
        return base + datePath + "/";
    }

    /**
     * 이미지 시그니처(매직넘버) 검사
     */
    private boolean isValidImageSignature(MultipartFile file, String extension) {
        byte[] header = new byte[12];
        try (var in = file.getInputStream()) {
            int read = in.read(header);
            if (read < 8) return false;
        } catch (IOException e) {
            return false;
        }

        String ext = extension == null ? "" : extension.toLowerCase();
        return switch (ext) {
            case "jpg", "jpeg" -> (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF;
            case "png" -> (header[0] & 0xFF) == 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47
                    && header[4] == 0x0D && header[5] == 0x0A && header[6] == 0x1A && header[7] == 0x0A;
            case "gif" -> header[0] == 0x47 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x38
                    && (header[4] == 0x37 || header[4] == 0x39) && header[5] == 0x61;
            case "webp" -> header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46
                    && header[8] == 0x57 && header[9] == 0x45 && header[10] == 0x42 && header[11] == 0x50;
            default -> false;
        };
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
