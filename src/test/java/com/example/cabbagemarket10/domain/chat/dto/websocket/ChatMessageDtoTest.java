package com.example.cabbagemarket10.domain.chat.dto.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.cabbagemarket10.domain.chat.entity.MessageType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChatMessageDtoTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @DisplayName("메시지 내용과 타입이 있으면 검증을 통과한다")
    @Test
    void 메시지_내용과_타입이_있으면_검증을_통과한다() {
        Set<ConstraintViolation<ChatMessageDto>> violations =
                validator.validate(new ChatMessageDto("안녕하세요", MessageType.TEXT));

        assertThat(violations).isEmpty();
    }

    @DisplayName("메시지 내용이 공백이면 검증에 실패한다")
    @Test
    void 메시지_내용이_공백이면_검증에_실패한다() {
        Set<ConstraintViolation<ChatMessageDto>> violations =
                validator.validate(new ChatMessageDto(" ", MessageType.TEXT));

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("content");
    }

    @DisplayName("메시지 타입이 없으면 검증에 실패한다")
    @Test
    void 메시지_타입이_없으면_검증에_실패한다() {
        Set<ConstraintViolation<ChatMessageDto>> violations =
                validator.validate(new ChatMessageDto("안녕하세요", null));

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("contentType");
    }
}
