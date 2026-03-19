package co.kr.mini_spring.global.common.file.config;

import co.kr.mini_spring.global.common.file.storage.FileStorage;
import co.kr.mini_spring.global.common.file.storage.R2FileStorage;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(FileProperties.class)
public class FileStorageConfig {

    @Bean
    public S3Client r2S3Client(FileProperties fileProperties) {
        FileProperties.R2 r2 = fileProperties.getR2();

        Assert.hasText(r2.getEndpoint(), "file.r2.endpoint must not be blank");
        Assert.hasText(r2.getAccessKey(), "file.r2.access-key must not be blank");
        Assert.hasText(r2.getSecretKey(), "file.r2.secret-key must not be blank");

        return S3Client.builder()
                .endpointOverride(URI.create(r2.getEndpoint()))
                .region(Region.of(r2.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(r2.getAccessKey(), r2.getSecretKey())
                ))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    @Bean
    public FileStorage r2FileStorage(S3Client r2S3Client, FileProperties fileProperties) {
        FileProperties.R2 r2 = fileProperties.getR2();
        Assert.hasText(r2.getBucket(), "file.r2.bucket must not be blank");
        return new R2FileStorage(r2S3Client, r2.getBucket(), r2.getKeyPrefix());
    }
}
