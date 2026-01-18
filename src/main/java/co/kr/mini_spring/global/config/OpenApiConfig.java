package co.kr.mini_spring.global.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.models.GroupedOpenApi;

import java.util.List;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "mini_spring API",
                description = "게시글/댓글 플랫폼 REST API 명세",
                version = "v1",
                contact = @Contact(name = "mini_spring")
        ),
        security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
)
@SecurityScheme(
        name = OpenApiConfig.BEARER_SCHEME,
        type = io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {

    static final String BEARER_SCHEME = "BearerAuth";

    @Bean
    public OpenAPI baseOpenAPI() {
        return new OpenAPI()
                .info(new io.swagger.v3.oas.models.info.Info()
                        .title("mini_spring API")
                        .version("v1")
                        .description("게시글/댓글 플랫폼 REST API")
                        .license(new License().name("MIT")))
                .servers(List.of(new Server().url("http://localhost:8081")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new io.swagger.v3.oas.models.security.SecurityScheme()
                                .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new io.swagger.v3.oas.models.security.SecurityRequirement().addList(BEARER_SCHEME));
    }

    /**
     * /api/** 경로에 대한 OpenAPI 그룹 정의 (Swagger UI에 확실히 노출되도록).
     */
    @Bean
    public GroupedOpenApi apiGroup() {
        return GroupedOpenApi.builder()
                .group("유저 API")
                .pathsToMatch("/api/**")
                .build();
    }

    /**
     * 관리자 전용 API 그룹 (/api/v1/admin/**)을 별도 탭으로 노출합니다.
     */
    @Bean
    public GroupedOpenApi adminGroup() {
        return GroupedOpenApi.builder()
                .group("관리자 API")
                .pathsToMatch("/api/v1/admin/**")
                .build();
    }
}
