# ERD

## Mermaid

```mermaid
erDiagram
    direction LR

    category ||--o{ item : classifies
    itemImage }o--|| item : belongs_to
    item ||--|{ itemLike : receives
    itemLike }o--|| client : added_by
    inquiry }o--|| item : belongs_to
    inquiry }o--|| client : written_by
    item }o--|| client : sold_by
    item }o--o| client : bought_by

    client ||--o{ follow : follows
    client ||--o{ follow : followed_by

    client ||--o{ chatRoom : creates
    item ||--o{ chatRoom : discussed_in
    chatRoom ||--o{ chatMessage : contains
    client ||--o{ chatMessage : sends

    client ||--o{ review : writes
    client ||--o{ review : receives
    item ||--o{ review : reviewed_by

    item |o--|| auctionStatus : open
    auctionStatus ||--o{ auctionBidHistory : records
    client ||--o{ auctionBidHistory : bids
    inquiry |o--|| inquiry : answers

    client["CLIENT"] {
        bigint id PK
        varchar email UK
        varchar password
        varchar nickname
        varchar name
        varchar phone
        varchar(500) profile_image_url
        varchar role
        varchar status
        boolean is_verified
        datetime created_at
        datetime updated_at
        boolean is_deleted
    }

    category["CATEGORY"] {
        bigint id PK
        bigint parent_id FK
        varchar name UK
        int sort_order
        boolean is_active
        datetime created_at
        datetime updated_at
    }

    item["ITEM"] {
        bigint id PK
        bigint seller_id FK
        bigint buyer_id FK
        bigint category_id FK
        varchar trade_type
        varchar title
        text description
        bigint initial_price
        varchar condition_type
        varchar trade_status
        bigint view_count
        bigint like_count
        bigint inquiry_count
        boolean is_draft
        datetime created_at
        datetime updated_at
        boolean is_deleted
    }

    itemImage["ITEM_IMAGE"] {
        bigint id PK
        bigint item_id FK
        varchar image_url
        int sort_order
        boolean is_thumbnail
        datetime created_at
    }

    itemLike["ITEM_LIKE"] {
        bigint client_id PK, FK
        bigint item_id PK, FK
        datetime created_at
    }

    inquiry["INQUIRY_LOG"] {
        bigint id PK
        bigint item_id FK
        bigint author_id FK
        bigint target_inquiry_id FK
        varchar title
        text description
        varchar status
        datetime created_at
        datetime updated_at
        boolean is_deleted
    }

    follow["FOLLOW"] {
        bigint follower_id PK, FK
        bigint following_id PK, FK
        datetime created_at
    }

    chatRoom["CHAT_ROOM"] {
        varchar id PK
        bigint item_id FK
        bigint created_by FK
        datetime last_message_at
        datetime created_at
        datetime updated_at
    }

    chatMessage["CHAT_MESSAGE"] {
        bigint id PK
        varchar chat_room_id FK
        bigint sender_id FK
        varchar message_type
        text content
        varchar image_url
        datetime created_at
        datetime deleted_at
    }

    review["REVIEW"] {
        bigint id PK
        bigint item_id FK
        bigint reviewer_id FK
        bigint reviewee_id FK
        int rating
        text content
        datetime created_at
        datetime updated_at
        boolean is_deleted
    }

    auctionStatus["AUCTION_STATUS"] {
        bigint item_id PK,FK
        bigint current_bid
        bigint current_bidder_id
        datetime close_date
    }

    auctionBidHistory["AUCTION_BID_HISTORY"] {
        bigint id PK
        bigint item_id FK
        bigint bidder_id FK
        bigint previous_bid
        bigint bid_price
        datetime created_at
    }
```

## Entity 목록

| Domain          | Entity                                   |
|-----------------|------------------------------------------|
| auth/membership | Client                                   |
| categories      | Category                                 |
| Items           | Item, ItemImage, ItemLike, AuctionStatus, AuctionBidHistory |
| Inquiries       | InquiryLog                               |
| Follow          | Follow                                   |
| chat            | ChatRoom, ChatMember, ChatMessage        |
| reviews         | Review                                   |

## Main Constraints

- `client.email` is unique.
- `category.name`is unique.
- `category.parent_id` refers `category.id`.
- `item.seller_id` refers `client.id`.
- `item.category_id` refers `category.id`.
- The Primary key of `item_like` is `(client_id, item_id)`.
- `inquiry_log.target_inquiry_id` refers `inquiry_log.id`, can be null, and is unique so each inquiry can have at most one answer.
- The `title` request field of the inquiry API is stored in `inquiry_log.title`.
- The `contents` request field of the inquiry API is stored in `inquiry_log.description`.
- The Primary Key of `follow` is `(follower_id, following_id)`.
- `follow.follower_id` and `follow.following_id` must be different.
- The Primary Key of `chat_member` is `(chat_room_id, client_id)`.
- `auction_status.item_id` is both the PK and FK to `item.id`.
- `auction_status.current_bidder_id` stores the current highest bidder's `client.id` and can be nullable before any bids are placed.
- `auction_bid_history.item_id` stores the auction item id for each successful bid.
- `auction_bid_history.bidder_id` stores the bidder's `client.id`.

## Deletion policies

| Entity        | Policies                   |
|---------------|----------------------------|
| Client        | Soft Delete, `is_deleted`  |
| Item          | Draft: Hard Delete / Published: Soft Delete, `is_deleted` |
| InquiryLog    | Soft Delete, `is_deleted`  |
| ChatMessage   | Soft Delete, `deleted_at`  |
| Review        | Soft Delete, `is_deleted`  |
| ItemImage     | Hard Delete                |
| ItemLike      | Hard Delete                |
| Follow        | Hard Delete                |
| ChatMember    | record `left_at`           |
| Category      | `is_active` = false        |
| AuctionStatus | Manage with Item lifecycle |
| AuctionBidHistory | Keep bid audit records |
