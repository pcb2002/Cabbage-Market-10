package com.example.cabbagemarket10.domain.inquiry.service;

import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.domain.client.repository.ClientRepository;
import com.example.cabbagemarket10.domain.inquiry.dto.request.InquiryAnswerCreateRequest;
import com.example.cabbagemarket10.domain.inquiry.dto.request.InquiryCreateRequest;
import com.example.cabbagemarket10.domain.inquiry.dto.response.InquiryAnswerCreateResponse;
import com.example.cabbagemarket10.domain.inquiry.dto.response.InquiryCreateResponse;
import com.example.cabbagemarket10.domain.inquiry.dto.response.InquiryListResponse;
import com.example.cabbagemarket10.domain.inquiry.entity.InquiryLog;
import com.example.cabbagemarket10.domain.inquiry.repository.InquiryLogRepository;
import com.example.cabbagemarket10.domain.item.entity.Item;
import com.example.cabbagemarket10.domain.item.repository.ItemRepository;
import com.example.cabbagemarket10.global.exception.BusinessException;
import com.example.cabbagemarket10.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InquiryService {

    private static final String QUESTION_STATUS = "QUESTION";
    private static final String ANSWER_STATUS = "ANSWER";

    private final InquiryLogRepository inquiryLogRepository;
    private final ItemRepository itemRepository;
    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public InquiryListResponse getInquiries(Long itemId, int page, int size) {
        if (!itemRepository.existsById(itemId)) {
            throw new BusinessException(ErrorCode.ITEM_NOT_FOUND);
        }

        Pageable pageable = PageRequest.of(page, size);
        return InquiryListResponse.from(
                inquiryLogRepository.findRootInquiriesByItemIdWithAuthor(itemId, pageable)
        );
    }

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

    @Transactional
    public InquiryAnswerCreateResponse createAnswer(
            Long inquiryId,
            Long authorId,
            InquiryAnswerCreateRequest request
    ) {
        InquiryLog inquiry = inquiryLogRepository.findQuestionByIdWithItemSeller(inquiryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INQUIRY_NOT_FOUND));
        Client seller = inquiry.getItem().getSeller();

        if (!seller.getId().equals(authorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        if (inquiryLogRepository.existsByTargetInquiryId(inquiryId)) {
            throw new BusinessException(ErrorCode.ANSWER_ALREADY_EXISTS);
        }

        InquiryLog answer = InquiryLog.builder()
                .item(inquiry.getItem())
                .author(seller)
                .targetInquiry(inquiry)
                .title(request.title())
                .description(request.contents())
                .status(ANSWER_STATUS)
                .build();

        try {
            return InquiryAnswerCreateResponse.from(inquiryLogRepository.save(answer));
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.ANSWER_ALREADY_EXISTS);
        }
    }
}
