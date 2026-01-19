package co.kr.mini_spring.global.util;

import java.util.Random;
import java.util.function.Predicate;

/**
 * 랜덤 닉네임 생성 유틸.
 * 전달된 중복 검사(predicate)가 true를 반환하지 않을 때까지 재시도한다.
 */
public final class NicknameGenerator {

    private NicknameGenerator() {
    }

    private static final String[] ADJECTIVES = {
            "행복한", "즐거운", "빛나는", "용감한", "친절한", "똑똑한", "신비한", "고요한", "따뜻한", "시원한"
    };
    private static final String[] NOUNS = {
            "사자", "호랑이", "코끼리", "기린", "고래", "돌고래", "참새", "독수리", "강아지", "고양이"
    };
    private static final Random RANDOM = new Random();

    public static String generateUniqueNickname(Predicate<String> isDuplicate) {
        while (true) {
            String nickname = buildNickname();
            if (!isDuplicate.test(nickname)) {
                return nickname;
            }
        }
    }

    private static String buildNickname() {
        String adjective = ADJECTIVES[RANDOM.nextInt(ADJECTIVES.length)];
        String noun = NOUNS[RANDOM.nextInt(NOUNS.length)];
        int number = RANDOM.nextInt(10000); // 0000~9999
        return String.format("%s%s#%04d", adjective, noun, number);
    }
}
