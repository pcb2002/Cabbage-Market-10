package com.example.cabbagemarket10.domain.item.entity;

import com.example.cabbagemarket10.common.entity.BaseEntity;
import com.example.cabbagemarket10.domain.category.entity.Category;
import com.example.cabbagemarket10.domain.item.enums.ConditionType;
import com.example.cabbagemarket10.domain.item.enums.TradeStatus;
import com.example.cabbagemarket10.domain.item.enums.TradeType;
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
@SQLDelete(sql = "UPDATE item SET is_deleted = true, deleted_at = NOW() WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class Item extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

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

    @Builder
    public Item(Category category, TradeType tradeType, String title, String description,
                Long initialPrice, ConditionType conditionType, TradeStatus tradeStatus, Boolean isDraft) {
        this.category = category;
        this.tradeType = tradeType;
        this.title = title;
        this.description = description;
        this.initialPrice = initialPrice;
        this.conditionType = conditionType;
        this.tradeStatus = tradeStatus;
        this.isDraft = (isDraft != null) ? isDraft : false;
        this.isDeleted = false;
    }
}