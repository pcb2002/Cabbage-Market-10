package com.example.cabbagemarket10.domain.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.example.cabbagemarket10.domain.client.dto.request.ClientMyInfoUpdateRequest;
import com.example.cabbagemarket10.domain.client.dto.response.ClientMyInfoResponse;
import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.domain.client.repository.ClientRepository;
import com.example.cabbagemarket10.global.exception.BusinessException;
import com.example.cabbagemarket10.global.exception.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ClientService clientService;

    @DisplayName("내 정보 수정 시 전달된 필드만 변경한다")
    @Test
    void 내_정보_수정_시_전달된_필드만_변경한다() {
        Client client = Client.create(
                "client@example.com",
                "encodedPassword",
                "beforeNickname",
                "기존이름",
                "010-1234-5678");
        ReflectionTestUtils.setField(client, "id", 1L);
        ClientMyInfoUpdateRequest request = new ClientMyInfoUpdateRequest(
                "afterNickname",
                null,
                null,
                "http://localhost:9000/me.png"
        );

        given(clientRepository.findById(1L)).willReturn(Optional.of(client));

        ClientMyInfoResponse response = clientService.updateMyInfo(1L, request);

        assertThat(response.nickname()).isEqualTo("afterNickname");
        assertThat(response.name()).isEqualTo("기존이름");
        assertThat(response.phone()).isEqualTo("010-1234-5678");
        assertThat(response.profileImageUrl()).isEqualTo("http://localhost:9000/me.png");
        assertThat(client.getNickname()).isEqualTo("afterNickname");
        assertThat(client.getName()).isEqualTo("기존이름");
    }

    @DisplayName("내 정보 수정 시 회원이 없으면 CLIENT_NOT_FOUND 예외가 발생한다")
    @Test
    void 내_정보_수정_시_회원이_없으면_CLIENT_NOT_FOUND_예외가_발생한다() {
        ClientMyInfoUpdateRequest request = new ClientMyInfoUpdateRequest(
                "afterNickname",
                null,
                null,
                null
        );

        given(clientRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.updateMyInfo(1L, request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CLIENT_NOT_FOUND));
    }
}
