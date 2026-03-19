package co.kr.mini_spring.post.service;

import co.kr.mini_spring.post.domain.Hashtag;
import co.kr.mini_spring.post.domain.Post;
import co.kr.mini_spring.post.domain.PostHashtag;
import co.kr.mini_spring.post.domain.repository.HashtagQueryRepository;
import co.kr.mini_spring.post.domain.repository.HashtagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HashtagServiceTest {

    @Mock
    private HashtagRepository hashtagRepository;

    @Mock
    private HashtagQueryRepository hashtagQueryRepository;

    @InjectMocks
    private HashtagService hashtagService;

    @Test
    void 수정시_입력_해시태그를_정규화해서_비교한다() {
        Post post = Post.builder()
                .id(1L)
                .title("title")
                .content("content")
                .build();
        Hashtag java = Hashtag.builder()
                .id(10L)
                .name("java")
                .usageCount(3)
                .build();
        PostHashtag existing = PostHashtag.builder()
                .id(new PostHashtag.PostHashtagId(1L, 10L))
                .post(post)
                .hashtag(java)
                .build();
        post.getPostHashtags().add(existing);

        when(hashtagRepository.findByNameIn(anyList())).thenReturn(List.of());
        when(hashtagRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        hashtagService.updateHashtagsForPost(post, List.of("#Java", " Spring "));

        Set<String> names = post.getPostHashtags().stream()
                .map(ph -> ph.getHashtag().getName())
                .collect(Collectors.toSet());

        assertThat(names).containsExactlyInAnyOrder("java", "spring");
        assertThat(java.getUsageCount()).isEqualTo(3);
        verify(hashtagRepository).saveAll(any());
    }

    @Test
    void 수정시_해시태그가_null이면_기존_해시태그를_유지한다() {
        Post post = Post.builder()
                .id(1L)
                .title("title")
                .content("content")
                .build();
        Hashtag java = Hashtag.builder()
                .id(10L)
                .name("java")
                .usageCount(2)
                .build();
        post.getPostHashtags().add(PostHashtag.builder()
                .id(new PostHashtag.PostHashtagId(1L, 10L))
                .post(post)
                .hashtag(java)
                .build());

        hashtagService.updateHashtagsForPost(post, null);

        assertThat(post.getPostHashtags()).hasSize(1);
        assertThat(post.getPostHashtags().iterator().next().getHashtag().getName()).isEqualTo("java");
        verify(hashtagRepository, never()).findByNameIn(anyList());
        verify(hashtagRepository, never()).saveAll(any());
    }

    @Test
    void 수정시_빈_배열이면_기존_해시태그를_모두_제거한다() {
        Post post = Post.builder()
                .id(1L)
                .title("title")
                .content("content")
                .build();
        Hashtag java = Hashtag.builder()
                .id(10L)
                .name("java")
                .usageCount(2)
                .build();
        Hashtag spring = Hashtag.builder()
                .id(11L)
                .name("spring")
                .usageCount(1)
                .build();
        post.getPostHashtags().add(PostHashtag.builder()
                .id(new PostHashtag.PostHashtagId(1L, 10L))
                .post(post)
                .hashtag(java)
                .build());
        post.getPostHashtags().add(PostHashtag.builder()
                .id(new PostHashtag.PostHashtagId(1L, 11L))
                .post(post)
                .hashtag(spring)
                .build());

        hashtagService.updateHashtagsForPost(post, List.of());

        assertThat(post.getPostHashtags()).isEmpty();
        assertThat(java.getUsageCount()).isEqualTo(1);
        assertThat(spring.getUsageCount()).isEqualTo(0);
        verify(hashtagRepository, never()).findByNameIn(anyList());
    }
}
