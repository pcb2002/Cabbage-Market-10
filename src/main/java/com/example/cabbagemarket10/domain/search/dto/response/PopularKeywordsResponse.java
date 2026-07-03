package com.example.cabbagemarket10.domain.search.dto.response;

import java.util.List;

public record PopularKeywordsResponse(
        List<PopularKeywordResponse> keywords
) {
}
