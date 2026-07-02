package com.example.cabbagemarket10.domain.search.service;

import com.example.cabbagemarket10.domain.search.config.PopularSearchProperties;
import com.example.cabbagemarket10.domain.search.dto.response.PopularKeywordResponse;
import com.example.cabbagemarket10.domain.search.dto.response.PopularKeywordsResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PopularSearchService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final PopularSearchProperties properties;

    public void recordKeyword(String keyword, Long clientId, String sessionId) {
        String normalizedKeyword = normalizeKeyword(keyword);
        if (normalizedKeyword == null) {
            return;
        }

        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return;
        }

        String dailyKey = dailyKey();
        String dedupKey = dedupKey(clientId, sessionId, normalizedKeyword);
        Boolean firstSearch = redisTemplate.opsForValue().setIfAbsent(dedupKey, "1", properties.getDedupTtl());

        if (!Boolean.TRUE.equals(firstSearch)) {
            return;
        }

        redisTemplate.opsForZSet().incrementScore(dailyKey, normalizedKeyword, 1);
        redisTemplate.expire(dailyKey, properties.getDailyKeyTtl());
    }

    public PopularKeywordsResponse getPopularKeywords() {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return new PopularKeywordsResponse(List.of());
        }

        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                .reverseRangeWithScores(dailyKey(), 0, properties.getLimit() - 1L);

        if (tuples == null || tuples.isEmpty()) {
            return new PopularKeywordsResponse(List.of());
        }

        List<PopularKeywordResponse> keywords = new ArrayList<>();
        int rank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            String value = tuple.getValue();
            Double score = tuple.getScore();
            if (value == null || score == null) {
                continue;
            }
            keywords.add(new PopularKeywordResponse(rank++, value, score.longValue()));
        }

        return new PopularKeywordsResponse(keywords);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmedKeyword = keyword.trim();
        return trimmedKeyword.isEmpty() ? null : trimmedKeyword;
    }

    private String dailyKey() {
        return properties.getDailyKeyPrefix() + LocalDate.now().format(DATE_FORMATTER);
    }

    private String dedupKey(Long clientId, String sessionId, String keyword) {
        String subject = clientId != null ? "client:" + clientId : "session:" + Objects.requireNonNullElse(sessionId, "anonymous");
        String encodedKeyword = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(keyword.getBytes(StandardCharsets.UTF_8));
        return properties.getDedupKeyPrefix()
                + subject
                + ":"
                + LocalDate.now().format(DATE_FORMATTER)
                + ":"
                + encodedKeyword;
    }
}
