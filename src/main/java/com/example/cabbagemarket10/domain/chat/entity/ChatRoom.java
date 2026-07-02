package com.example.cabbagemarket10.domain.chat.entity;

import com.example.cabbagemarket10.common.entity.BaseEntity;
import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.domain.item.entity.Item;
import com.example.cabbagemarket10.global.exception.BusinessException;
import com.example.cabbagemarket10.global.exception.ErrorCode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "chat_room")
public class ChatRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private Client createdBy;

    private LocalDateTime lastMessageAt;

    @Builder
    public ChatRoom(Item item, Client createdBy) {
        this.item = item;
        this.createdBy = createdBy;
    }

    public void inspectClientAsParticipant(long clientId) {
        if(clientId != createdBy.getId() && item.getSeller().getId() != clientId) {
            throw new BusinessException(ErrorCode.CLIENT_NOT_PARTICIPANT);
        }
    }

    public void updateLastMessageAt(LocalDateTime createdAt) {
        if (lastMessageAt == null || createdAt.isAfter(lastMessageAt)) {
            this.lastMessageAt = createdAt;
        }
    }
}
