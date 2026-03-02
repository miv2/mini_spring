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
        description = "게시글/댓글 플랫폼 REST API 명세\n\n" +
                      "### 인증 안내\n" +
                      "- 이 API는 **JWT(Header 기반)**와 **HttpOnly Cookie(accessToken/refreshToken)** 방식을 모두 지원합니다.\n" +
                      "- 소셜 로그인 성공 시 자동으로 쿠키가 설정되며, 이후 요청 시 `withCredentials: true` 설정을 권장합니다.\n\n" +
                      "### 채팅(WebSocket/STOMP)\n" +
                      "- Handshake: `/ws/chat`\n" +
                      "- Publish: `/app/chat/rooms/{roomId}/send`\n" +
                      "- Subscribe: `/topic/chat/rooms/{roomId}`\n" +
                      "- User Error Queue: `/user/queue/errors`",
        version = "v1",
        contact = @Contact(name = "mini_spring")
    ),
    security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
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
                                .servers(List.of(
                                                new Server().url("https://minispring.duckdns.org")
                                                                .description("Production Server"),
                                                new Server().url("http://localhost:8081")
                                                                .description("Local Development Server")))
                                .components(new Components()
                                                .addSecuritySchemes(BEARER_SCHEME,
                                                                new io.swagger.v3.oas.models.security.SecurityScheme()
                                                                                .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
                                                                                .scheme("bearer")
                                                                                .bearerFormat("JWT")))
                                .addSecurityItem(new io.swagger.v3.oas.models.security.SecurityRequirement()
                                                .addList(BEARER_SCHEME));
        }

            @Bean
            public GroupedOpenApi apiGroup() {
                return GroupedOpenApi.builder()
                        .group("전체 API")
                        .pathsToMatch("/api/**")
                        .build();
            }

            @Bean
            public GroupedOpenApi chatApiGroup() {
                return GroupedOpenApi.builder()
                        .group("채팅 API")
                        .pathsToMatch("/api/chat/**")
                        .build();
            }
        }
        
