package com.example.cabbagemarket10.domain.inquiry.entity;

import com.example.cabbagemarket10.common.entity.BaseEntity;
import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.domain.item.entity.Item;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "inquiry_log")
@SQLDelete(sql = "UPDATE inquiry_log SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class InquiryLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private Client author;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_inquiry_id", unique = true)
    private InquiryLog targetInquiry;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private Boolean isDeleted = false;

    @Builder
    public InquiryLog(
            Item item,
            Client author,
            InquiryLog targetInquiry,
            String title,
            String description,
            String status
    ) {
        this.item = item;
        this.author = author;
        this.targetInquiry = targetInquiry;
        this.title = title;
        this.description = description;
        this.status = status;
        this.isDeleted = false;
    }

    public void update(String title, String description) {
        if (title != null) {
            this.title = title;
        }
        this.description = description;
    }
}
