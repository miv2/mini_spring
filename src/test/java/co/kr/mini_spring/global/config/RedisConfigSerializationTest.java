package co.kr.mini_spring.global.config;

import co.kr.mini_spring.global.common.response.PageResponse;
import co.kr.mini_spring.member.domain.MemberProvider;
import co.kr.mini_spring.member.domain.MemberRole;
import co.kr.mini_spring.member.domain.MemberStatus;
import co.kr.mini_spring.member.domain.SocialMember;
import co.kr.mini_spring.post.domain.Post;
import co.kr.mini_spring.post.dto.response.PostSummaryResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

class RedisConfigSerializationTest {

    @Test
    void redisSerializer_supportsLocalDateTime() {
        Post post = Post.builder()
                .id(1L)
                .title("title")
                .authorId(3L)
                .createdAt(LocalDateTime.now())
                .build();

        SocialMember author = SocialMember.builder()
                .id(3L)
                .email("test@example.com")
                .provider(MemberProvider.GOOGLE)
                .oauthId("oauth")
                .name("tester")
                .nickname("nick")
                .role(MemberRole.USER)
                .status(MemberStatus.ACTIVE)
                .build();

        PageResponse<PostSummaryResponse> response = new PageResponse<>(
                List.of(new PostSummaryResponse(post, author)),
                0, 10, 1, 1, false
        );

        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        RedisConfig config = new RedisConfig();
        GenericJackson2JsonRedisSerializer serializer =
                (GenericJackson2JsonRedisSerializer) config.redisTemplate(connectionFactory).getValueSerializer();

        assertThatCode(() -> serializer.serialize(response))
                .doesNotThrowAnyException();
    }
}
