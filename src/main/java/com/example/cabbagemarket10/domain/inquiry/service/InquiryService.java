package com.example.cabbagemarket10.domain.inquiry.service;

import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.domain.client.repository.ClientRepository;
import com.example.cabbagemarket10.domain.inquiry.dto.request.InquiryCreateRequest;
import com.example.cabbagemarket10.domain.inquiry.dto.response.InquiryCreateResponse;
import com.example.cabbagemarket10.domain.inquiry.entity.InquiryLog;
import com.example.cabbagemarket10.domain.inquiry.repository.InquiryLogRepository;
import com.example.cabbagemarket10.domain.item.entity.Item;
import com.example.cabbagemarket10.domain.item.repository.ItemRepository;
import com.example.cabbagemarket10.global.exception.BusinessException;
import com.example.cabbagemarket10.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InquiryService {

    private static final String QUESTION_STATUS = "QUESTION";

    private final InquiryLogRepository inquiryLogRepository;
    private final ItemRepository itemRepository;
    private final ClientRepository clientRepository;

    @Transactional
    public InquiryCreateResponse createInquiry(Long itemId, Long authorId, InquiryCreateRequest request) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));
        Client author = clientRepository.findById(authorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLIENT_NOT_FOUND));

        InquiryLog inquiryLog = InquiryLog.builder()
                .item(item)
                .author(author)
                .title(request.title())
                .description(request.contents())
                .status(QUESTION_STATUS)
                .build();

        return InquiryCreateResponse.from(inquiryLogRepository.save(inquiryLog));
    }
}
