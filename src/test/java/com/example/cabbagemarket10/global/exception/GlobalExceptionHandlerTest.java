package com.example.cabbagemarket10.global.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.cabbagemarket10.global.common.CommonResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.TestController.class})
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @DisplayName("성공 응답은 공통 응답 형식으로 반환한다")
    @Test
    void 성공_응답은_공통_응답_형식으로_반환한다() throws Exception {
        mockMvc.perform(get("/test/success"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.message").doesNotExist())
                .andExpect(jsonPath("$.data.name").value("배추"));
    }

    @DisplayName("요청 본문 검증 실패는 공통 오류 응답으로 반환한다")
    @Test
    void 요청_본문_검증_실패는_공통_오류_응답으로_반환한다() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("이름은 필수입니다."))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @DisplayName("요청 파라미터 검증 실패는 공통 오류 응답으로 반환한다")
    @Test
    void 요청_파라미터_검증_실패는_공통_오류_응답으로_반환한다() throws Exception {
        mockMvc.perform(get("/test/query").param("count", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("수량은 1 이상입니다."));
    }

    @DisplayName("요청 파라미터 타입 오류는 400 공통 오류 응답으로 반환한다")
    @Test
    void 요청_파라미터_타입_오류는_400_공통_오류_응답으로_반환한다() throws Exception {
        mockMvc.perform(get("/test/query").param("count", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("요청값 검증에 실패했습니다."));
    }

    @DisplayName("공통 예외는 선언한 에러 코드와 상태로 반환한다")
    @Test
    void 공통_예외는_선언한_에러_코드와_상태로_반환한다() throws Exception {
        mockMvc.perform(get("/test/global-exception"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("DUPLICATED_EMAIL"))
                .andExpect(jsonPath("$.message").value("이미 사용 중인 이메일입니다."));
    }

    @DisplayName("처리되지 않은 예외는 공통 내부 서버 오류 응답으로 반환한다")
    @Test
    void 처리되지_않은_예외는_공통_내부_서버_오류_응답으로_반환한다() throws Exception {
        mockMvc.perform(get("/test/runtime-exception"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("서버 내부 오류가 발생했습니다."));
    }

    @DisplayName("없는 경로는 404 공통 오류 응답으로 반환한다")
    @Test
    void 없는_경로는_404_공통_오류_응답으로_반환한다() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("요청한 자원을 찾을 수 없습니다."));
    }

    @DisplayName("지원하지 않는 HTTP 메서드는 405 공통 오류 응답으로 반환한다")
    @Test
    void 지원하지_않는_HTTP_메서드는_405_공통_오류_응답으로_반환한다() throws Exception {
        mockMvc.perform(put("/test/success"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message").value("지원하지 않는 HTTP 메서드입니다."));
    }

    @Validated
    @RestController
    @RequestMapping("/test")
    static class TestController {

        @GetMapping("/success")
        public org.springframework.http.ResponseEntity<CommonResponse<Map<String, String>>> success() {
            return CommonResponse.success(HttpStatus.OK, Map.of("name", "배추"))
                    .toResponseEntity(HttpStatus.OK);
        }

        @PostMapping("/validation")
        public org.springframework.http.ResponseEntity<CommonResponse<Void>> validation(
                @Valid @RequestBody TestRequest request) {
            return CommonResponse.success(HttpStatus.CREATED).toResponseEntity(HttpStatus.CREATED);
        }

        @GetMapping("/query")
        public org.springframework.http.ResponseEntity<CommonResponse<Void>> query(
                @RequestParam @Min(value = 1, message = "수량은 1 이상입니다.") Integer count) {
            return CommonResponse.success(HttpStatus.OK).toResponseEntity(HttpStatus.OK);
        }

        @GetMapping("/global-exception")
        public org.springframework.http.ResponseEntity<CommonResponse<Void>> globalException() {
            throw new BusinessException(ErrorCode.DUPLICATED_EMAIL);
        }

        @GetMapping("/runtime-exception")
        public org.springframework.http.ResponseEntity<CommonResponse<Void>> runtimeException() {
            throw new RuntimeException("boom");
        }
    }

    record TestRequest(@NotBlank(message = "이름은 필수입니다.") String name) {}
}
