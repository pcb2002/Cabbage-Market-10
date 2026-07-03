package com.example.cabbagemarket10.domain.inquiry.dto.response;

import com.example.cabbagemarket10.domain.inquiry.entity.InquiryLog;
import java.time.LocalDateTime;

public record InquiryUpdateResponse(
        Long id,
        String authorName,
        String contents,
        LocalDateTime date
) {

    public static InquiryUpdateResponse from(InquiryLog inquiryLog) {
        return new InquiryUpdateResponse(
                inquiryLog.getId(),
                inquiryLog.getAuthor().getName(),
                inquiryLog.getDescription(),
                inquiryLog.getUpdatedAt()
        );
    }
}
