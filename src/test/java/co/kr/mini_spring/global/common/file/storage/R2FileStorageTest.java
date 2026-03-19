package co.kr.mini_spring.global.common.file.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.web.MockMultipartFile;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class R2FileStorageTest {

    @Mock
    private S3Client s3Client;

    private R2FileStorage r2FileStorage;

    @BeforeEach
    void setUp() {
        r2FileStorage = new R2FileStorage(s3Client, "mini-spring-bucket", "app");
    }

    @Test
    void 업로드시_경로와_파일명을_합친_객체키로_R2에_저장한다() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.png",
                "image/png",
                "content".getBytes(StandardCharsets.UTF_8)
        );

        r2FileStorage.upload(file, "2026/03/19/", "stored.png");

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));

        PutObjectRequest request = requestCaptor.getValue();
        assertThat(request.bucket()).isEqualTo("mini-spring-bucket");
        assertThat(request.key()).isEqualTo("app/2026/03/19/stored.png");
        assertThat(request.contentType()).isEqualTo("image/png");
    }

    @Test
    void 삭제시_동일한_객체키로_R2에서_삭제한다() {
        r2FileStorage.delete("2026/03/19/", "stored.png");

        ArgumentCaptor<DeleteObjectRequest> requestCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(requestCaptor.capture());

        DeleteObjectRequest request = requestCaptor.getValue();
        assertThat(request.bucket()).isEqualTo("mini-spring-bucket");
        assertThat(request.key()).isEqualTo("app/2026/03/19/stored.png");
    }
}
