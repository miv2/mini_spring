package co.kr.mini_spring.global.common.file.service;

import co.kr.mini_spring.global.common.exception.FileException;
import co.kr.mini_spring.global.common.file.domain.FileType;
import co.kr.mini_spring.global.common.file.domain.StoredFile;
import co.kr.mini_spring.global.common.file.domain.repository.ImageFileRepository;
import co.kr.mini_spring.global.common.file.storage.FileStorage;
import co.kr.mini_spring.global.common.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final ImageFileRepository imageFileRepository;
    private final FileStorage fileStorage;

    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    /**
     * 이미지를 업로드하고 메타데이터를 DB에 저장합니다.
     */
    @Transactional
    public StoredFile uploadImage(MultipartFile file) {
        // 1. 유효성 검사
        validateImage(file);

        // 2. 경로 및 파일명 생성
        String datePath = createDatePath(); // yyyy/MM/dd
        String originalName = file.getOriginalFilename();
        String extension = extractExtension(originalName);
        String storedName = UUID.randomUUID() + "." + extension;

        // 3. 파일 저장소 업로드
        fileStorage.upload(file, datePath, storedName);

        // 4. DB 메타데이터 저장 (도메인 없는 순수 날짜 경로만 저장)
        try {
            StoredFile imageFile = StoredFile.builder()
                    .originName(originalName)
                    .storedName(storedName)
                    .filePath(datePath + "/")
                    .fileSize(file.getSize())
                    .extension(extension)
                    .contentType(file.getContentType())
                    .type(FileType.IMAGE)
                    .build();

            StoredFile savedFile = imageFileRepository.save(imageFile);
            registerRollbackCleanup(savedFile.getFilePath(), savedFile.getStoredName());
            return savedFile;
        } catch (RuntimeException e) {
            // DB 저장 실패 시 이미 저장된 객체 삭제
            fileStorage.delete(datePath + "/", storedName);
            throw e;
        }
    }

    /**
     * 여러 이미지를 업로드하고 메타데이터를 DB에 저장합니다. 하나라도 실패하면 전체 롤백.
     */
    @Transactional
    public List<StoredFile> uploadImages(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new FileException(ResponseCode.INVALID_INPUT_VALUE, "업로드할 파일이 없습니다.");
        }

        List<StoredFile> savedFiles = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                savedFiles.add(uploadImage(file));
            }
        } catch (RuntimeException e) {
            // 트랜잭션이 없을 때만 즉시 보상 삭제한다.
            if (!TransactionSynchronizationManager.isSynchronizationActive()) {
                for (StoredFile savedFile : savedFiles) {
                    try {
                        deleteFile(savedFile);
                    } catch (RuntimeException cleanupException) {
                        log.warn("[다중 업로드 보상 삭제 실패] fileId={}, error={}",
                                savedFile.getId(), cleanupException.getMessage());
                        e.addSuppressed(cleanupException);
                    }
                }
            }
            throw e;
        }
        return savedFiles;
    }

    /**
     * 물리적 파일과 DB 메타데이터를 삭제합니다.
     */
    @Transactional
    public void deleteFile(StoredFile storedFile) {
        if (storedFile == null) return;

        // 2. DB 메타데이터 삭제
        imageFileRepository.delete(storedFile);
        log.info("[파일 DB 레코드 삭제 성공] fileId={}", storedFile.getId());
        registerDeleteAfterCommit(storedFile.getFilePath(), storedFile.getStoredName());
    }

    /**
     * 이미지 파일 여부 및 유효성 검사
     */
    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileException(ResponseCode.INVALID_INPUT_VALUE, "업로드할 파일이 없습니다.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileException(ResponseCode.INVALID_INPUT_VALUE, "파일 크기는 10MB를 초과할 수 없습니다.");
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

    private String createDatePath() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
    }

    private void registerRollbackCleanup(String filePath, String storedName) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    fileStorage.delete(filePath, storedName);
                }
            }
        });
    }

    private void registerDeleteAfterCommit(String filePath, String storedName) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    fileStorage.delete(filePath, storedName);
                }
            });
            return;
        }

        fileStorage.delete(filePath, storedName);
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

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "unknown";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}
