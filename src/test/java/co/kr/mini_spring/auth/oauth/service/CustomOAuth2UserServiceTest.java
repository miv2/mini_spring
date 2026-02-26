package co.kr.mini_spring.auth.oauth.service;

import co.kr.mini_spring.auth.oauth.OAuthAttributes;
import co.kr.mini_spring.global.common.file.domain.StoredFile;
import co.kr.mini_spring.global.common.file.domain.repository.ImageFileRepository;
import co.kr.mini_spring.member.domain.MemberProvider;
import co.kr.mini_spring.member.domain.SocialMember;
import co.kr.mini_spring.member.domain.repository.SocialMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock
    private SocialMemberRepository socialMemberRepository;

    @Mock
    private ImageFileRepository imageFileRepository;

    @InjectMocks
    private CustomOAuth2UserService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "defaultProfileImagePath", "/uploads/default-profile.png");
    }

    @Test
    void 신규_소셜_가입시_기본_프로필_이미지_ID_12_로_고정된다() {
        OAuthAttributes attributes = OAuthAttributes.builder()
                .email("new@user.com")
                .name("New User")
                .provider(MemberProvider.GOOGLE)
                .oauthId("oauth-123")
                .attributes(Map.of("sub", "oauth-123"))
                .nameAttributeKey("sub")
                .build();

        StoredFile defaultFile = StoredFile.builder()
                .originName("default.png")
                .storedName("default-profile.png")
                .filePath("2026/02/21/")
                .fileSize(1234L)
                .extension("png")
                .contentType("image/png")
                .build();
        ReflectionTestUtils.setField(defaultFile, "id", 18L);

        when(socialMemberRepository.findByProviderAndOauthId(MemberProvider.GOOGLE, "oauth-123"))
                .thenReturn(Optional.empty());
        when(socialMemberRepository.findByEmail("new@user.com"))
                .thenReturn(Optional.empty());
        when(socialMemberRepository.findByNickname(anyString()))
                .thenReturn(Optional.empty());
        when(imageFileRepository.findById(18L))
                .thenReturn(Optional.of(defaultFile));
        when(socialMemberRepository.save(any(SocialMember.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        SocialMember saved = ReflectionTestUtils.invokeMethod(service, "findOrCreateMember", attributes);

        assertThat(saved.getProfileImage()).isNotNull();
        assertThat(saved.getProfileImage().getId()).isEqualTo(18L);
    }

    @Test
    void 기존_회원은_프로필_이미지가_변경되지_않는다() {
        OAuthAttributes attributes = OAuthAttributes.builder()
                .email("old@user.com")
                .name("Old User")
                .provider(MemberProvider.GOOGLE)
                .oauthId("oauth-999")
                .attributes(Map.of("sub", "oauth-999"))
                .nameAttributeKey("sub")
                .build();

        StoredFile original = StoredFile.builder()
                .originName("custom.png")
                .storedName("custom.png")
                .filePath("2026/02/21/")
                .fileSize(999L)
                .extension("png")
                .contentType("image/png")
                .build();
        ReflectionTestUtils.setField(original, "id", 99L);

        SocialMember existing = SocialMember.builder()
                .email("old@user.com")
                .provider(MemberProvider.GOOGLE)
                .oauthId("oauth-999")
                .name("Old User")
                .nickname("old-user")
                .profileImage(original)
                .build();

        when(socialMemberRepository.findByProviderAndOauthId(MemberProvider.GOOGLE, "oauth-999"))
                .thenReturn(Optional.of(existing));
        when(socialMemberRepository.save(any(SocialMember.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        SocialMember saved = ReflectionTestUtils.invokeMethod(service, "findOrCreateMember", attributes);

        assertThat(saved.getProfileImage()).isNotNull();
        assertThat(saved.getProfileImage().getId()).isEqualTo(99L);
    }
}
