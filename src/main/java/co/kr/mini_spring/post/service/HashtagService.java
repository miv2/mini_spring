package co.kr.mini_spring.post.service;

import co.kr.mini_spring.post.domain.Hashtag;
import co.kr.mini_spring.post.domain.Post;
import co.kr.mini_spring.post.domain.PostHashtag;
import co.kr.mini_spring.post.domain.repository.HashtagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 해시태그 관련 비즈니스 로직을 담당하는 서비스
 * - 해시태그 생성 및 조회
 * - 게시글-해시태그 연결 관리
 * - 해시태그 사용 횟수 관리
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HashtagService {

    private final HashtagRepository hashtagRepository;

    /**
     * 해시태그 이름 목록으로 해시태그를 조회하거나 생성합니다.
     * 기존 해시태그는 사용 횟수를 증가시키고, 없는 해시태그는 새로 생성합니다.
     *
     * @param hashtagNames 해시태그 이름 목록
     * @return 조회되거나 생성된 해시태그 목록
     */
    @Transactional
    public List<Hashtag> findOrCreateHashtags(List<String> hashtagNames) {
        if (CollectionUtils.isEmpty(hashtagNames)) {
            return List.of();
        }

        // 1. 기존 해시태그 조회
        List<Hashtag> existingHashtags = hashtagRepository.findByNameIn(hashtagNames);
        List<String> existingHashtagNames = existingHashtags.stream()
                .map(Hashtag::getName)
                .toList();

        // 2. 기존 해시태그 사용 횟수 증가
        existingHashtags.forEach(Hashtag::increaseUsage);

        // 3. 새로운 해시태그 생성
        List<Hashtag> newHashtags = hashtagNames.stream()
                .filter(name -> !existingHashtagNames.contains(name))
                .map(name -> Hashtag.builder()
                        .name(name)
                        .usageCount(1)
                        .lastUsedAt(LocalDateTime.now())
                        .build())
                .collect(Collectors.toList());

        if (!newHashtags.isEmpty()) {
            hashtagRepository.saveAll(newHashtags);
        }

        // 4. 전체 해시태그 목록 반환
        List<Hashtag> allHashtags = new ArrayList<>(existingHashtags);
        allHashtags.addAll(newHashtags);
        return allHashtags;
    }

    /**
     * 게시글에 해시태그를 연결합니다.
     *
     * @param post 게시글 엔티티
     * @param hashtagNames 연결할 해시태그 이름 목록
     */
    @Transactional
    public void attachHashtagsToPost(Post post, List<String> hashtagNames) {
        if (CollectionUtils.isEmpty(hashtagNames)) {
            return;
        }

        List<Hashtag> hashtags = findOrCreateHashtags(hashtagNames);
        hashtags.forEach(hashtag -> {
            PostHashtag.PostHashtagId postHashtagId = new PostHashtag.PostHashtagId(post.getId(), hashtag.getId());
            PostHashtag postHashtag = PostHashtag.builder()
                    .id(postHashtagId)
                    .post(post)
                    .hashtag(hashtag)
                    .build();
            post.getPostHashtags().add(postHashtag);
        });
    }

    /**
     * 게시글의 해시태그를 업데이트합니다.
     * 기존 해시태그와 새로운 해시태그 목록을 비교하여 추가/제거를 수행합니다.
     *
     * @param post 게시글 엔티티
     * @param newHashtagNames 새로운 해시태그 이름 목록
     */
    @Transactional
    public void updateHashtagsForPost(Post post, List<String> newHashtagNames) {
        // 1. 현재 게시글에 연결된 해시태그 이름 목록
        Set<String> existingHashtagNames = post.getPostHashtags().stream()
                .map(postHashtag -> postHashtag.getHashtag().getName())
                .collect(Collectors.toSet());

        List<String> newNames = newHashtagNames == null ? new ArrayList<>() : newHashtagNames;

        // 2. 제거할 해시태그 처리 (사용 횟수 감소)
        post.getPostHashtags().removeIf(postHashtag -> {
            boolean shouldRemove = !newNames.contains(postHashtag.getHashtag().getName());
            if (shouldRemove) {
                postHashtag.getHashtag().decreaseUsage();
            }
            return shouldRemove;
        });

        // 3. 추가할 해시태그 처리
        List<String> namesToAdd = newNames.stream()
                .filter(name -> !existingHashtagNames.contains(name))
                .collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(namesToAdd)) {
            List<Hashtag> hashtagsToAdd = findOrCreateHashtags(namesToAdd);
            hashtagsToAdd.forEach(hashtag -> {
                PostHashtag.PostHashtagId postHashtagId = new PostHashtag.PostHashtagId(post.getId(), hashtag.getId());
                PostHashtag postHashtag = PostHashtag.builder()
                        .id(postHashtagId)
                        .post(post)
                        .hashtag(hashtag)
                        .build();
                post.getPostHashtags().add(postHashtag);
            });
        }
    }
}
