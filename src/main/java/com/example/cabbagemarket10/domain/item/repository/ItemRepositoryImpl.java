package com.example.cabbagemarket10.domain.item.repository;

import com.example.cabbagemarket10.domain.item.dto.response.ItemDetailResponse;
import com.example.cabbagemarket10.domain.item.dto.response.ItemListItemResponse;
import com.example.cabbagemarket10.domain.item.dto.response.MyItemListItemResponse;
import com.example.cabbagemarket10.domain.item.dto.response.MyLikedItemResponse;
import com.example.cabbagemarket10.domain.item.enums.TradeStatus;
import com.example.cabbagemarket10.global.exception.BusinessException;
import com.example.cabbagemarket10.global.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import java.util.*;

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
            "likeCount", "i.likeCount",
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
                            thumbnail.imageUrl,
                            i.title,
                            i.initialPrice,
                            a.currentBid,
                            i.tradeStatus,
                            a.closeDate,
                            i.likeCount
                        )
                        from Item i
                        left join AuctionStatus a on a.item = i
                        left join ItemImage thumbnail on thumbnail.item = i and thumbnail.isThumbnail = true
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

    @Override
    public Page<MyItemListItemResponse> findMyItems(Long sellerId, String tradeStatus, Pageable pageable) {
        TradeStatus parsedTradeStatus = parseTradeStatus(tradeStatus);
        String whereClause = buildMyItemsWhereClause(parsedTradeStatus);

        TypedQuery<MyItemListItemResponse> contentQuery = entityManager.createQuery(
                """
                        select new com.example.cabbagemarket10.domain.item.dto.response.MyItemListItemResponse(
                            i.id,
                            i.title,
                            i.initialPrice,
                            a.currentBid,
                            i.tradeStatus,
                            a.closeDate,
                            i.isDraft,
                            i.tradeType,
                            i.conditionType,
                            i.likeCount,
                            (select min(ii.imageUrl) from ItemImage ii where ii.item = i and ii.isThumbnail = true),
                            i.category.id,
                            i.createdAt
                        )
                        from Item i
                        left join AuctionStatus a on a.item = i
                        """
                        + whereClause
                        + buildOrderClause(pageable),
                MyItemListItemResponse.class);
        contentQuery.setParameter("sellerId", sellerId);
        bindParameters(contentQuery, null, parsedTradeStatus);
        contentQuery.setFirstResult((int) pageable.getOffset());
        contentQuery.setMaxResults(pageable.getPageSize());

        TypedQuery<Long> countQuery = entityManager.createQuery(
                """
                        select count(i.id)
                        from Item i
                        """
                        + whereClause,
                Long.class);
        countQuery.setParameter("sellerId", sellerId);
        bindParameters(countQuery, null, parsedTradeStatus);

        return new PageImpl<>(contentQuery.getResultList(), pageable, countQuery.getSingleResult());
    }

    private String buildMyItemsWhereClause(TradeStatus tradeStatus) {
        List<String> conditions = new ArrayList<>();
        conditions.add("i.isDeleted = false");
        conditions.add("i.seller.id = :sellerId");

        if (tradeStatus != null) {
            conditions.add("i.tradeStatus = :tradeStatus");
        }

        return " where " + String.join(" and ", conditions);
    }

    @Override
    public Page<MyLikedItemResponse> findLikedItems(Long clientId, Pageable pageable) {
        TypedQuery<MyLikedItemResponse> contentQuery = entityManager.createQuery(
                """
                        select new com.example.cabbagemarket10.domain.item.dto.response.MyLikedItemResponse(
                            i.id,
                            i.title,
                            i.initialPrice,
                            a.currentBid,
                            i.tradeStatus,
                            a.closeDate,
                            i.tradeType,
                            i.conditionType,
                            i.likeCount,
                            true,
                            ii.imageUrl,
                            i.category.id,
                            i.createdAt
                        )
                        from ItemLike il
                        join il.item i
                        left join AuctionStatus a on a.item = i
                        left join ItemImage ii on ii.item = i and ii.isThumbnail = true
                        where il.client.id = :clientId
                          and i.isDeleted = false
                          and i.isDraft = false
                        """
                        + buildLikedItemsOrderClause(pageable),
                MyLikedItemResponse.class);
        contentQuery.setParameter("clientId", clientId);
        contentQuery.setFirstResult((int) pageable.getOffset());
        contentQuery.setMaxResults(pageable.getPageSize());

        TypedQuery<Long> countQuery = entityManager.createQuery(
                """
                        select count(il)
                        from ItemLike il
                        join il.item i
                        where il.client.id = :clientId
                          and i.isDeleted = false
                          and i.isDraft = false
                        """,
                Long.class);
        countQuery.setParameter("clientId", clientId);

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

    private String buildLikedItemsOrderClause(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return " order by il.createdAt desc, i.id desc";
        }

        List<String> orders = new ArrayList<>();
        for (Sort.Order order : pageable.getSort()) {
            if ("likedAt".equals(order.getProperty())) {
                orders.add("il.createdAt" + (order.isAscending() ? " asc" : " desc"));
                continue;
            }

            String property = SORT_PROPERTIES.get(order.getProperty());
            if (property != null) {
                orders.add(property + (order.isAscending() ? " asc" : " desc"));
            }
        }

        if (orders.isEmpty()) {
            return " order by il.createdAt desc, i.id desc";
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

    @Override
    public Optional<ItemDetailResponse> findItemDetail(Long itemId) {
        // 보여주신 형식대로 Text Block과 DTO 직접 생성을 활용한 순수 JPQL 작성
        String jpql = """
                select new com.example.cabbagemarket10.domain.item.dto.response.ItemDetailResponse(
                    i.id,
                    i.title,
                    i.description,
                    i.initialPrice,
                    a.currentBid,
                    i.tradeStatus,
                    a.closeDate,
                    i.viewCount,
                    i.likeCount,
                    i.inquiryCount
                )
                from Item i
                left join AuctionStatus a on a.item = i
                where i.id = :itemId
                  and i.isDeleted = false
                  and i.isDraft = false
                """;

        try {
            ItemDetailResponse result = entityManager.createQuery(jpql, ItemDetailResponse.class)
                    .setParameter("itemId", itemId)
                    .getSingleResult();

            return Optional.of(result);
        } catch (NoResultException e) {
            // 결과가 없을 경우 예외 대신 빈 Optional 반환 (Service에서 ITEM_NOT_FOUND 처리)
            return Optional.empty();
        }
    }

    @Override
    public int incrementViewCount(Long itemId) {
        return entityManager.createQuery("""
                        update Item i
                        set i.viewCount = coalesce(i.viewCount, 0) + 1
                        where i.id = :itemId
                          and i.isDeleted = false
                          and i.isDraft = false
                        """)
                .setParameter("itemId", itemId)
                .executeUpdate();
    }
}
