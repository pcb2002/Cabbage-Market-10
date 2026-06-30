package com.example.cabbagemarket10.global;

import com.example.cabbagemarket10.domain.client.repository.ClientRepository;
import com.example.cabbagemarket10.global.exception.BusinessException;
import com.example.cabbagemarket10.global.exception.ErrorCode;
import com.example.cabbagemarket10.global.security.jwt.AuthenticatedClient;
import com.example.cabbagemarket10.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final ClientRepository clientRepository;
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authorizationHeader = accessor.getFirstNativeHeader("Authorization");
            if (!StringUtils.hasText(authorizationHeader)
                    || !authorizationHeader.startsWith(BEARER_PREFIX)) {
                throw new BusinessException(ErrorCode.ACCESS_TOKEN_MISSING);
            }

            String token = authorizationHeader.substring(BEARER_PREFIX.length());
            Long userId = jwtTokenProvider.validateAccessToken(token).clientId();
            var client = clientRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_TOKEN_INVALID));
            accessor.setUser(new AuthenticatedClient(client.getId(), client.getEmail()));
        }

        return message;
    }
}
