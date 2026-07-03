package com.example.cabbagemarket10.global.security;

import static org.assertj.core.api.Assertions.assertThat;
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

    @DisplayName("정적 에셋 경로는 Spring Security 인증 없이 통과한다")
    @Test
    void 정적_에셋_경로는_Spring_Security_인증_없이_통과한다() throws Exception {
        mockMvc.perform(get("/assets/js/core/api.js"))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .isNotEqualTo(401)
                        .isNotEqualTo(403));
    }

    @DisplayName("정적 HTML 경로는 Spring Security 인증 없이 통과한다")
    @Test
    void 정적_HTML_경로는_Spring_Security_인증_없이_통과한다() throws Exception {
        mockMvc.perform(get("/login.html"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/chat.html"))
                .andExpect(status().isOk());
    }
}
