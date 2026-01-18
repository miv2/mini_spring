package co.kr.mini_spring.auth.controller;

import co.kr.mini_spring.auth.dto.response.SocialLoginLinkResponse;
import co.kr.mini_spring.global.common.exception.BusinessException;
import co.kr.mini_spring.global.common.response.ApiResponse;
import co.kr.mini_spring.global.common.response.ResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.stream.StreamSupport;

/**
 * OAuth2 소셜 로그인 관련 엔드포인트
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/social")
@Tag(name = "소셜 로그인", description = "OAuth2 로그인 링크 조회")
public class SocialLoginController {

    private static final String AUTHORIZATION_BASE_URI = "/oauth2/authorization";

    private final ClientRegistrationRepository clientRegistrationRepository;

    /**
     * 지원 중인 모든 소셜 로그인 제공자와 각 인증 페이지 진입 URL을 반환한다.
     */
    @Operation(summary = "소셜 로그인 제공자 목록", description = "Google/Kakao 등 지원 중인 소셜 로그인 제공자와 인증 URL을 반환합니다.")
    @GetMapping("/providers")
    public ApiResponse<List<SocialLoginLinkResponse>> getProviderLinks(HttpServletRequest request) {
        log.debug("[SocialLogin] 전체 제공자 조회");

        List<SocialLoginLinkResponse> responses = StreamSupport
                .stream(getClientRegistrations().spliterator(), false)
                .map(registration -> buildResponse(registration, request))
                .toList();

        return ApiResponse.success(responses);
    }

    /**
     * 특정 소셜 제공자의 인증 URL만 단건으로 요청할 때 사용.
     */
    @Operation(summary = "소셜 로그인 단일 제공자 URL", description = "provider(google|kakao)를 지정해 인증 URL을 반환합니다.")
    @GetMapping("/providers/{provider}")
    public ApiResponse<SocialLoginLinkResponse> getProviderLink(
            @PathVariable String provider,
            HttpServletRequest request
    ) {
        ClientRegistration registration = getRequiredRegistration(provider);
        log.debug("[SocialLogin] 단일 제공자 조회 provider={}", provider);
        return ApiResponse.success(buildResponse(registration, request));
    }

    /**
     * ClientRegistrationRepository가 InMemory 구성인 경우 Iterable이므로 순회하며 provider 정보를 만든다.
     */
    @SuppressWarnings("unchecked")
    private Iterable<ClientRegistration> getClientRegistrations() {
        if (clientRegistrationRepository instanceof Iterable) {
            return (Iterable<ClientRegistration>) clientRegistrationRepository;
        }
        throw new IllegalStateException("ClientRegistrationRepository does not support iteration.");
    }

    private ClientRegistration getRequiredRegistration(String provider) {
        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId(provider);
        if (registration == null) {
            throw new BusinessException(
                    ResponseCode.INVALID_INPUT_VALUE,
                    "지원하지 않는 소셜 로그인 제공자입니다: " + provider
            );
        }
        return registration;
    }

    private SocialLoginLinkResponse buildResponse(ClientRegistration registration, HttpServletRequest request) {
        String authorizationUrl = buildAuthorizationUrl(request, registration.getRegistrationId());
        return SocialLoginLinkResponse.builder()
                .provider(registration.getRegistrationId())
                .displayName(registration.getClientName())
                .authorizationUrl(authorizationUrl)
                .build();
    }

    private String buildAuthorizationUrl(HttpServletRequest request, String provider) {
        String baseUri = ServletUriComponentsBuilder.fromRequestUri(request)
                .replacePath(request.getContextPath())
                .replaceQuery(null)
                .build()
                .toUriString();

        return UriComponentsBuilder.fromUriString(baseUri)
                .path(AUTHORIZATION_BASE_URI)
                .path("/")
                .path(provider)
                .build()
                .toUriString();
    }
}
