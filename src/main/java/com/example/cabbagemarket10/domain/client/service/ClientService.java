package com.example.cabbagemarket10.domain.client.service;

import com.example.cabbagemarket10.domain.client.dto.request.ClientMyInfoUpdateRequest;
import com.example.cabbagemarket10.domain.client.dto.response.ClientMyInfoResponse;
import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.domain.client.repository.ClientRepository;
import com.example.cabbagemarket10.global.exception.BusinessException;
import com.example.cabbagemarket10.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public ClientMyInfoResponse getMyInfo(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLIENT_NOT_FOUND));

        return ClientMyInfoResponse.from(client);
    }

    @Transactional
    public ClientMyInfoResponse updateMyInfo(Long clientId, ClientMyInfoUpdateRequest request) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLIENT_NOT_FOUND));

        client.updateProfile(
                request.nickname(),
                request.name(),
                request.phone(),
                request.profileImageUrl()
        );

        return ClientMyInfoResponse.from(client);
    }

    public Client getClient(Long clientId) {
        return clientRepository.findById(clientId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLIENT_NOT_FOUND));
    }
}
