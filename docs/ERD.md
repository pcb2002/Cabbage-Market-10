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

    client ||--o{ follow : follows
    client ||--o{ follow : followed_by

    client ||--o{ chatRoom : creates
    item ||--o{ chatRoom : discussed_in
    chatRoom ||--|{ chatMember : has
    client ||--o{ chatMember : joins
    chatRoom ||--o{ chatMessage : contains
    client ||--o{ chatMessage : sends

    client ||--o{ review : writes
    client ||--o{ review : receives

    item |o--|| auctionStatus : open
    client ||--o{ auctionStatus : current_high_bidder

    inquiry |o--|| inquiry : answers

    client["CLIENT"] {
        bigint id PK
        varchar email UK
        varchar password
        varchar nickname
        varchar name
        varchar phone
        varchar profile_image_url
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
        bigint id PK
        bigint item_id FK
        bigint created_by FK
        varchar status
        datetime last_message_at
        datetime created_at
        datetime updated_at
    }

    chatMember["CHAT_MEMBER"] {
        bigint chat_room_id PK, FK
        bigint client_id PK, FK
        bigint last_read_message_id FK
        datetime joined_at
        datetime left_at
    }

    chatMessage["CHAT_MESSAGE"] {
        bigint id PK
        bigint chat_room_id FK
        bigint sender_id FK
        varchar message_type
        text content
        varchar image_url
        datetime created_at
        datetime deleted_at
    }

    review["REVIEW"] {
        bigint id PK
        bigint reviewer_id FK
        bigint reviewee_id FK
        int rating
        text content
        datetime created_at
        datetime updated_at
        datetime deleted_at
    }

    auctionStatus["AUCTION_STATUS"] {
        bigint item_id PK,FK
        bigint current_bid
        bigint current_bidder_id FK
        datetime close_date
    }
```

## Entity 목록

| Domain          | Entity                                   |
|-----------------|------------------------------------------|
| auth/membership | Client                                   |
| categories      | Category                                 |
| Items           | Item, ItemImage, ItemLike, AuctionStatus |
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
- The `contents` request field of the inquiry API is stored in `inquiry_log.description`.
- The Primary Key of `follow` is `(follower_id, following_id)`.
- The Primary Key of `chat_member` is `(chat_room_id, client_id)`.
- `auction_status.item_id` is both the PK and FK to `item.id`.
- `auction_status.current_bidder_id` is a `client.id` that refers to the current highest bidder and can be nullable before any bids are placed.

## Deletion policies

| Entity        | Policies                   |
|---------------|----------------------------|
| Client        | Soft Delete, `is_deleted`  |
| Item          | Soft Delete, `is_deleted`  |
| InquiryLog    | Soft Delete, `is_deleted`  |
| ChatMessage   | Soft Delete, `deleted_at`  |
| Review        | Soft Delete, `deleted_at`  |
| ItemImage     | Hard Delete                |
| ItemLike      | Hard Delete                |
| Follow        | Hard Delete                |
| ChatMember    | record `left_at`           |
| Category      | `is_active` = false        |
| AuctionStatus | Manage with Item lifecycle |
