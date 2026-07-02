package com.example.cabbagemarket10.domain.search.dto.response;

public record PopularKeywordResponse(
        int rank,
        String keyword,
        long score
) {
}
