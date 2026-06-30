package com.example.cabbagemarket10.domain.search.repository;

import com.example.cabbagemarket10.domain.auction.entity.QAuctionStatus;
import com.example.cabbagemarket10.domain.item.entity.QItem;
import com.example.cabbagemarket10.domain.item.enums.ConditionType;
import com.example.cabbagemarket10.domain.item.enums.TradeStatus;
import com.example.cabbagemarket10.domain.item.enums.TradeType;
import com.example.cabbagemarket10.domain.itemImage.entity.QItemImage;
import com.example.cabbagemarket10.domain.search.dto.response.SearchItemResponse;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ItemSearchRepositoryImpl implements ItemSearchRepository {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Page<SearchItemResponse> searchItemsV1(
            String keyword,
            Long categoryId,
            TradeStatus tradeStatus,
            TradeType tradeType,
            ConditionType conditionType,
            Long minPrice,
            Long maxPrice,
            Pageable pageable
    ) {
        QItem item = QItem.item;
        QAuctionStatus auctionStatus = QAuctionStatus.auctionStatus;
        QItemImage thumbnail = QItemImage.itemImage;

        BooleanBuilder where = new BooleanBuilder();
        where.and(item.isDraft.isFalse());
        where.and(item.isDeleted.isFalse());

        if (categoryId != null) {
            where.and(item.category.id.eq(categoryId));
        }

        if (tradeStatus != null) {
            where.and(item.tradeStatus.eq(tradeStatus));
        }

        if (tradeType != null) {
            where.and(item.tradeType.eq(tradeType));
        }

        if (conditionType != null) {
            where.and(item.conditionType.eq(conditionType));
        }

        if (minPrice != null) {
            where.and(item.initialPrice.goe(minPrice));
        }

        if (maxPrice != null) {
            where.and(item.initialPrice.loe(maxPrice));
        }

        if (keyword != null && !keyword.isBlank()) {
            String trimmedKeyword = keyword.trim();

            where.and(
                    item.title.containsIgnoreCase(trimmedKeyword)
                            .or(item.description.containsIgnoreCase(trimmedKeyword))
            );
        }

        // 현재 입찰가순 정렬일 때는 경매 상품만 대상으로 한다 (일반거래는 입찰가가 없음)
        if (isCurrentBidSort(pageable)) {
            where.and(item.tradeType.eq(TradeType.AUCTION));
        }

        List<SearchItemResponse> content = jpaQueryFactory
                .select(Projections.constructor(
                        SearchItemResponse.class,
                        item.id,
                        thumbnail.imageUrl,
                        item.category.name,
                        item.title,
                        item.tradeType,
                        item.initialPrice,
                        auctionStatus.currentBid,
                        item.tradeStatus,
                        item.likeCount,
                        item.createdAt
                ))
                .from(item)
                .join(item.category)
                .leftJoin(auctionStatus).on(auctionStatus.item.eq(item))
                .leftJoin(thumbnail).on(thumbnail.item.eq(item).and(thumbnail.isThumbnail.isTrue()))
                .where(where)
                .orderBy(buildOrderSpecifiers(pageable, item, auctionStatus))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = jpaQueryFactory
                .select(item.count())
                .from(item)
                .where(where)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }

    private OrderSpecifier<?>[] buildOrderSpecifiers(
            Pageable pageable,
            QItem item,
            QAuctionStatus auctionStatus
    ) {
        if (pageable.getSort().isUnsorted()) {
            return defaultOrder(item);
        }

        List<OrderSpecifier<?>> orders = new ArrayList<>();
        for (org.springframework.data.domain.Sort.Order sortOrder : pageable.getSort()) {
            OrderSpecifier<?> orderSpecifier = toOrderSpecifier(sortOrder, item, auctionStatus);
            if (orderSpecifier != null) {
                orders.add(orderSpecifier);
            }
        }

        if (orders.isEmpty()) {
            return defaultOrder(item);
        }

        // 동률 시 페이지 간 순서가 흔들리지 않도록 고유키(id)를 보조 정렬로 항상 추가
        orders.add(item.id.desc());

        return orders.toArray(OrderSpecifier[]::new);
    }

    private boolean isCurrentBidSort(Pageable pageable) {
        return pageable.getSort().stream()
                .anyMatch(order -> "currentBid".equals(order.getProperty()) && order.isDescending());
    }

    private OrderSpecifier<?> toOrderSpecifier(
            org.springframework.data.domain.Sort.Order order,
            QItem item,
            QAuctionStatus auctionStatus
    ) {
        Order direction = order.isAscending() ? Order.ASC : Order.DESC;

        return switch (order.getProperty()) {
            case "initialPrice" -> new OrderSpecifier<>(direction, item.initialPrice);
            case "currentBid" -> order.isDescending() ? auctionStatus.currentBid.desc() : null;
            case "createdAt" -> order.isDescending() ? item.createdAt.desc() : null;
            default -> null;
        };
    }

    private OrderSpecifier<?>[] defaultOrder(QItem item) {
        return new OrderSpecifier<?>[]{item.createdAt.desc(), item.id.desc()};
    }
}
