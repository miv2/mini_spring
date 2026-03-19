package co.kr.mini_spring.global.common.file.service;

import co.kr.mini_spring.global.common.file.domain.StoredFile;
import co.kr.mini_spring.global.common.file.domain.repository.ImageFileRepository;
import co.kr.mini_spring.global.common.file.storage.FileStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.nio.charset.StandardCharsets;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private ImageFileRepository imageFileRepository;

    @Mock
    private FileStorage fileStorage;

    @InjectMocks
    private FileService fileService;

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
        verify(fileStorage).upload(any(), anyString(), anyString());
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
        verify(fileStorage, times(1)).delete(anyString(), anyString());
    }

    @Test
    void DB_저장_실패시_업로드된_파일을_스토리지에서_삭제한다() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "broken.jpg",
                "image/jpeg",
                validJpegHeader()
        );

        when(imageFileRepository.save(any(StoredFile.class)))
                .thenThrow(new RuntimeException("db save failed"));

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> fileService.uploadImage(file)
        );

        assertEquals("db save failed", thrown.getMessage());
        verify(fileStorage).upload(any(), anyString(), anyString());
        verify(fileStorage).delete(anyString(), anyString());
    }

    @Test
    void 유효성_검사_실패시_스토리지_업로드를_시도하지_않는다() {
        MockMultipartFile invalid = new MockMultipartFile(
                "file",
                "broken.txt",
                "text/plain",
                "invalid".getBytes(StandardCharsets.UTF_8)
        );

        assertThrows(RuntimeException.class, () -> fileService.uploadImage(invalid));

        verify(fileStorage, never()).upload(any(), anyString(), anyString());
    }

    @Test
    void 외부_트랜잭션이_롤백되면_업로드한_R2_파일을_보상_삭제한다() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "image.jpg",
                "image/jpeg",
                validJpegHeader()
        );
        when(imageFileRepository.save(any(StoredFile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionSynchronizationManager.initSynchronization();
        try {
            StoredFile saved = fileService.uploadImage(file);

            verify(fileStorage, never()).delete(anyString(), anyString());
            List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
            assertEquals(1, synchronizations.size());

            synchronizations.get(0).afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

            verify(fileStorage).delete(saved.getFilePath(), saved.getStoredName());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void 삭제는_커밋_후에만_R2_파일을_삭제한다() {
        StoredFile storedFile = StoredFile.builder()
                .originName("origin.jpg")
                .storedName("stored.jpg")
                .filePath("2026/03/19/")
                .fileSize(10L)
                .extension("jpg")
                .contentType("image/jpeg")
                .build();

        TransactionSynchronizationManager.initSynchronization();
        try {
            fileService.deleteFile(storedFile);

            verify(imageFileRepository).delete(storedFile);
            verify(fileStorage, never()).delete(anyString(), anyString());

            List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
            assertEquals(1, synchronizations.size());

            synchronizations.get(0).afterCommit();

            verify(fileStorage).delete("2026/03/19/", "stored.jpg");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
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
