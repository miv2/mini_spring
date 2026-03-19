package co.kr.mini_spring.post.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.CollectionUtils;

import java.util.List;

public class HashtagListValidator implements ConstraintValidator<ValidHashtags, List<String>> {

    @Override
    public boolean isValid(List<String> hashtags, ConstraintValidatorContext context) {
        if (hashtags == null || CollectionUtils.isEmpty(hashtags)) {
            return true;
        }

        for (String hashtag : hashtags) {
            if (!HashtagNormalizer.isValid(hashtag)) {
                return false;
            }
        }

        return true;
    }
}
