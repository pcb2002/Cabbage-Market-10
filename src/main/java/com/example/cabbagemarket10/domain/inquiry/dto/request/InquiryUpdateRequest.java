package com.example.cabbagemarket10.domain.inquiry.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record InquiryUpdateRequest(
        @Pattern(regexp = ".*\\S.*", message = "제목은 공백일 수 없습니다.")
        @Size(max = 200, message = "제목은 최대 200자까지 입력할 수 있습니다.")
        String title,

        @NotBlank(message = "내용은 필수입니다.")
        @Size(max = 2000, message = "내용은 최대 2000자까지 입력할 수 있습니다.")
        String contents
) {
}
