# 비즈니스 규칙

## 회원

- Client는 계정 주체다.
- `email`은 유일해야 한다.
- `password`는 평문 저장을 금지하고 해시로 저장한다.
- `status`는 계정 상태를 표현한다.
- `is_verified`는 인증 완료 여부다.
- 탈퇴 회원은 `is_deleted`을 기록하고 일반 조회에서 제외한다.

## 카테고리

- Category는 자기 참조 `parent_id`로 계층을 표현한다.
- `name`은 유일하다.
- 삭제 대신 `is_active = false`로 비활성화한다.
- 목록 조회는 `sort_order`를 우선한다.

## 상품

- Item은 회원이 판매하는 게시글이다.
- `seller_id`는 판매자 Client를 참조한다.
- `trade_type`은 일반 거래와 경매 등 거래 방식을 표현한다.
- `trade_status`는 판매 진행 상태를 표현한다.
- `is_draft = true`인 상품은 임시저장 상태다.
- 삭제는 `is_deleted = true`로 처리한다.
- `view_count`, `like_count`, `inquiry_count`는 조회 성능을 위한 카운트 컬럼이다.
- 상품 정보 수정은 임시저장 상품과 등록된 직거래 상품만 가능하며, 등록된 경매 상품은 수정할 수 없다.
- 판매 상태 변경은 등록된 상품에만 가능하며, 임시저장 상품의 판매 상태는 변경할 수 없다.

## 이미지

- ItemImage는 Item에 속한다.
- `sort_order`로 노출 순서를 정한다.
- `is_thumbnail = true`인 이미지는 상품 대표 이미지다.
- 상품별 대표 이미지는 하나만 허용한다.

## 좋아요

- ItemLike는 `(client_id, item_id)` 복합키로 중복을 막는다.
- 좋아요 추가·취소 시 `item.like_count`를 함께 갱신한다.
- 삭제는 Hard Delete다.

## 문의

- Inquiry는 상품에 대한 공개 문의다.
- 작성자는 `author_id`로 기록한다.
- 답변은 판매자만 등록, 수정, 삭제할 수 있다.
- 문의 삭제는 `is_deleted = true`로 처리한다.

## 팔로우

- Follow는 `(follower_id, following_id)` 복합키로 중복을 막는다.
- 자기 자신 팔로우는 허용하지 않는다.
- 팔로우 취소는 Hard Delete다.

## 채팅

- ChatRoom은 특정 Item을 기준으로 생성된다.
- `created_by`는 채팅방 생성자다.
- ChatMember는 참여자와 읽음 위치를 관리한다.
- 채팅방 나가기는 `left_at`을 기록한다.
- ChatMessage는 텍스트 또는 이미지 메시지를 표현한다.
- 메시지 삭제는 `deleted_at`으로 처리한다.

## 리뷰

- Review는 작성자 `reviewer_id`와 대상자 `reviewee_id`를 가진다.
- `rating`은 점수, `content`는 후기 내용이다.
- 리뷰 수정은 작성자만 가능하다.
- 리뷰 삭제는 `deleted_at`으로 처리한다.

## 경매

- AuctionStatus는 Item과 1:1이다.
- `current_bid`는 현재 최고 입찰가다.
- `current_bidder_id`는 현재 최고 입찰자다.
- 입찰가는 현재가보다 높아야 한다.
- `close_date` 이후 입찰은 거절한다.
- 입찰 동시성 제어 방식은 구현 전 확정한다.
