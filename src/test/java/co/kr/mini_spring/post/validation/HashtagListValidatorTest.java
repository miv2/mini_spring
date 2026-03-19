package co.kr.mini_spring.post.validation;

import co.kr.mini_spring.post.dto.request.PostCreateRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class HashtagListValidatorTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    @Test
    void 완성형_한글_영문_숫자_해시태그는_허용한다() throws Exception {
        PostCreateRequest request = new PostCreateRequest();
        setField(request, "title", "title");
        setField(request, "content", "content");
        setField(request, "hashtags", List.of("spring", "#Java", "스프링부트2026"));

        Set<ConstraintViolation<PostCreateRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void 자모만_있는_해시태그는_거부한다() throws Exception {
        PostCreateRequest request = new PostCreateRequest();
        setField(request, "title", "title");
        setField(request, "content", "content");
        setField(request, "hashtags", List.of("ㅋㅋ", "ㅎㅎ"));

        Set<ConstraintViolation<PostCreateRequest>> violations = validator.validate(request);

        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .contains("유효하지 않은 해시태그가 포함되어 있습니다. 영문, 숫자, 완성형 한글만 사용할 수 있습니다.");
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
