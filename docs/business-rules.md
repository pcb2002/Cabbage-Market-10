# 비즈니스 규칙

현재 문서는 서비스와 엔티티의 **현재 구현 규칙**만 적는다.

## 회원

- `email`은 유일해야 한다.
- 비밀번호는 해시로 저장한다.
- `status != ACTIVE` 회원은 로그인과 토큰 재발급이 차단된다.
- 탈퇴 회원은 `is_deleted = true`로 soft delete 처리된다.

## 카테고리

- 카테고리 목록은 `sortOrder ASC`로 조회한다.
- 카테고리는 `isActive`로 활성 여부를 관리한다.
- 현재 구현에는 계층형 `parentId`가 없다.

## 상품

- 등록 상품은 `isDraft = false`, 임시저장 상품은 `isDraft = true`다.
- 상품 상세 조회 시 조회수가 먼저 증가한다.
- 등록된 경매 상품은 수정할 수 없다.
- 등록된 직거래 상품은 수정할 수 있다.
- 임시저장 상품은 판매 상태를 변경할 수 없다.
- 직거래 상품을 `SOLD_OUT`으로 바꿀 때는 `buyerId`가 필요하다.
- `buyerId`는 판매자 본인일 수 없다.
- 임시저장 상품 삭제는 hard delete다.
- 게시된 상품 삭제는 soft delete다.

## 경매

- 경매 상품 생성 시 `AuctionStatus`를 함께 만든다.
- 경매 임시저장은 `closeDate`가 있을 때만 `AuctionStatus`를 만든다.
- 경매 게시 시 `AuctionStatus`가 존재하고 `closeDate > now`여야 한다.
- 입찰은 본인 상품에 할 수 없다.
- 입찰은 `tradeType = AUCTION`, `isDraft = false`, `tradeStatus = ON_SALE`일 때만 가능하다.
- 입찰가는 현재 최고가보다 커야 한다.
- 입찰 마감 이후 요청은 거절한다.
- 입찰 성공 시 `AuctionBidHistory`를 남긴다.
- 동시성 제어는 Redis Redisson 분산 락을 사용한다.

## 상품 이미지

- 이미지 업로드는 판매자만 가능하다.
- 허용 확장자는 `jpg`, `jpeg`, `png`, `webp`다.
- 파일 최대 크기는 장당 5MB다.
- 상품의 첫 이미지 첫 파일만 자동 썸네일이 된다.
- 썸네일 변경 시 기존 썸네일은 모두 해제 후 대상 이미지를 승격한다.
- 현재 썸네일 이미지는 삭제할 수 없다.
- 업로드 중 실패하면 이미 올린 스토리지 파일은 보상 삭제한다.

## 좋아요

- `ItemLike`는 `(client_id, item_id)` 복합키로 중복을 막는다.
- 좋아요 토글 시 `item.like_count`를 함께 갱신한다.
- 좋아요 변경 후 검색 v2 캐시는 커밋 후 무효화한다.

## 문의

- 문의는 상품별 루트 질문과 답변으로 관리한다.
- 질문 작성자는 `author_id`다.
- 답변 작성자는 해당 상품 판매자만 가능하다.
- 질문 수정/삭제는 질문 작성자만 가능하다.
- 답변은 질문당 최대 1개다.
- 문의 삭제는 soft delete다.

## 채팅

- 채팅방은 `item`과 `createdBy` 기준으로 생성된다.
- 채팅방 참여자 판정은 `createdBy` 또는 `item.seller`다.
- 채팅 메시지 조회는 참여자만 가능하다.
- 메시지 삭제는 작성자만 가능하다.
- 메시지 목록은 `createdAt DESC` offset 페이지네이션이다.
- 채팅방 목록은 `lastMessageAt DESC`로 조회한다.

## 리뷰

- 리뷰 작성은 `SOLD_OUT` 상품에만 가능하다.
- 판매자는 자기 상품에 리뷰를 작성할 수 없다.
- 직거래는 `item.buyer`, 경매는 `auction_status.current_bidder_id`가 실제 작성 가능 사용자다.
- 같은 작성자는 같은 상품에 리뷰를 한 번만 작성할 수 있다.
- 리뷰 조회는 받은 리뷰와 내가 작성한 리뷰만 구현되어 있다.
- 리뷰 수정/삭제 API는 현재 구현되어 있지 않다.

## 검색

- `v1`과 `v2`는 같은 검색 조건을 사용한다.
- `likedOnly=true`는 로그인 회원만 허용한다.
- `minPrice`와 `maxPrice`를 같이 주면 `minPrice <= maxPrice`여야 한다.
- `v2`는 비로그인 검색 결과만 Redis 캐시한다.
- 상품 생성/수정/상태 변경/삭제/좋아요/입찰 후 검색 v2 캐시는 커밋 후 무효화한다.

## 인기 검색어

- 공백 검색어는 집계하지 않는다.
- 일별 키로 Redis Sorted Set에 집계한다.
- 로그인 회원은 `clientId`, 비회원은 `sessionId` 기준으로 dedup TTL 동안 중복 집계를 막는다.
- Redis가 없으면 인기 검색어 조회는 빈 목록을 반환한다.
