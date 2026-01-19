package co.kr.mini_spring.post.service;

import co.kr.mini_spring.post.domain.Hashtag;
import co.kr.mini_spring.post.domain.Post;
import co.kr.mini_spring.post.domain.PostHashtag;
import co.kr.mini_spring.post.domain.repository.HashtagRepository;
import co.kr.mini_spring.post.domain.repository.HashtagQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HashtagService {

    private final HashtagRepository hashtagRepository;
    private final HashtagQueryRepository hashtagQueryRepository;

    @Transactional
    public List<Hashtag> findOrCreateHashtags(List<String> hashtagNames) {
        if (CollectionUtils.isEmpty(hashtagNames)) return List.of();

        Set<String> normalizedNames = hashtagNames.stream()
                .filter(name -> name != null && !name.isBlank())
                .map(name -> name.trim().toLowerCase().replaceAll("[^a-z0-9가-힣]", ""))
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toSet());

        if (normalizedNames.isEmpty()) return List.of();

        List<Hashtag> existingHashtags = hashtagRepository.findByNameIn(new ArrayList<>(normalizedNames));
        List<String> existingNames = existingHashtags.stream().map(Hashtag::getName).toList();
        
        if (!existingHashtags.isEmpty()) {
            hashtagQueryRepository.bulkIncreaseUsageCount(existingNames);
        }

        List<Hashtag> newHashtags = normalizedNames.stream()
                .filter(name -> !existingNames.contains(name))
                .map(name -> Hashtag.builder()
                        .name(name)
                        .usageCount(1)
                        .lastUsedAt(LocalDateTime.now())
                        .build())
                .collect(Collectors.toList());

        if (!newHashtags.isEmpty()) {
            hashtagRepository.saveAll(newHashtags);
        }

        List<Hashtag> allHashtags = new ArrayList<>(existingHashtags);
        allHashtags.addAll(newHashtags);
        return allHashtags;
    }

    @Transactional
    public void attachHashtagsToPost(Post post, List<String> hashtagNames) {
        if (CollectionUtils.isEmpty(hashtagNames)) return;
        List<Hashtag> hashtags = findOrCreateHashtags(hashtagNames);
        hashtags.forEach(hashtag -> {
            PostHashtag postHashtag = PostHashtag.builder()
                    .id(new PostHashtag.PostHashtagId(post.getId(), hashtag.getId()))
                    .post(post)
                    .hashtag(hashtag)
                    .build();
            post.getPostHashtags().add(postHashtag);
        });
    }

    @Transactional
    public void updateHashtagsForPost(Post post, List<String> newHashtagNames) {
        Set<String> currentHashtagNames = post.getPostHashtags().stream()
                .map(ph -> ph.getHashtag().getName())
                .collect(Collectors.toSet());
        List<String> incomingNames = newHashtagNames == null ? new ArrayList<>() : newHashtagNames;

        post.getPostHashtags().removeIf(ph -> {
            boolean isRemoved = !incomingNames.contains(ph.getHashtag().getName());
            if (isRemoved) ph.getHashtag().decreaseUsage();
            return isRemoved;
        });

        List<String> namesToAdd = incomingNames.stream()
                .filter(name -> !currentHashtagNames.contains(name))
                .toList();
        if (!CollectionUtils.isEmpty(namesToAdd)) attachHashtagsToPost(post, namesToAdd);
    }
}
