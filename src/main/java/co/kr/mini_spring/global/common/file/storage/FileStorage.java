package co.kr.mini_spring.global.common.file.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorage {

    void upload(MultipartFile file, String filePath, String storedName);

    void delete(String filePath, String storedName);
}
