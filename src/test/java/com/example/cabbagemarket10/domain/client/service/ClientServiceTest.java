package com.example.cabbagemarket10.domain.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.example.cabbagemarket10.domain.client.dto.response.ClientProfileResponse;
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

    @DisplayName("회원이 존재하면 공개 프로필을 DTO로 반환한다")
    @Test
    void 회원이_존재하면_공개_프로필을_DTO로_반환한다() {
        Client client = Client.create(
                "profile@example.com",
                "encodedPassword",
                "배추프로필",
                "홍길동",
                "010-1234-5678");
        ReflectionTestUtils.setField(client, "id", 1L);

        given(clientRepository.findById(1L)).willReturn(Optional.of(client));

        ClientProfileResponse response = clientService.getClientProfile(1L);

        assertThat(response.clientId()).isEqualTo(1L);
        assertThat(response.nickname()).isEqualTo("배추프로필");
        assertThat(response.profileImageUrl()).isEmpty();
    }

    @DisplayName("회원이 없으면 공개 프로필 조회 시 CLIENT_NOT_FOUND 예외가 발생한다")
    @Test
    void 회원이_없으면_공개_프로필_조회_시_CLIENT_NOT_FOUND_예외가_발생한다() {
        given(clientRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.getClientProfile(1L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CLIENT_NOT_FOUND));
    }
}
