package co.kr.mini_spring.global.common.file.controller;

import co.kr.mini_spring.global.common.file.domain.StoredFile;
import co.kr.mini_spring.global.common.file.service.FileService;
import co.kr.mini_spring.global.common.file.util.FileUrlResolver;
import co.kr.mini_spring.global.common.response.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/files")
@Tag(name = "파일", description = "파일 업로드 API")
public class FileController {

    private final FileService fileService;

    @Value("${file.public-base-url}")
    private String publicBaseUrl;

    @Operation(summary = "이미지 다중 업로드", description = "여러 장의 이미지를 업로드하고 URL 목록을 반환합니다.")
    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResult<List<String>> uploadImages(
            @RequestParam("files") List<MultipartFile> files
    ) {
        List<StoredFile> saved = fileService.uploadImages(files);
        List<String> urls = saved.stream()
                .map(file -> FileUrlResolver.resolve(publicBaseUrl, file))
                .toList();
        return ApiResult.success(urls);
    }
}
