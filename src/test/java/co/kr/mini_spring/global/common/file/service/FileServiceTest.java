package co.kr.mini_spring.global.common.file.service;

import co.kr.mini_spring.global.common.exception.FileException;
import co.kr.mini_spring.global.common.file.domain.StoredFile;
import co.kr.mini_spring.global.common.file.domain.repository.ImageFileRepository;
import co.kr.mini_spring.global.common.response.ResponseCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FileServiceTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("이미지 업로드 성공 - 파일 저장 및 메타데이터 저장")
    void uploadImage_success() throws IOException {
        ImageFileRepository repository = Mockito.mock(ImageFileRepository.class);
        FileService service = new FileService(repository);
        ReflectionTestUtils.setField(service, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(service, "publicBaseUrl", "/uploads/");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cat.png",
                "image/png",
                pngBytes()
        );

        when(repository.save(any(StoredFile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StoredFile saved = service.uploadImage(file);

        ArgumentCaptor<StoredFile> captor = ArgumentCaptor.forClass(StoredFile.class);
        verify(repository, times(1)).save(captor.capture());

        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        Path expectedDir = tempDir.resolve(datePath);

        assertThat(Files.exists(expectedDir)).isTrue();
        List<Path> files = Files.list(expectedDir).toList();
        assertThat(files).hasSize(1);

        assertThat(saved.getFilePath()).isEqualTo("/uploads/" + datePath + "/");
        assertThat(saved.getStoredName()).endsWith(".png");
        assertThat(saved.getOriginName()).isEqualTo("cat.png");
    }

    @Test
    @DisplayName("DB 저장 실패 시 업로드 파일은 정리되어야 한다")
    void uploadImage_dbFail_cleanupFile() throws IOException {
        ImageFileRepository repository = Mockito.mock(ImageFileRepository.class);
        FileService service = new FileService(repository);
        ReflectionTestUtils.setField(service, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(service, "publicBaseUrl", "/uploads/");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cat.png",
                "image/png",
                pngBytes()
        );

        when(repository.save(any(StoredFile.class))).thenThrow(new RuntimeException("DB down"));

        assertThatThrownBy(() -> service.uploadImage(file))
                .isInstanceOf(RuntimeException.class);

        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        Path expectedDir = tempDir.resolve(datePath);

        if (Files.exists(expectedDir)) {
            assertThat(Files.list(expectedDir).toList()).isEmpty();
        }
    }

    @Test
    @DisplayName("이미지 시그니처가 맞지 않으면 예외가 발생한다")
    void uploadImage_invalidSignature() {
        ImageFileRepository repository = Mockito.mock(ImageFileRepository.class);
        FileService service = new FileService(repository);
        ReflectionTestUtils.setField(service, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(service, "publicBaseUrl", "/uploads/");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cat.png",
                "image/png",
                "not-a-real-image".getBytes()
        );

        assertThatThrownBy(() -> service.uploadImage(file))
                .isInstanceOf(FileException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.INVALID_FILE_SIGNATURE);
    }

    private byte[] pngBytes() {
        return new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x00
        };
    }
}
