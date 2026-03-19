package co.kr.mini_spring.global.common.file.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "file")
public class FileProperties {

    private String publicBaseUrl;
    private String defaultProfileImage;
    private R2 r2 = new R2();

    @Getter
    @Setter
    public static class R2 {
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String bucket;
        private String region = "auto";
        private String keyPrefix = "";
    }
}
