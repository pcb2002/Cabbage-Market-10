package com.example.cabbagemarket10.global;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.domain.client.repository.ClientRepository;
import com.example.cabbagemarket10.global.exception.BusinessException;
import com.example.cabbagemarket10.global.exception.ErrorCode;
import com.example.cabbagemarket10.global.security.jwt.AuthenticatedClient;
import com.example.cabbagemarket10.global.security.jwt.JwtClaims;
import com.example.cabbagemarket10.global.security.jwt.JwtTokenProvider;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StompAuthInterceptorTest {

    private static final String BEARER_TOKEN = "Bearer valid-token";

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private MessageChannel messageChannel;

    @InjectMocks
    private StompAuthInterceptor stompAuthInterceptor;

    @DisplayName("CONNECT 요청에 유효한 Bearer 토큰이 있으면 Principal을 설정한다")
    @Test
    void CONNECT_요청에_유효한_Bearer_토큰이_있으면_Principal을_설정한다() {
        Client client = createClient(1L, "client@example.com");
        given(jwtTokenProvider.validateAccessToken("valid-token"))
                .willReturn(createClaims(1L, "client@example.com"));
        given(clientRepository.findById(1L)).willReturn(Optional.of(client));

        Message<?> result = stompAuthInterceptor.preSend(
                createMessage(StompCommand.CONNECT, BEARER_TOKEN),
                messageChannel);

        StompHeaderAccessor accessor = StompHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertThat(accessor).isNotNull();
        assertThat(accessor.getUser()).isInstanceOfSatisfying(AuthenticatedClient.class, principal -> {
            assertThat(principal.clientId()).isEqualTo(1L);
            assertThat(principal.email()).isEqualTo("client@example.com");
        });
    }

    @DisplayName("CONNECT 요청에 Authorization 헤더가 없으면 ACCESS_TOKEN_MISSING 예외가 발생한다")
    @Test
    void CONNECT_요청에_Authorization_헤더가_없으면_ACCESS_TOKEN_MISSING_예외가_발생한다() {
        assertThatThrownBy(() -> stompAuthInterceptor.preSend(
                createMessage(StompCommand.CONNECT, null),
                messageChannel))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCESS_TOKEN_MISSING));
    }

    @DisplayName("CONNECT 요청의 Authorization 헤더가 Bearer 형식이 아니면 ACCESS_TOKEN_MISSING 예외가 발생한다")
    @Test
    void CONNECT_요청의_Authorization_헤더가_Bearer_형식이_아니면_ACCESS_TOKEN_MISSING_예외가_발생한다() {
        assertThatThrownBy(() -> stompAuthInterceptor.preSend(
                createMessage(StompCommand.CONNECT, "Basic valid-token"),
                messageChannel))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCESS_TOKEN_MISSING));
    }

    @DisplayName("CONNECT 요청의 토큰에 해당하는 회원이 없으면 ACCESS_TOKEN_INVALID 예외가 발생한다")
    @Test
    void CONNECT_요청의_토큰에_해당하는_회원이_없으면_ACCESS_TOKEN_INVALID_예외가_발생한다() {
        given(jwtTokenProvider.validateAccessToken("valid-token"))
                .willReturn(createClaims(1L, "client@example.com"));
        given(clientRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> stompAuthInterceptor.preSend(
                createMessage(StompCommand.CONNECT, BEARER_TOKEN),
                messageChannel))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCESS_TOKEN_INVALID));
    }

    @DisplayName("CONNECT가 아닌 STOMP 메시지는 인증 처리를 하지 않는다")
    @Test
    void CONNECT가_아닌_STOMP_메시지는_인증_처리를_하지_않는다() {
        Message<?> message = createMessage(StompCommand.SEND, null);

        Message<?> result = stompAuthInterceptor.preSend(message, messageChannel);

        assertThat(result).isSameAs(message);
        verifyNoInteractions(jwtTokenProvider, clientRepository);
    }

    private Message<byte[]> createMessage(StompCommand command, String authorizationHeader) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setLeaveMutable(true);
        if (authorizationHeader != null) {
            accessor.addNativeHeader("Authorization", authorizationHeader);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private JwtClaims createClaims(Long clientId, String email) {
        Instant now = Instant.parse("2026-06-30T00:00:00Z");
        return new JwtClaims(
                clientId,
                email,
                "ROLE_CLIENT",
                "jti",
                now,
                now.plusSeconds(3600));
    }

    private Client createClient(Long id, String email) {
        Client client = Client.create(
                email,
                "encodedPassword",
                "배추판매자",
                "홍길동",
                "010-1234-5678");
        ReflectionTestUtils.setField(client, "id", id);
        return client;
    }
}
