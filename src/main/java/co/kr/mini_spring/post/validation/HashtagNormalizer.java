package co.kr.mini_spring.post.validation;

import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class HashtagNormalizer {

    private static final String NORMALIZE_REGEX = "[^a-z0-9가-힣]";

    private HashtagNormalizer() {
    }

    public static String normalize(String hashtag) {
        if (hashtag == null || hashtag.isBlank()) {
            return "";
        }

        return hashtag.trim()
                .toLowerCase()
                .replaceAll(NORMALIZE_REGEX, "");
    }

    public static boolean isValid(String hashtag) {
        return !normalize(hashtag).isEmpty();
    }

    public static Set<String> normalizeAll(List<String> hashtags) {
        if (CollectionUtils.isEmpty(hashtags)) {
            return Set.of();
        }

        return hashtags.stream()
                .map(HashtagNormalizer::normalize)
                .filter(normalized -> !normalized.isEmpty())
                .collect(Collectors.toSet());
    }
}
