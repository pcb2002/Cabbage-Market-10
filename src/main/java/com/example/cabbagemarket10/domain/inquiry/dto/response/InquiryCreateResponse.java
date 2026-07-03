package com.example.cabbagemarket10.domain.inquiry.dto.response;

import com.example.cabbagemarket10.domain.inquiry.entity.InquiryLog;
import java.time.LocalDateTime;

public record InquiryCreateResponse(
        Long id,
        String authorName,
        String contents,
        LocalDateTime date
) {

    public static InquiryCreateResponse from(InquiryLog inquiryLog) {
        return new InquiryCreateResponse(
                inquiryLog.getId(),
                inquiryLog.getAuthor().getName(),
                inquiryLog.getDescription(),
                inquiryLog.getCreatedAt()
        );
    }
}
