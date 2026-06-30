package com.example.cabbagemarket10.domain.chat.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.cabbagemarket10.global.exception.BusinessException;
import com.example.cabbagemarket10.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MessageTypeTest {

    @DisplayName("JSON 문자열은 대소문자와 관계없이 MessageType으로 역직렬화된다")
    @Test
    void JSON_문자열은_대소문자와_관계없이_MessageType으로_역직렬화된다() {
        MessageType messageType = MessageType.from("text");

        assertThat(messageType).isEqualTo(MessageType.TEXT);
    }

    @DisplayName("MessageType은 enum 이름 문자열로 직렬화된다")
    @Test
    void MessageType은_enum_이름_문자열로_직렬화된다() {
        String value = MessageType.ENTRANCE_LOG.getValue();

        assertThat(value).isEqualTo("ENTRANCE_LOG");
    }

    @DisplayName("지원하지 않는 메시지 타입이면 NOT_FOUND 예외가 발생한다")
    @Test
    void 지원하지_않는_메시지_타입이면_NOT_FOUND_예외가_발생한다() {
        assertThatThrownBy(() -> MessageType.from("UNKNOWN"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }
}
