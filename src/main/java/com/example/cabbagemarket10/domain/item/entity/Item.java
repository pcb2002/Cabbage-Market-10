package com.example.cabbagemarket10.domain.item.entity;

import com.example.cabbagemarket10.common.entity.BaseEntity;
import com.example.cabbagemarket10.domain.category.entity.Category;
import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.domain.item.enums.ConditionType;
import com.example.cabbagemarket10.domain.item.enums.TradeStatus;
import com.example.cabbagemarket10.domain.item.enums.TradeType;
import com.example.cabbagemarket10.global.exception.BusinessException;
import com.example.cabbagemarket10.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "item")
@SQLDelete(sql = "UPDATE item SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class Item extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Client seller;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeType tradeType;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Long initialPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConditionType conditionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeStatus tradeStatus;

    @Column(nullable = false)
    private Boolean isDraft;

    @Column(nullable = false)
    private Boolean isDeleted = false; // Soft Delete 플래그

    @Column(name = "view_count", nullable = false)
    private Long viewCount = 0L;

    @Column(name = "like_count", nullable = false)
    private Long likeCount = 0L;

    @Column(name = "inquiry_count", nullable = false)
    private Long inquiryCount = 0L;

    @Builder
    public Item(Category category, Client seller, TradeType tradeType, String title, String description,
                Long initialPrice, ConditionType conditionType, TradeStatus tradeStatus, Boolean isDraft) {
        this.category = category;
        this.seller = seller;
        this.tradeType = tradeType;
        this.title = title;
        this.description = description;
        this.initialPrice = initialPrice;
        this.conditionType = conditionType;
        this.tradeStatus = tradeStatus;
        this.isDraft = (isDraft != null) ? isDraft : false;
        this.isDeleted = false;
    }

    /**
     * 상품 조회수 1 증가
     */
    public void incrementViewCount() {
        // 기존 데이터가 null일 경우를 대비한 안전 장치
        if (this.viewCount == null) {
            this.viewCount = 0L;
        }
        this.viewCount++;
    }

    // 작성자 검증 로직
    public void verifySeller(Long clientId) {
        if (!this.seller.getId().equals(clientId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    public boolean isAuction() {
        return this.tradeType == TradeType.AUCTION;
    }

    public void validateUpdatable() {
        if (!this.isDraft && isAuction()) {
            throw new BusinessException(ErrorCode.ITEM_UPDATE_NOT_ALLOWED);
        }
    }

    // 상품 정보 수정 로직
    public void updateInfo(Category category, String title, String description, Long initialPrice) {
        this.category = category;
        this.title = title;
        this.description = description;
        this.initialPrice = initialPrice;
    }
}
