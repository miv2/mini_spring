package co.kr.mini_spring.post.cache;

import java.util.List;
import java.util.StringJoiner;

public final class PostCacheKey {
    private PostCacheKey() {}

    public static String list(int page, int size, String sort, String keyword, List<String> hashtags, Long authorId) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase();
        String normalizedHashtags = normalizeHashtags(hashtags);
        String normalizedAuthor = authorId == null ? "" : authorId.toString();
        return "posts:list:" + page + ":" + size + ":" + sort + ":" + normalizedKeyword + ":" + normalizedHashtags + ":" + normalizedAuthor;
    }

    private static String normalizeHashtags(List<String> hashtags) {
        if (hashtags == null || hashtags.isEmpty()) return "";
        return hashtags.stream()
                .map(String::trim)
                .map(String::toLowerCase)
                .sorted()
                .reduce(new StringJoiner(","), StringJoiner::add, StringJoiner::merge)
                .toString();
    }
}
