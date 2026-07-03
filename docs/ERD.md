# ERD

현재 문서는 JPA 엔티티 기준이다.

## Mermaid

```mermaid
erDiagram
    CATEGORY ||--o{ ITEM : classifies
    CLIENT ||--o{ ITEM : sells
    CLIENT ||--o{ ITEM : buys
    ITEM ||--o{ ITEM_IMAGE : has
    ITEM ||--o{ ITEM_LIKE : receives
    CLIENT ||--o{ ITEM_LIKE : adds

    ITEM ||--o{ INQUIRY_LOG : has
    CLIENT ||--o{ INQUIRY_LOG : writes
    INQUIRY_LOG ||--o| INQUIRY_LOG : answers

    ITEM ||--o{ CHAT_ROOM : discussed_in
    CLIENT ||--o{ CHAT_ROOM : creates
    CHAT_ROOM ||--o{ CHAT_MESSAGE : contains
    CLIENT ||--o{ CHAT_MESSAGE : sends

    ITEM ||--o{ REVIEW : reviewed
    CLIENT ||--o{ REVIEW : writes
    CLIENT ||--o{ REVIEW : receives

    CLIENT ||--o{ FOLLOW : follows
    CLIENT ||--o{ FOLLOW : followed_by

    ITEM ||--|| AUCTION_STATUS : has
    AUCTION_STATUS ||--o{ AUCTION_BID_HISTORY : records

    CLIENT {
        bigint id PK
        varchar email UK
        varchar password
        varchar nickname
        varchar name
        varchar phone
        varchar profile_image_url
        varchar status
        boolean is_verified
        boolean is_deleted
        datetime created_at
        datetime updated_at
    }

    CATEGORY {
        bigint id PK
        varchar name
        int sort_order
        boolean is_active
        datetime created_at
        datetime updated_at
    }

    ITEM {
        bigint id PK
        bigint category_id FK
        bigint seller_id FK
        bigint buyer_id FK
        varchar trade_type
        varchar title
        text description
        bigint initial_price
        varchar condition_type
        varchar trade_status
        boolean is_draft
        boolean is_deleted
        bigint view_count
        bigint like_count
        bigint inquiry_count
        datetime created_at
        datetime updated_at
    }

    ITEM_IMAGE {
        bigint id PK
        bigint item_id FK
        varchar image_url
        int sort_order
        boolean is_thumbnail
        datetime created_at
        datetime updated_at
    }

    ITEM_LIKE {
        bigint client_id PK FK
        bigint item_id PK FK
        datetime created_at
    }

    INQUIRY_LOG {
        bigint id PK
        bigint item_id FK
        bigint author_id FK
        bigint target_inquiry_id FK UK
        varchar title
        text description
        varchar status
        boolean is_deleted
        datetime created_at
        datetime updated_at
    }

    FOLLOW {
        bigint follower_id PK FK
        bigint following_id PK FK
        datetime created_at
    }

    CHAT_ROOM {
        varchar id PK
        bigint item_id FK
        bigint created_by FK
        datetime last_message_at
        datetime created_at
        datetime updated_at
    }

    CHAT_MESSAGE {
        bigint id PK
        varchar chat_room_id FK
        bigint sender_id FK
        varchar message_type
        text content
        varchar image_url
        datetime created_at
        datetime deleted_at
    }

    REVIEW {
        bigint id PK
        bigint item_id FK
        bigint reviewer_id FK
        bigint reviewee_id FK
        int rating
        text content
        boolean is_deleted
        datetime created_at
        datetime updated_at
    }

    AUCTION_STATUS {
        bigint item_id PK FK
        bigint current_bid
        bigint current_bidder_id
        datetime close_date
    }

    AUCTION_BID_HISTORY {
        bigint id PK
        bigint item_id
        bigint bidder_id
        bigint previous_bid
        bigint bid_price
        datetime created_at
    }
```

## 현재 코드 기준 차이 메모

- `Category`는 `parent_id`가 없다.
- `Category.name` 유니크 제약은 현재 엔티티에 없다.
- `Client.role` 컬럼은 없다.
- `ChatMember` 엔티티는 없다.
- `ChatRoom.id`는 `UUID String`이다.
- `ChatMessage`는 `deleted_at` soft delete를 쓴다.
- `AuctionStatus.current_bidder_id`는 연관관계가 아니라 `Long` 값으로 저장한다.

## 주요 제약

- `client.email`은 unique다.
- `item_like` PK는 `(client_id, item_id)`다.
- `follow` PK는 `(follower_id, following_id)`다.
- `follow`는 `follower_id <> following_id` check constraint가 있다.
- `review`는 `(item_id, reviewer_id)` unique 제약이 있다.
- `inquiry_log.target_inquiry_id`는 unique라 질문당 답변은 최대 1개다.
- `auction_status.item_id`는 PK이자 `item.id` FK다.

## 삭제 정책

| Entity | 정책 |
|---|---|
| Client | Soft Delete, `is_deleted` |
| Item | 게시글 Soft Delete, `is_deleted` |
| Item draft | Hard Delete |
| InquiryLog | Soft Delete, `is_deleted` |
| Review | Soft Delete, `is_deleted` |
| ChatMessage | Soft Delete, `deleted_at` |
| ItemImage | Hard Delete |
| ItemLike | Hard Delete |
| Follow | Hard Delete |
| AuctionStatus | Item 생명주기에 종속 |
| AuctionBidHistory | 입찰 이력 유지 |
