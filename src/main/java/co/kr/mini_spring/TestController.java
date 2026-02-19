package co.kr.mini_spring;

import co.kr.mini_spring.global.common.file.domain.StoredFile;
import co.kr.mini_spring.global.common.file.service.FileService;
import co.kr.mini_spring.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test")
@Tag(name = "테스트", description = "개발용 테스트 API")
public class TestController {

    private final FileService fileService;

    @Operation(summary = "파일 업로드 테스트", description = "파일을 업로드하고 서버에 저장된 상세 정보를 반환합니다.")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, Object>> testUpload(@RequestParam("file") MultipartFile file) {
        log.info("[테스트 업로드 시작] 원본 파일명: {}", file.getOriginalFilename());
        
        StoredFile storedFile = fileService.uploadImage(file);
        
        Map<String, Object> result = new HashMap<>();
        result.put("id", storedFile.getId());
        result.put("originName", storedFile.getOriginName());
        result.put("storedName", storedFile.getStoredName());
        result.put("fullUrl", storedFile.getFullUrl());
        result.put("fileSize", storedFile.getFileSize());
        
        log.info("[테스트 업로드 완료] 저장된 파일 URL: {}", storedFile.getFullUrl());
        
        return ApiResponse.success(result);
    }
}