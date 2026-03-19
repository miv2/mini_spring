package co.kr.mini_spring.global.common.file.util;

import co.kr.mini_spring.global.common.file.domain.StoredFile;

public final class FileUrlResolver {

    private FileUrlResolver() {
    }

    public static String resolve(String baseUrl, StoredFile storedFile) {
        if (storedFile == null || storedFile.getStoredName() == null || storedFile.getStoredName().isBlank()) {
            return null;
        }

        String normalizedBaseUrl = normalizeBaseUrl(baseUrl);
        String normalizedPath = normalizePath(storedFile.getFilePath());

        return normalizedBaseUrl + normalizedPath + storedFile.getStoredName();
    }

    public static String resolveOrDefault(String baseUrl, StoredFile storedFile, String defaultUrl) {
        String resolved = resolve(baseUrl, storedFile);
        return resolved != null ? resolved : defaultUrl;
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "";
        }
        return baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        return normalized.endsWith("/") ? normalized : normalized + "/";
    }
}
