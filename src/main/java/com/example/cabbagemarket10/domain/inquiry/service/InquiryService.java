package com.example.cabbagemarket10.domain.inquiry.service;

import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.domain.client.repository.ClientRepository;
import com.example.cabbagemarket10.domain.inquiry.dto.request.InquiryCreateRequest;
import com.example.cabbagemarket10.domain.inquiry.dto.request.InquiryUpdateRequest;
import com.example.cabbagemarket10.domain.inquiry.dto.response.InquiryCreateResponse;
import com.example.cabbagemarket10.domain.inquiry.dto.response.InquiryListResponse;
import com.example.cabbagemarket10.domain.inquiry.dto.response.InquiryUpdateResponse;
import com.example.cabbagemarket10.domain.inquiry.entity.InquiryLog;
import com.example.cabbagemarket10.domain.inquiry.repository.InquiryLogRepository;
import com.example.cabbagemarket10.domain.item.entity.Item;
import com.example.cabbagemarket10.domain.item.repository.ItemRepository;
import com.example.cabbagemarket10.global.exception.BusinessException;
import com.example.cabbagemarket10.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InquiryService {

    private static final String QUESTION_STATUS = "QUESTION";

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
    public void deleteInquiry(Long inquiryId, Long authorId) {
      inquiryLogRepository.delete(inquiryLog);
  
      return InquiryUpdateResponse.from(inquiryLog);
  }

    @Transactional
    public InquiryUpdateResponse updateInquiry(Long inquiryId, Long authorId, InquiryUpdateRequest request) {

        InquiryLog inquiryLog = inquiryLogRepository.findRootInquiryByIdWithAuthor(inquiryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INQUIRY_NOT_FOUND));

        if (!inquiryLog.getAuthor().getId().equals(authorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        inquiryLog.update(request.title(), request.contents());
        inquiryLogRepository.flush();

        return InquiryUpdateResponse.from(inquiryLog);
    }
}
