package com.example.cabbagemarket10.domain.inquiry.dto.response;

import com.example.cabbagemarket10.domain.inquiry.entity.InquiryLog;

public record InquiryAnswerCreateResponse(
        InquiryAnswerDataResponse answerData,
        Long inquiryId
) {

    public static InquiryAnswerCreateResponse from(InquiryLog answer) {
        return new InquiryAnswerCreateResponse(
                InquiryAnswerDataResponse.from(answer),
                answer.getTargetInquiry().getId()
        );
    }
}
