package co.kr.mini_spring.global.common.file.service;

import co.kr.mini_spring.global.common.file.domain.StoredFile;
import co.kr.mini_spring.global.common.file.domain.repository.ImageFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    private ImageFileRepository imageFileRepository;

    @InjectMocks
    private FileService fileService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fileService, "uploadDir", tempDir.toString());
    }

    @Test
    void 정상_webp_파일은_업로드에_성공한다() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "image.webp",
                "image/webp",
                validWebpHeader()
        );
        when(imageFileRepository.save(any(StoredFile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> fileService.uploadImage(file));
    }

    @Test
    void 다중_업로드_실패시_원본_예외를_유지한다() {
        MockMultipartFile first = new MockMultipartFile(
                "file",
                "first.jpg",
                "image/jpeg",
                validJpegHeader()
        );
        MockMultipartFile second = new MockMultipartFile(
                "file",
                "second.jpg",
                "image/jpeg",
                validJpegHeader()
        );

        when(imageFileRepository.save(any(StoredFile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0))
                .thenThrow(new RuntimeException("db save failed"));
        doThrow(new RuntimeException("cleanup delete failed"))
                .when(imageFileRepository).delete(any(StoredFile.class));

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> fileService.uploadImages(List.of(first, second))
        );

        assertEquals("db save failed", thrown.getMessage());
    }

    private byte[] validWebpHeader() {
        return new byte[]{
                0x52, 0x49, 0x46, 0x46, // RIFF
                0x24, 0x00, 0x00, 0x00,
                0x57, 0x45, 0x42, 0x50, // WEBP
                0x56, 0x50, 0x38, 0x20
        };
    }

    private byte[] validJpegHeader() {
        byte[] bytes = new byte[16];
        bytes[0] = (byte) 0xFF;
        bytes[1] = (byte) 0xD8;
        bytes[2] = (byte) 0xFF;
        byte[] body = "jpeg".getBytes(StandardCharsets.UTF_8);
        System.arraycopy(body, 0, bytes, 3, body.length);
        return bytes;
    }
}
