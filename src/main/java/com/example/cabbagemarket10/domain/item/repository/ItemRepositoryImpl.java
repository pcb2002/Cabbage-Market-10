package com.example.cabbagemarket10.domain.item.repository;

import com.example.cabbagemarket10.domain.item.dto.response.ItemListItemResponse;
import com.example.cabbagemarket10.domain.item.enums.TradeStatus;
import com.example.cabbagemarket10.global.exception.BusinessException;
import com.example.cabbagemarket10.global.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@RequiredArgsConstructor
public class ItemRepositoryImpl implements ItemRepositoryCustom {

    private static final Map<String, String> SORT_PROPERTIES = Map.of(
            "itemId", "i.id",
            "title", "i.title",
            "initialPrice", "i.initialPrice",
            "currentBid", "a.currentBid",
            "tradeStatus", "i.tradeStatus",
            "closeDate", "a.closeDate",
            "createdAt", "i.createdAt",
            "updatedAt", "i.updatedAt"
    );

    private final EntityManager entityManager;

    @Override
    public Page<ItemListItemResponse> searchItems(Long categoryId, String tradeStatus, Pageable pageable) {
        TradeStatus parsedTradeStatus = parseTradeStatus(tradeStatus);
        String whereClause = buildWhereClause(categoryId, parsedTradeStatus);

        TypedQuery<ItemListItemResponse> contentQuery = entityManager.createQuery(
                """
                        select new com.example.cabbagemarket10.domain.item.dto.response.ItemListItemResponse(
                            i.id,
                            i.title,
                            i.initialPrice,
                            a.currentBid,
                            i.tradeStatus,
                            a.closeDate
                        )
                        from Item i
                        left join AuctionStatus a on a.item = i
                        """
                        + whereClause
                        + buildOrderClause(pageable),
                ItemListItemResponse.class);
        bindParameters(contentQuery, categoryId, parsedTradeStatus);
        contentQuery.setFirstResult((int) pageable.getOffset());
        contentQuery.setMaxResults(pageable.getPageSize());

        TypedQuery<Long> countQuery = entityManager.createQuery(
                """
                        select count(i.id)
                        from Item i
                        """
                        + whereClause,
                Long.class);
        bindParameters(countQuery, categoryId, parsedTradeStatus);

        return new PageImpl<>(contentQuery.getResultList(), pageable, countQuery.getSingleResult());
    }

    private TradeStatus parseTradeStatus(String tradeStatus) {
        if (tradeStatus == null || tradeStatus.isBlank()) {
            return null;
        }

        try {
            return TradeStatus.valueOf(tradeStatus.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "지원하지 않는 거래 상태입니다.");
        }
    }

    private String buildWhereClause(Long categoryId, TradeStatus tradeStatus) {
        List<String> conditions = new ArrayList<>();
        conditions.add("i.isDeleted = false");
        conditions.add("i.isDraft = false");

        if (categoryId != null) {
            conditions.add("i.category.id = :categoryId");
        }
        if (tradeStatus != null) {
            conditions.add("i.tradeStatus = :tradeStatus");
        }

        return " where " + String.join(" and ", conditions);
    }

    private String buildOrderClause(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return " order by i.createdAt desc, i.id desc";
        }

        List<String> orders = new ArrayList<>();
        for (Sort.Order order : pageable.getSort()) {
            String property = SORT_PROPERTIES.get(order.getProperty());
            if (property != null) {
                orders.add(property + (order.isAscending() ? " asc" : " desc"));
            }
        }

        if (orders.isEmpty()) {
            return " order by i.createdAt desc, i.id desc";
        }
        return " order by " + String.join(", ", orders) + ", i.id desc";
    }

    private void bindParameters(TypedQuery<?> query, Long categoryId, TradeStatus tradeStatus) {
        if (categoryId != null) {
            query.setParameter("categoryId", categoryId);
        }
        if (tradeStatus != null) {
            query.setParameter("tradeStatus", tradeStatus);
        }
    }
}
