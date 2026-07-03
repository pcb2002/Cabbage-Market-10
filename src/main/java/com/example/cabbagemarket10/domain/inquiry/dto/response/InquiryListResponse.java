package com.example.cabbagemarket10.domain.inquiry.dto.response;

import com.example.cabbagemarket10.domain.inquiry.entity.InquiryLog;
import java.util.List;
import org.springframework.data.domain.Page;

public record InquiryListResponse(
        List<InquiryListItemResponse> itemList,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static InquiryListResponse from(Page<InquiryLog> inquiryLogs) {
        return new InquiryListResponse(
                inquiryLogs.getContent().stream()
                        .map(InquiryListItemResponse::from)
                        .toList(),
                inquiryLogs.getNumber(),
                inquiryLogs.getSize(),
                inquiryLogs.getTotalElements(),
                inquiryLogs.getTotalPages()
        );
    }
}
