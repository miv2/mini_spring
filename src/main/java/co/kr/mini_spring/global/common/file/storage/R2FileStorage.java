package co.kr.mini_spring.global.common.file.storage;

import co.kr.mini_spring.global.common.exception.FileException;
import co.kr.mini_spring.global.common.response.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;

@Slf4j
public class R2FileStorage implements FileStorage {

    private final S3Client s3Client;
    private final String bucket;
    private final String keyPrefix;

    public R2FileStorage(S3Client s3Client, String bucket, String keyPrefix) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.keyPrefix = normalizePrefix(keyPrefix);
    }

    @Override
    public void upload(MultipartFile file, String filePath, String storedName) {
        String objectKey = buildObjectKey(filePath, storedName);

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            log.info("[R2 파일 저장 성공] bucket={}, key={}", bucket, objectKey);
        } catch (IOException | SdkException e) {
            log.error("[R2 파일 저장 실패] bucket={}, key={}, error={}", bucket, objectKey, e.getMessage());
            throw new FileException(ResponseCode.FILE_UPLOAD_ERROR, "파일 저장 중 오류가 발생했습니다.");
        }
    }

    @Override
    public void delete(String filePath, String storedName) {
        String objectKey = buildObjectKey(filePath, storedName);

        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build();

            s3Client.deleteObject(request);
            log.info("[R2 파일 삭제 성공] bucket={}, key={}", bucket, objectKey);
        } catch (SdkException e) {
            log.warn("[R2 파일 삭제 실패] bucket={}, key={}, error={}", bucket, objectKey, e.getMessage());
        }
    }

    private String buildObjectKey(String filePath, String storedName) {
        String normalizedPath = normalizePath(filePath);
        return keyPrefix + normalizedPath + storedName;
    }

    private String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "";
        }
        String normalized = prefix.startsWith("/") ? prefix.substring(1) : prefix;
        return normalized.endsWith("/") ? normalized : normalized + "/";
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        return normalized.endsWith("/") ? normalized : normalized + "/";
    }
}
