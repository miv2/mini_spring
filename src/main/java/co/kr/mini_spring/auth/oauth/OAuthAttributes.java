package co.kr.mini_spring.auth.oauth;

import co.kr.mini_spring.global.common.exception.BusinessException;
import co.kr.mini_spring.global.common.response.ResponseCode;
import co.kr.mini_spring.member.domain.Member;
import co.kr.mini_spring.member.domain.MemberProvider;
import co.kr.mini_spring.member.domain.MemberRole;
import co.kr.mini_spring.member.domain.MemberStatus;
import lombok.Builder;
import lombok.Getter;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * OAuth 공급자별 사용자 속성을 내부 모델로 변환하기 위한 DTO.
 * - provider마다 다른 필드를 공통 필드(name, email, attributes, nameAttributeKey)로 정규화한다.
 * - 이후 서비스 레이어에서 Member 엔티티 생성에 사용한다.
 */
@Getter
public class OAuthAttributes {

    private final Map<String, Object> attributes;
    private final String nameAttributeKey;
    private final String name;
    private final String email;
    private final MemberProvider provider;
    private final String oauthId;

    @Builder
    public OAuthAttributes(Map<String, Object> attributes, String nameAttributeKey, String name, String email, MemberProvider provider, String oauthId) {
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
        this.name = name;
        this.email = email;
        this.provider = provider;
        this.oauthId = oauthId;
    }

    /**
     * 공급자(registrationId)에 따라 적절한 변환 로직을 선택한다.
     */
    public static OAuthAttributes of(String registrationId, String userNameAttributeName, Map<String, Object> attributes) {
        if (!StringUtils.hasText(registrationId)) {
            throw new BusinessException(ResponseCode.INVALID_INPUT_VALUE, "registrationId가 비어있습니다.");
        }

        return switch (registrationId.toLowerCase()) {
            case "kakao" -> ofKakao("id", attributes);
            case "google" -> ofGoogle(userNameAttributeName, attributes);
            default -> throw new BusinessException(ResponseCode.INVALID_INPUT_VALUE, "지원하지 않는 OAuth 제공자입니다: " + registrationId);
        };
    }

    // Kakao 응답 → 내부 모델 변환
    private static OAuthAttributes ofKakao(String userNameAttributeName, Map<String, Object> attributes) {
        Map<String, Object> kakaoAccount = cast(attributes.get("kakao_account"));
        Map<String, Object> properties = cast(attributes.get("properties"));

        return OAuthAttributes.builder()
                .name(getString(properties, "nickname"))
                .email(getString(kakaoAccount, "email"))
                .provider(MemberProvider.KAKAO)
                .oauthId(String.valueOf(attributes.get("id")))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    // Google 응답 → 내부 모델 변환(기본)
    private static OAuthAttributes ofGoogle(String userNameAttributeName, Map<String, Object> attributes) {
        String oauthId = getString(attributes, userNameAttributeName);
        if (!StringUtils.hasText(oauthId)) {
            oauthId = getString(attributes, "sub");
        }
        if (!StringUtils.hasText(oauthId)) {
            oauthId = getString(attributes, "id");
        }

        return OAuthAttributes.builder()
                .name(getString(attributes, "name"))
                .email(getString(attributes, "email"))
                .provider(MemberProvider.GOOGLE)
                .oauthId(oauthId)
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    /**
     * 정규화된 속성 + 주입된 비밀번호/닉네임으로 Member 엔티티를 생성한다.
     */
    public Member toEntity(String encodedPassword, String nickname) {
        String displayName = (StringUtils.hasText(name)) ? name : email.split("@")[0];

        return Member.builder()
                .name(displayName)
                .nickname(nickname)
                .email(email)
                .passwordHash(encodedPassword)
                .role(MemberRole.USER)
                .status(MemberStatus.ACTIVE)
                .oauthProvider(provider == null ? MemberProvider.LOCAL : provider)
                .oauthId(oauthId)
                .build();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> cast(Object value) {
        return (value instanceof Map<?, ?> map) ? (Map<String, Object>) map : Map.of();
    }

    private static String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? null : value.toString();
    }
}