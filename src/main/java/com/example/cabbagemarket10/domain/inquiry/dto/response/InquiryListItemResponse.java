package com.example.cabbagemarket10.domain.inquiry.dto.response;

import com.example.cabbagemarket10.domain.inquiry.entity.InquiryLog;
import java.time.LocalDateTime;

public record InquiryListItemResponse(
        Long inquiryID,
        String authorName,
        String contents,
        LocalDateTime date
) {

    public static InquiryListItemResponse from(InquiryLog inquiryLog) {
        return new InquiryListItemResponse(
                inquiryLog.getId(),
                inquiryLog.getAuthor().getName(),
                inquiryLog.getDescription(),
                inquiryLog.getCreatedAt()
        );
    }
}
