package com.example.cabbagemarket10.global.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class WebSocketSecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @DisplayName("WebSocket handshake 경로는 Spring Security 인증 없이 통과한다")
    @Test
    void WebSocket_handshake_경로는_Spring_Security_인증_없이_통과한다() throws Exception {
        mockMvc.perform(get("/ws/chat"))
                .andExpect(status().isBadRequest());
    }
}
