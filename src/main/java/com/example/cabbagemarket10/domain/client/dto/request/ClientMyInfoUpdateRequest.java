package com.example.cabbagemarket10.domain.client.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClientMyInfoUpdateRequest(
        @Pattern(regexp = ".*\\S.*", message = "닉네임은 공백일 수 없습니다.")
        @Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하로 입력해야 합니다.")
        String nickname,

        @Pattern(regexp = ".*\\S.*", message = "이름은 공백일 수 없습니다.")
        @Size(min = 1, max = 50, message = "이름은 최대 50자까지 입력할 수 있습니다.")
        String name,

        @Pattern(
                regexp = "^01[0-9]-?\\d{3,4}-?\\d{4}$",
                message = "전화번호 형식이 올바르지 않습니다.")
        String phone,

        @Pattern(
                regexp = "^https?://\\S+$",
                message = "프로필 이미지 URL 형식이 올바르지 않습니다.")
        @Size(max = 500, message = "프로필 이미지 URL은 최대 500자까지 입력할 수 있습니다.")
        String profileImageUrl
) {
}
