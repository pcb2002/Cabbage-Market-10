package com.example.cabbagemarket10.domain.inquiry.dto.response;

import com.example.cabbagemarket10.domain.inquiry.entity.InquiryLog;
import java.time.LocalDateTime;

public record InquiryAnswerDataResponse(
        Long id,
        String authorName,
        String contents,
        LocalDateTime date
) {

    public static InquiryAnswerDataResponse from(InquiryLog answer) {
        return new InquiryAnswerDataResponse(
                answer.getId(),
                answer.getAuthor().getName(),
                answer.getDescription(),
                answer.getCreatedAt()
        );
    }
}
