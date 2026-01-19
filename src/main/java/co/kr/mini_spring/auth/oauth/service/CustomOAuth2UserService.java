package co.kr.mini_spring.auth.oauth.service;

import co.kr.mini_spring.member.domain.Member;
import co.kr.mini_spring.member.domain.MemberStatus;
import co.kr.mini_spring.member.domain.repository.MemberRepository;
import co.kr.mini_spring.auth.oauth.OAuthAttributes;
import co.kr.mini_spring.global.util.NicknameGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

/**
 * 소셜 로그인 성공 후 후속 조치를 담당하는 서비스입니다.
 * - 사용자 정보를 가져와 DB에 저장하거나 업데이트합니다.
 * - Spring Security가 이해할 수 있는 형태의 인증 객체(OAuth2User)를 반환합니다.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Spring Security가 소셜 로그인 성공 시 호출하는 메인 메서드입니다.
     */
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. 기본 OAuth2 서비스를 통해 소셜 서비스로부터 사용자 정보를 가져옵니다.
        OAuth2User oAuth2User = loadOAuth2User(userRequest);

        // 2. 어떤 소셜 서비스(google, kakao 등)인지, 그리고 고유 식별자 키가 무엇인지 가져옵니다.
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        // 3. 소셜 서비스별로 다른 응답 구조를 우리 시스템에 맞는 표준화된 형태로 변환합니다.
        OAuthAttributes attributes;
        try {
            attributes = OAuthAttributes.of(registrationId, userNameAttributeName, oAuth2User.getAttributes());
        } catch (co.kr.mini_spring.global.common.exception.BusinessException e) {
            throw new OAuth2AuthenticationException(new OAuth2Error("unsupported_provider"), e.getMessage());
        }

        if (!StringUtils.hasText(attributes.getEmail())) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_token"), "이메일을 제공하지 않는 소셜 계정입니다.");
        }

        // 4. DB에 사용자가 이미 있는지 확인하고, 없으면 새로 저장(회원가입), 있으면 정보를 업데이트합니다.
        Member member = findOrCreateMember(attributes);

        // 5. Spring Security가 이 사용자를 인증된 사용자로 처리할 수 있도록 최종 인증 객체를 생성하여 반환합니다.
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(member.getRole().getKey())),
                attributes.getAttributes(),
                attributes.getNameAttributeKey()
        );
    }

    /**
     * 기본 OAuth2UserService를 사용하여 소셜 서비스로부터 사용자 정보를 로드합니다.
     * 이 메서드는 테스트 용이성을 위해 분리되었습니다.
     * @param userRequest 소셜 서비스에서 제공하는 사용자 요청 정보
     * @return 로드된 OAuth2User 객체
     */
    private OAuth2User loadOAuth2User(OAuth2UserRequest userRequest) {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        return delegate.loadUser(userRequest);
    }

    /**
     * 이메일을 기준으로 사용자를 찾거나, 없는 경우 새로 생성하여 저장합니다.
     * @param attributes 표준화된 소셜 로그인 사용자 정보
     * @return 저장되거나 업데이트된 Member 엔티티
     */
    private Member findOrCreateMember(OAuthAttributes attributes) {
        // 1. 이메일로 기존 사용자가 있는지 찾아봅니다.
        Optional<Member> memberOptional = Optional.empty();
        if (attributes.getOauthId() != null && attributes.getProvider() != null) {
            memberOptional = memberRepository.findByOauthProviderAndOauthId(attributes.getProvider(), attributes.getOauthId());
        }
        if (memberOptional.isEmpty() && StringUtils.hasText(attributes.getEmail())) {
            memberOptional = memberRepository.findByEmail(attributes.getEmail());
        }

        Member member = memberOptional.orElseGet(() -> {
            String encodedPassword = passwordEncoder.encode(UUID.randomUUID().toString());
            String nickname = NicknameGenerator.generateUniqueNickname(n -> memberRepository.findByNickname(n).isPresent());
            return attributes.toEntity(encodedPassword, nickname);
        });

        if (member.getStatus() == MemberStatus.WITHDRAWN) {
            throw new OAuth2AuthenticationException(new OAuth2Error("access_denied"), "탈퇴한 계정입니다.");
        }
        if (member.getStatus() == MemberStatus.SUSPENDED) {
            throw new OAuth2AuthenticationException(new OAuth2Error("access_denied"), "정지된 계정입니다.");
        }

        if (StringUtils.hasText(attributes.getName())) {
            member.update(attributes.getName(), member.getNickname());
        }

        // 이메일로 찾았으나 소셜 정보가 비어 있다면 연결해 둔다.
        if (member.getOauthId() == null && attributes.getOauthId() != null && attributes.getProvider() != null) {
            member.changeProvider(attributes.getProvider(), attributes.getOauthId());
        }

        return memberRepository.save(member);
    }

}
