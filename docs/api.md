# API

## 기본 규칙

- REST API prefix는 `/api`다.
- URI는 리소스 중심으로 작성한다.
- Request는 Bean Validation으로 1차 검증한다.
- Entity를 응답으로 직접 반환하지 않는다.
- 목록 조회는 Pageable 또는 cursor를 사용한다.
- 내부 예외 메시지와 stack trace를 응답에 노출하지 않는다.

## 공통 응답 형식

성공 응답:

```json
{
  "status": 200,
  "data": {}
}
```

오류 응답:

```json
{
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "요청값 검증에 실패했습니다.",
  "data": null
}
```

공통 오류 코드:

| Status | Code | 설명 |
|---:|---|---|
| 400 | `VALIDATION_ERROR` | 요청값 검증 실패 |
| 401 | `UNAUTHORIZED` | 인증 필요 |
| 403 | `FORBIDDEN` | 접근 권한 없음 |
| 404 | `NOT_FOUND` | 요청한 자원 없음 |
| 405 | `METHOD_NOT_ALLOWED` | 지원하지 않는 HTTP 메서드 |
| 500 | `INTERNAL_SERVER_ERROR` | 서버 내부 오류 |

페이지네이션 응답:

목록 조회 API는 `data` 필드에 아래 구조를 반환한다.

```json
{
  "status": 200,
  "data": {
    "content": [],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5
  }
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `content` | array | 현재 페이지 데이터 목록 |
| `page` | int | 현재 페이지 번호 (0부터 시작) |
| `size` | int | 페이지당 항목 수 |
| `totalElements` | long | 전체 항목 수 |
| `totalPages` | int | 전체 페이지 수 |

요청 파라미터: `page` (기본값 0), `size` (기본값 20)

> 채팅 메시지 목록(`GET /api/chat-rooms/{chatRoomId}/messages`)은 실시간 삽입이 많아 offset 페이징이 부적합할 수 있다. cursor 방식 전환은 [docs/adr/README.md](adr/README.md)에서 관리한다.

## API 목록

| 기능 | 그룹 | Method | Path |
|---|---|---:|---|
| 회원가입 | 인증 | POST | `/api/auth/signup` |
| 로그인 | 인증 | POST | `/api/auth/login` |
| 로그아웃 | 인증 | POST | `/api/auth/logout` |
| 토큰 재발급 | 인증 | POST | `/api/auth/refresh` |
| 내 정보 조회 | 마이 페이지 | GET | `/api/clients/me` |
| 내 정보 수정 | 마이 페이지 | PATCH | `/api/clients/me` |
| 회원 프로필 조회 | 마이 페이지 | GET | `/api/clients/{clientId}` |
| 카테고리 목록 조회 | 카테고리 | GET | `/api/categories` |
| 상품 등록 | 상품 게시글 | POST | `/api/items` |
| 상품 임시저장 | 상품 게시글 | POST | `/api/items/drafts` |
| 상품 임시저장 게시 | 상품 게시글 | POST | `/api/items/{itemId}/publish` |
| 상품 목록 조회 | 상품 게시글 | GET | `/api/items` |
| 상품 상세 조회 | 상품 게시글 | GET | `/api/items/{itemId}` |
| 인기 검색어 조회 | 검색어 | GET | `/api/search/popular` |
| 상품 검색 v1 | 검색어 | GET | `/api/v1/items/search` |
| 상품 정보 수정 | 상품 게시글 | PUT | `/api/items/{itemId}` |
| 판매 상태 변경 | 상품 게시글 | PATCH | `/api/items/{itemId}/status` |
| 상품 삭제 | 상품 게시글 | DELETE | `/api/items/{itemId}` |
| 내 판매글 목록 | 마이 페이지 | GET | `/api/clients/me/items` |
| 내 관심목록 조회 | 마이 페이지 | GET | `/api/clients/me/likes` |
| 대표 이미지 설정 | 상품 이미지 | PATCH | `/api/items/{itemId}/images/{imageId}/thumbnail` |
| 상품 이미지 삭제 | 상품 이미지 | DELETE | `/api/items/{itemId}/images/{imageId}` |
| 상품 좋아요 토글 | 좋아요 | POST | `/api/items/{itemId}/likes` |
| 상품 문의 작성 | 문의 | POST | `/api/items/{itemId}/inquiries` |
| 상품 문의 목록 조회 | 문의 | GET | `/api/items/{itemId}/inquiries` |
| 상품 문의 수정 | 문의 | PATCH | `/api/inquiries/{inquiryId}` |
| 상품 문의 삭제 | 문의 | DELETE | `/api/inquiries/{inquiryId}` |
| 상품 문의 답변 등록 | 문의 | POST | `/api/inquiries/{inquiryId}/answer` |
| 상품 문의 답변 수정 | 문의 | PATCH | `/api/inquiries/{inquiryId}/answer` |
| 상품 문의 답변 삭제 | 문의 | DELETE | `/api/inquiries/{inquiryId}/answer` |
| 상품 리뷰 작성 | 리뷰 | POST | `/api/items/{itemId}/reviews` |
| 회원 팔로우 | 팔로우 | POST | `/api/clients/{clientId}/follows` |
| 회원 팔로우 취소 | 팔로우 | DELETE | `/api/clients/{clientId}/follows` |
| 내가 팔로우한 회원 목록 | 팔로우 | GET | `/api/clients/me/followings` |
| 나를 팔로우한 회원 목록 | 팔로우 | GET | `/api/clients/me/followers` |
| 채팅방 생성 | 채팅 | POST | `/api/items/{itemId}/chat-rooms` |
| 채팅방 조회 | 채팅 | GET | `/api/chat-rooms` |
| 채팅방 나가기 | 채팅 | POST | `/api/chat-rooms/{chatRoomId}/leave` |
| 메시지 목록 조회 | 채팅 | GET | `/api/chat-rooms/{chatRoomId}/messages` |
| 메시지 전송 | 채팅 | WS | `/api/chat-rooms/{chatRoomId}/messages` |
| 메시지 삭제 | 채팅 | DELETE | `/api/chat-messages/{messageId}` |
| 메시지 읽음 처리 | 채팅 | POST | `/api/chat-rooms/{chatRoomId}/read` |
| 받은 리뷰 목록 조회 | 리뷰 | GET | `/api/clients/{clientId}/reviews` |
| 내가 작성한 리뷰 목록 | 리뷰 | GET | `/api/clients/me/reviews/written` |
| 리뷰 수정 | 리뷰 | PATCH | `/api/reviews/{reviewId}` |
| 리뷰 삭제 | 리뷰 | DELETE | `/api/reviews/{reviewId}` |
| 입찰하기 | 경매 | POST | `/api/items/{itemId}/auction-status/bid` |

## WebSocket

| 기능 | Path |
|---|---|
| WebSocket 연결 | `/ws/chat` |

메시지 전송은 `/ws/chat` 연결 후 STOMP `SEND /api/chat-rooms/{chatRoomId}/messages`로 처리한다.

## 인증 기준

| 공개 API | 인증 필요 API |
|---|---|
| 회원가입, 로그인, 토큰 재발급, 상품 목록·상세·검색, 카테고리, 회원 공개 프로필 | 로그아웃, 내 정보, 상품 등록·수정·게시·삭제, 좋아요, 문의 작성·수정·삭제, 팔로우, 채팅, 리뷰, 입찰 |

## Notion DB 상세 명세

Notion `DB` 페이지의 API 명세 데이터베이스를 기준으로 정리한다.

### 요청 검증 요약

| 기능 | 필드 | 규칙 |
|---|---|---|
| 회원가입 | email | 필수, 공백 불가, 이메일 형식, 최대 254자 |
| 회원가입 | password | 필수, 공백 불가, 8~64자, 영문·숫자·특수문자 각 1자 이상 |
| 회원가입 | nickname | 필수, 공백 불가, 2~20자 |
| 회원가입 | name | 필수, 공백 불가, 1~50자 |
| 회원가입 | phone | 필수, 형식 `01[0-9]-?\d{3,4}-?\d{4}` |
| 로그인 | email | 필수, 이메일 형식 |
| 로그인 | password | 필수, 공백 불가 |
| 토큰 재발급 | refresh_token Cookie | 필수, 유효한 Refresh Token |
| 내 정보 수정 | nickname | 선택, 전달 시 2~20자 |
| 내 정보 수정 | name | 선택, 전달 시 1~50자 |
| 내 정보 수정 | phone | 선택, 형식 `01[0-9]-?\d{3,4}-?\d{4}` |
| 내 정보 수정 | profileImageUrl | 선택, URL 형식, 최대 500자 |
| 상품 등록 | categoryId | 필수, 존재하는 카테고리 ID |
| 상품 등록 | tradeType | 필수, `SALE` 또는 `AUCTION` |
| 상품 등록 | title | 필수, 1~100자 |
| 상품 등록 | description | 필수, 1~2000자 |
| 상품 등록 | initialPrice | 필수, 0 이상 정수 |
| 상품 등록 | conditionType | 필수, `NEW` 또는 `USED` |
| 상품 등록 | closeDate | `AUCTION`일 때 필수, 현재 시각 이후 |
| 상품 임시저장 | categoryId | 필수, 존재하는 카테고리 ID |
| 상품 임시저장 | title | 필수, 1~100자 |
| 상품 임시저장 | tradeType | 필수, `DIRECT` 또는 `AUCTION` |
| 상품 임시저장 | conditionType | 필수, `NEW` 또는 `USED` |
| 상품 임시저장 | description | 필수 |
| 상품 임시저장 | initialPrice | 필수, 0 이상 정수 |
| 상품 임시저장 | closeDate | 선택, `AUCTION`이고 전달된 경우 AuctionStatus 생성 |
| 상품 임시저장 게시 | itemId | Path 필수, 판매자 본인의 임시저장 상품 ID |
| 상품 목록 조회 | categoryId | 선택, 해당 카테고리 상품만 조회 |
| 상품 목록 조회 | tradeStatus | 선택, `ON_SALE`, `RESERVED`, `SOLD_OUT` |
| 상품 목록 조회 | page, size | 선택, 페이징 |
| 상품 검색 v1 | keyword | 선택, 최대 100자, 공백이면 전체 검색 결과 |
| 상품 검색 v1 | categoryId | 선택, 해당 카테고리 상품만 검색 |
| 상품 검색 v1 | tradeStatus | 선택, `ON_SALE`, `RESERVED`, `SOLD_OUT` |
| 상품 검색 v1 | tradeType | 선택, `DIRECT`, `AUCTION` |
| 상품 검색 v1 | conditionType | 선택, `NEW`, `USED` |
| 상품 검색 v1 | minPrice, maxPrice | 선택, 0 이상. 둘 다 전달 시 `minPrice <= maxPrice` |
| 상품 검색 v1 | page, size, sort | 선택, 페이징 및 정렬 |
| 상품 정보 수정 | categoryId | 필수, 존재하는 카테고리 ID |
| 상품 정보 수정 | title | 필수, 공백 불가 |
| 상품 정보 수정 | description | 필수, 공백 불가 |
| 상품 정보 수정 | initialPrice | 필수, 0 이상 정수 |
| 상품 정보 수정 | closeDate | 임시저장 경매 상품이면 선택, 전달 시 현재 시각 이후. 직거래 상품이면 생략 가능 |
| 판매 상태 변경 | tradeStatus | 필수, `ON_SALE`, `RESERVED`, `SOLD_OUT` |
| 판매 상태 변경 | buyerId | 직거래 상품을 `SOLD_OUT`으로 변경할 때 필수 |
| 입찰하기 | bidPrice | 필수, 0 이상 정수, 현재 입찰가 초과 |
| 상품 문의 작성 | title | 필수, 1~200자 |
| 상품 문의 작성 | contents | 필수, 1~2000자 |
| 상품 문의 수정 | title | 선택, 전달 시 1~200자 |
| 상품 문의 수정 | contents | 필수, 1~2000자 |
| 상품 문의 답변 등록 | title | 필수, 1~200자 |
| 상품 문의 답변 등록·수정 | contents | 필수, 1~2000자 |
| 채팅 메시지 전송 | content | 필수, 1~1000자 |
| 리뷰 작성 | rating | 필수, 1~5 정수 |
| 리뷰 작성 | content | 선택, 최대 500자 |
| 리뷰 수정 | rating | 선택, 전달 시 1~5 정수 |
| 리뷰 수정 | content | 선택, 최대 500자 |

### 인증

| 기능 | Method | Path | 인증 | 성공 |
|---|---:|---|---|---|
| 회원가입 | POST | `/api/auth/signup` | 불필요 | `201 Created` |
| 로그인 | POST | `/api/auth/login` | 불필요 | `200 OK` |
| 토큰 재발급 | POST | `/api/auth/refresh` | 불필요 | `200 OK` |
| 로그아웃 | POST | `/api/auth/logout` | 필요 | `200 OK` |

- 로그인 성공 시 Access Token은 응답 헤더로 전달하고 Refresh Token은 `Set-Cookie`로 전달한다.
- 인증 API는 `Authorization: Bearer {accessToken}` 헤더를 사용한다.
- 토큰 재발급은 `refresh_token` Cookie를 사용하며 요청 본문에 Refresh Token을 받지 않는다.
- 로그아웃 성공 시 서버는 Access Token을 Redis 블랙리스트에 등록하고 Refresh Token 만료 쿠키를 응답한다.
- 정지 계정은 로그인과 토큰 재발급이 차단된다.
- 이 PR은 관리자 백오피스의 회원 상태 변경 API를 포함하지 않는다.
- 기존 Access Token까지 즉시 차단해야 하면 DB 상태를 `SUSPENDED`로 변경한 뒤 Redis에 `auth:suspended-client:{clientId}` 회원 PK 차단 마커를 직접 등록한다.
- 정지 계정의 기존 Access Token으로 인증을 시도하면 서버는 회원 PK 기반 차단 마커를 확인해 요청을 거부하고, 해당 토큰의 `jti`를 Redis 블랙리스트에 등록한다.
- 정지 계정 PK 마커는 Access Token 만료 시간까지 유지하고, 계정 활성화 시 운영 명령으로 명시적으로 삭제한다.
- Redis의 블랙리스트 토큰과 회원 PK 차단 마커는 TTL 만료 시 자동 삭제된다.
- Refresh Token은 서버 저장소나 블랙리스트로 별도 관리하지 않으므로, 로그아웃은 현재 클라이언트 기준으로 처리된다.
- Refresh Token Cookie를 사용하는 요청은 `XSRF-TOKEN` Cookie 값을 `X-XSRF-TOKEN` Header로 전달한다.

주요 오류:

| 기능 | Status | Code                    | 설명                    |
|---|---:|-------------------------|-----------------------|
| 회원가입 | 400 | `VALIDATION_ERROR`      | 회원가입 요청값 검증 실패        |
| 회원가입 | 409 | `DUPLICATED_EMAIL`      | 이미 사용 중인 이메일          |
| 로그인 | 400 | `INVALID_INPUT`         | 이메일·비밀번호 형식 검증 실패     |
| 로그인 | 401 | `LOGIN_FAILED`          | 이메일 또는 비밀번호 불일치       |
| 로그인 | 403 | `SUSPENDED_ACCOUNT`            | 정지 회원 로그인 차단          |
| 토큰 재발급 | 401 | `INVALID_REFRESH_TOKEN` | Refresh Token 유효하지 않음 |
| 토큰 재발급 | 401 | `REFRESH_TOKEN_EXPIRED` | Refresh Token 만료      |
| 인증 필요 API | 403 | `SUSPENDED_ACCOUNT`     | 정지 회원의 기존 Access Token 인증 차단 |
| 로그아웃 | 401 | `UNAUTHORIZED`          | 인증 토큰 없음·만료           |

### 마이 페이지

| 기능 | Method | Path | 인증 | 요청 | 성공 |
|---|---:|---|---|---|---|
| 내 정보 조회 | GET | `/api/clients/me` | 필요 | 없음 | `200 OK` |
| 내 정보 수정 | PATCH | `/api/clients/me` | 필요 | `nickname`, `name`, `phone`, `profileImageUrl` 선택 | `200 OK` |
| 회원 프로필 조회 | GET | `/api/clients/{clientId}` | 불필요 | Path `clientId` | `200 OK` |
| 내 판매글 목록 | GET | `/api/clients/me/items` | 필요 | 페이징 | `200 OK` |
| 내 관심목록 조회 | GET | `/api/clients/me/likes` | 필요 | 페이징 | `200 OK` |

### 내 관심목록 조회

`GET /api/clients/me/likes`는 로그인 회원이 좋아요한 공개 상품 목록을 페이징으로 조회한다.

요청 Query:

| 필드 | 규칙 |
|---|---|
| page | 선택, 0부터 시작 |
| size | 선택 |
| sort | 선택, 기본값 `likedAt,desc` |

응답 `data.content` 항목:

| 필드 | 설명 |
|---|---|
| itemId | 상품 ID |
| title | 상품 제목 |
| initialPrice | 시작가 |
| currentBid | 경매 상품의 현재 입찰가, 직거래 상품이면 null |
| tradeStatus | 판매 상태 |
| closeDate | 경매 마감 일시, 직거래 상품이면 null |
| tradeType | 거래 유형, `DIRECT` 또는 `AUCTION` |
| conditionType | 상품 상태, `NEW` 또는 `USED` |
| likeCount | 좋아요 수 |
| likedByMe | 현재 로그인 회원의 좋아요 여부, 관심목록에서는 항상 `true` |
| thumbnailUrl | 대표 이미지 URL, 없으면 null |
| categoryId | 카테고리 ID |
| createdAt | 상품 생성 일시 |

임시저장 상품과 삭제된 상품은 관심목록에 노출하지 않는다.

### 회원 프로필 조회

`GET /api/clients/{clientId}`는 비회원과 회원 모두 특정 회원의 공개 프로필을 조회한다.

응답 `data` 필드:

| 필드 | 설명 |
|---|---|
| clientId | 회원 ID |
| nickname | 공개 닉네임 |
| profileImageUrl | 프로필 이미지 URL |
| averageRating | 받은 리뷰 평균 평점 |
| reviewCount | 삭제되지 않은 받은 리뷰 수 |
| followerCount | 팔로워 수 |
| isFollowing | 현재 로그인 사용자의 팔로우 여부, 비회원이면 `false` |
| sellingItemCount | 공개 판매중 상품 수 (`ON_SALE`, 임시저장 제외) |
| soldItemCount | 거래완료 상품 수 (`SOLD_OUT`, 임시저장 제외) |

민감정보인 `email`, `password`, `phone`과 내부 상태값은 공개 프로필 응답에 포함하지 않는다.

### 상품 게시글

| 기능 | Method | Path | 인증 | 요청 | 성공 |
|---|---:|---|---|---|---|
| 카테고리 목록 조회 | GET | `/api/categories` | 불필요 | 없음 | `200 OK` |
| 상품 등록 | POST | `/api/items` | 필요 | 상품 필수 필드 | `201 Created` |
| 상품 임시저장 | POST | `/api/items/drafts` | 필요 | 상품 필수 필드, `closeDate` 선택 | `201 Created` |
| 상품 임시저장 게시 | POST | `/api/items/{itemId}/publish` | 필요 | Path `itemId` | `200 OK` |
| 상품 목록 조회 | GET | `/api/items` | 불필요 | `categoryId`, `tradeStatus`, `page`, `size` 선택 | `200 OK` |
| 상품 상세 조회 | GET | `/api/items/{itemId}` | 불필요 | Path `itemId` | `200 OK` |
| 상품 검색 v1 | GET | `/api/v1/items/search` | 불필요 | `keyword`, `categoryId`, `tradeStatus`, `page`, `size`, `sort` 선택 | `200 OK` |
| 상품 정보 수정 | PUT | `/api/items/{itemId}` | 필요 | 수정할 상품 필드 | `200 OK` |
| 판매 상태 변경 | PATCH | `/api/items/{itemId}/status` | 필요 | `tradeStatus`, 직거래 완료 시 `buyerId` | `200 OK` |
| 상품 삭제 | DELETE | `/api/items/{itemId}` | 필요 | Path `itemId` | `204 No Content` |

상품 등록 요청 예시:

```json
{
  "categoryId": 2,
  "title": "아이폰 14 프로 S급",
  "tradeType": "AUCTION",
  "conditionType": "USED",
  "description": "풀박스 상태 매우 좋습니다.",
  "initialPrice": 800000,
  "closeDate": "2026-06-30T23:59:59"
}
```

상품 정보 수정 요청 예시:

```json
{
  "categoryId": 2,
  "title": "아이폰 14 프로 S급",
  "description": "풀박스 상태 매우 좋습니다.",
  "initialPrice": 800000,
  "closeDate": "2026-08-05T15:30:00"
}
```

임시저장 상품과 등록된 직거래 상품만 수정할 수 있다. 등록된 경매 상품은 수정할 수 없다. 직거래 상품은 `closeDate`를 생략할 수 있고, 임시저장 경매 상품은 `closeDate` 전달 시 경매 종료일을 함께 수정한다. 임시저장 경매 상품의 `initialPrice` 변경 시 `auction_status.current_bid`도 함께 변경된다.

상품 정보 수정 성공 응답:

```json
{
  "status": 200,
  "data": {
    "itemId": 1,
    "updatedAt": "2026-06-29T13:30:00"
  }
}
```

상품 정보 수정 오류:

| Status | Code | 설명 |
|---:|---|---|
| 400 | `VALIDATION_ERROR` | 요청값 누락 또는 형식 오류 |
| 400 | `ITEM_UPDATE_NOT_ALLOWED` | 등록된 경매 상품 수정 요청 |
| 400 | `AUCTION_ALREADY_IN_PROGRESS` | 입찰자가 있는 임시저장 경매 상품의 시작가 또는 종료일 변경 요청 |
| 401 | `UNAUTHORIZED` | 미인증 사용자 |
| 403 | `FORBIDDEN` | 상품 판매자가 아닌 사용자 |
| 404 | `ITEM_NOT_FOUND` | 존재하지 않거나 삭제된 상품 |
| 404 | `CATEGORY_NOT_FOUND` | 존재하지 않는 카테고리 |
| 404 | `AUCTION_STATUS_NOT_FOUND` | 경매 상품의 경매 상태 정보 없음 |

판매 상태 변경 요청 예시:

```json
{
  "tradeStatus": "RESERVED"
}
```

직거래 판매완료 요청 예시:

```json
{
  "tradeStatus": "SOLD_OUT",
  "buyerId": 2
}
```

판매 상태 변경은 판매자 본인의 등록된 상품에만 가능하다. 임시저장 상품의 판매 상태는 변경할 수 없다. 직거래 상품을 `SOLD_OUT`으로 변경할 때는 실제 구매자 `buyerId`를 함께 저장한다.

판매 상태 변경 성공 응답:

```json
{
  "status": 200,
  "data": {
    "itemId": 1,
    "tradeStatus": "RESERVED",
    "buyerId": null,
    "updatedAt": "2026-06-29T13:30:00"
  }
}
```

판매 상태 변경 오류:

| Status | Code | 설명 |
|---:|---|---|
| 400 | `VALIDATION_ERROR` | 요청값 누락 또는 형식 오류 |
| 400 | `INVALID_INPUT` | 정의되지 않은 `tradeStatus` 요청 또는 직거래 판매완료 구매자 누락 |
| 400 | `ITEM_STATUS_UPDATE_NOT_ALLOWED` | 임시저장 상품 판매 상태 변경 요청 |
| 401 | `UNAUTHORIZED` | 미인증 사용자 |
| 403 | `FORBIDDEN` | 상품 판매자가 아닌 사용자 |
| 404 | `ITEM_NOT_FOUND` | 존재하지 않거나 삭제된 상품 |
| 404 | `CLIENT_NOT_FOUND` | 구매자 회원 없음 |

상품 임시저장 게시는 판매자 본인의 `isDraft = true` 상품을 등록 상태(`isDraft = false`)로 전환한다. 요청 본문은 없다. 직거래 상품은 임시저장된 상품 필수 정보가 유효해야 하며, 경매 상품은 현재 시각 이후의 `closeDate`를 가진 `auction_status`가 준비되어 있어야 한다.

상품 임시저장 게시 성공 응답:

```json
{
  "status": 200,
  "data": {
    "itemId": 1,
    "updatedAt": "2026-06-29T13:30:00"
  }
}
```

상품 임시저장 게시 오류:

| Status | Code | 설명 |
|---:|---|---|
| 400 | `ITEM_PUBLISH_NOT_ALLOWED` | 이미 등록된 상품이거나 게시 조건을 만족하지 않는 상품 |
| 400 | `AUCTION_ALREADY_CLOSED` | 경매 종료일이 현재 시각 이전인 경매 상품 게시 요청 |
| 401 | `UNAUTHORIZED` | 미인증 사용자 |
| 403 | `FORBIDDEN` | 상품 판매자가 아닌 사용자 |
| 404 | `ITEM_NOT_FOUND` | 존재하지 않거나 삭제된 상품 |
| 404 | `AUCTION_STATUS_NOT_FOUND` | 경매 상품의 경매 상태 정보 없음 |

상품 삭제는 판매자 본인만 요청할 수 있다. 임시저장 상품은 Hard Delete로 실제 row를 삭제하고, 게시된 상품은 `is_deleted = true`로 Soft Delete 처리한다. 삭제된 상품은 목록 조회와 상세 조회에 노출하지 않는다.

상품 삭제 성공 응답:

`204 No Content`

상품 삭제 오류:

| Status | Code | 설명 |
|---:|---|---|
| 401 | `UNAUTHORIZED` | 미인증 사용자 |
| 403 | `FORBIDDEN` | 상품 판매자가 아닌 사용자 |
| 404 | `ITEM_NOT_FOUND` | 존재하지 않거나 이미 삭제된 상품 |

### 상품 이미지·좋아요·경매

| 기능 | Method | Path | 인증 | 요청 | 성공 |
|---|---:|---|---|---|---|
| 대표 이미지 설정 | PATCH | `/api/items/{itemId}/images/{imageId}/thumbnail` | 필요 | Path `itemId`, `imageId` | `200 OK` |
| 상품 이미지 삭제 | DELETE | `/api/items/{itemId}/images/{imageId}` | 필요 | Path `itemId`, `imageId` | `204 No Content` |
| 상품 좋아요 토글 | POST | `/api/items/{itemId}/likes` | 필요 | Path `itemId` | `200 OK` |
| 입찰하기 | POST | `/api/items/{itemId}/auction-status/bid` | 필요 | `bidPrice` | `200 OK` |

상품 좋아요 토글 성공 응답 `data`: `itemId`, `liked`, `likeCount`

상품 좋아요 토글 오류 응답:

| Status | Code | 설명 |
|---:|---|---|
| 401 | `UNAUTHORIZED` | 미인증 사용자 |
| 404 | `ITEM_NOT_FOUND` | 존재하지 않거나 이미 삭제된 상품 |

입찰하기 성공 응답 `data`: `itemId`, `currentBid`, `closeDate`

입찰하기 오류 응답:

| Status | Code | 설명 |
|---:|---|---|
| 400 | `INVALID_BID_REQUEST` | 판매자 본인 상품 입찰 요청 |
| 400 | `INVALID_BID_PRICE` | 현재 최고 입찰가 이하 입찰 요청 |
| 400 | `AUCTION_ALREADY_CLOSED` | 마감된 경매 입찰 요청 |
| 404 | `ITEM_NOT_FOUND`, `AUCTION_STATUS_NOT_FOUND` | 상품 또는 경매 상태 정보 없음 |
| 409 | `AUCTION_BID_LOCK_FAILED` | 같은 상품에 입찰 요청이 몰려 락 획득 실패 |

### 문의

| 기능 | Method | Path | 인증 | 요청 | 성공 |
|---|---:|---|---|---|---|
| 상품 문의 작성 | POST | `/api/items/{itemId}/inquiries` | 필요 | `title`, `contents` | `201 Created` |
| 상품 문의 목록 조회 | GET | `/api/items/{itemId}/inquiries` | 불필요 | Path `itemId`, Query `page`, `size` 선택 | `200 OK` |
| 상품 문의 수정 | PUT | `/api/inquiries/{inquiryId}` | 필요 | `title` 선택, `contents` | `200 OK` |
| 상품 문의 삭제 | DELETE | `/api/inquiries/{inquiryId}` | 필요 | Path `inquiryId` | `204 No Content` |
| 상품 문의 답변 등록 | POST | `/api/inquiries/{inquiryId}/answer` | 필요 | `title`, `contents` | `201 Created` |
| 상품 문의 답변 수정 | PATCH | `/api/inquiries/{inquiryId}/answer` | 필요 | `contents` | `200 OK` |
| 상품 문의 답변 삭제 | DELETE | `/api/inquiries/{inquiryId}/answer` | 필요 | Path `inquiryId` | `204 No Content` |

문의 오류 기준:

| Status | Code | 설명 |
|---:|---|---|
| 400 | `VALIDATION_ERROR` | 입력값 누락 또는 형식 오류 |
| 401 | `UNAUTHORIZED` | 미인증 사용자 |
| 403 | `FORBIDDEN` | 작성자 또는 판매자가 아닌 사용자 |
| 404 | `ITEM_NOT_FOUND`, `INQUIRY_NOT_FOUND` | 대상 상품 또는 문의 없음 |
| 409 | `ANSWER_ALREADY_EXISTS` | 이미 답변이 존재함 |

상품 문의 목록 조회 응답은 `data.itemList`에 문의 항목을 담고, 각 항목은 `inquiryID`, `authorName`, `contents`, `date`를 포함한다.
페이지 메타데이터는 `page`, `size`, `totalElements`, `totalPages`로 응답한다.
`page` 기본값은 0, `size` 기본값은 20이며 잘못된 쿼리 값은 `400 Bad Request`로 응답한다.
상품 문의 삭제 성공 응답은 `data: null`을 반환하며, 삭제는 soft delete로 처리한다.
상품 문의 수정 응답은 `id`, `authorName`, `contents`, `date`를 포함하며 `date`는 수정 시각이다.

### 팔로우·리뷰

| 기능 | Method | Path | 인증 | 요청 | 성공 |
|---|---:|---|---|---|---|
| 회원 팔로우 | POST | `/api/clients/{clientId}/follows` | 필요 | Path `clientId` | `201 Created` |
| 회원 팔로우 취소 | DELETE | `/api/clients/{clientId}/follows` | 필요 | Path `clientId` | `204 No Content` |
| 내가 팔로우한 회원 목록 | GET | `/api/clients/me/followings` | 필요 | 페이징 | `200 OK` |
| 나를 팔로우한 회원 목록 | GET | `/api/clients/me/followers` | 필요 | 페이징 | `200 OK` |
| 상품 리뷰 작성 | POST | `/api/items/{itemId}/reviews` | 필요 | Path `itemId`, `rating`, `content` | `201 Created` |
| 받은 리뷰 목록 조회 | GET | `/api/clients/{clientId}/reviews` | 불필요 | Path `clientId` | `200 OK` |
| 내가 작성한 리뷰 목록 | GET | `/api/clients/me/reviews/written` | 필요 | 페이징 | `200 OK` |
| 리뷰 수정 | PATCH | `/api/reviews/{reviewId}` | 필요 | `rating`, `content` 선택 | `200 OK` |
| 리뷰 삭제 | DELETE | `/api/reviews/{reviewId}` | 필요 | Path `reviewId` | `204 No Content` |

리뷰 오류 기준:

| Status | Code | 설명 |
|---:|---|---|
| 400 | `VALIDATION_ERROR`, `REVIEW_ITEM_NOT_COMPLETED` | 요청값 검증 실패 또는 거래완료 전 리뷰 작성 |
| 401 | `UNAUTHORIZED` | 인증 실패 |
| 403 | `FORBIDDEN`, `REVIEW_NOT_ALLOWED`, `SELF_REVIEW_NOT_ALLOWED` | 작성자가 아니거나 구매자가 아니거나 본인 상품 리뷰 작성 |
| 404 | `ITEM_NOT_FOUND`, `CLIENT_NOT_FOUND`, `REVIEW_NOT_FOUND` | 상품, 대상 회원 또는 리뷰 없음 |
| 409 | `REVIEW_ALREADY_EXISTS` | 동일 상품 중복 리뷰 |

### 채팅

| 기능 | Method | Path | 인증 | 요청 | 성공 |
|---|---:|---|---|---|---|
| 채팅방 생성 | POST | `/api/items/{itemId}/chat-rooms` | 필요 | Path `itemId` | `201 Created` |
| 채팅방 조회 | GET | `/api/chat-rooms` | 필요 | 페이징 | `200 OK` |
| 채팅방 나가기 | POST | `/api/chat-rooms/{chatRoomId}/leave` | 필요 | Path `chatRoomId` | `200 OK` |
| 메시지 목록 조회 | GET | `/api/chat-rooms/{chatRoomId}/messages` | 필요 | Path `chatRoomId`, 페이징 | `200 OK` |
| 메시지 전송 | WS | `/api/chat-rooms/{chatRoomId}/messages` | 필요 | `content` | 브로드캐스트 |
| 메시지 삭제 | DELETE | `/api/chat-messages/{messageId}` | 필요 | Path `messageId` | `204 No Content` |
| 메시지 읽음 처리 | POST | `/api/chat-rooms/{chatRoomId}/read` | 필요 | Path `chatRoomId` | `200 OK` |

메시지 전송은 `/ws/chat` 연결 후 STOMP `SEND /api/chat-rooms/{chatRoomId}/messages`로 처리한다.

## 인증 응답

### 회원가입

`POST /api/auth/signup`은 이메일 중복을 검증하고 비밀번호를 해시로 저장한다.

요청:

```json
{
  "email": "client@example.com",
  "password": "password123!",
  "nickname": "배추판매자",
  "name": "홍길동",
  "phone": "010-1234-5678"
}
```

요청 검증:

| 필드 | 규칙 |
|---|---|
| email | 필수, 공백 불가, 이메일 형식, 최대 254자 |
| password | 필수, 공백 불가, 8~64자, 영문·숫자·특수문자 각 1자 이상 |
| nickname | 필수, 공백 불가, 2~20자 |
| name | 필수, 공백 불가, 1~50자 |
| phone | 필수, 형식 `01[0-9]-?\d{3,4}-?\d{4}` |

성공 응답은 `201 Created`를 사용하고 password, phone은 포함하지 않는다.

```json
{
  "status": 201,
  "data": {
    "clientId": 1,
    "email": "client@example.com",
    "nickname": "배추판매자",
    "createdAt": "2026-06-24T03:00:00"
  }
}
```

중복 이메일은 `409 Conflict`, 입력 검증 실패는 `400 Bad Request`로 응답한다.
- 회원가입 요청은 프로필 이미지 파일을 직접 받지 않는다.
- 서버는 기본 프로필 이미지 URL을 `profileImageUrl`에 저장해 초기 프로필 이미지를 설정한다.

### 내 정보 수정

`PATCH /api/clients/me`는 회원 기본 정보와 프로필 이미지 URL을 수정한다.

- `profileImageUrl`은 업로드된 파일 자체가 아니라 접근 가능한 이미지 URL이다.
- 프로필 이미지 업로드는 별도 API 범위로 다루고, 내 정보 수정 API는 전달받은 URL로 회원 프로필 이미지를 교체한다.

### 로그인

`POST /api/auth/login` 성공 응답은 Access Token을 응답 헤더로 전달하고 Refresh Token을 `Set-Cookie`로 전달한다.

```http
Authorization: Bearer {accessToken}
```

```http
Set-Cookie: refresh_token={jwt}; HttpOnly; Secure; SameSite=Lax; Path=/; Max-Age=1209600
```

운영 환경은 `Secure` 속성을 `true`로 사용하고, 로컬 환경(`local`)은 HTTP 테스트를 위해 `false`로 사용한다.
Redis 연동 테스트는 로컬 Redis가 아니라 테스트 프로세스가 띄우는 embedded Redis를 사용한다.

### 토큰 재발급

`POST /api/auth/refresh`는 `refresh_token` Cookie를 검증하고 새 Access Token은 응답 헤더로, 새 Refresh Token은 `Set-Cookie`로 재발급한다.
요청 본문에는 Refresh Token을 받지 않는다.

```http
Authorization: Bearer {accessToken}
```

```http
Cookie: refresh_token={jwt}
```

### 로그아웃

`POST /api/auth/logout`은 Access Token을 Redis 블랙리스트에 등록하고 Refresh Token 만료 쿠키를 응답한다.

### 정지 계정 토큰 차단

이 PR은 관리자 백오피스의 회원 상태 변경 API를 포함하지 않는다.
운영·테스트에서 회원 상태를 `SUSPENDED`로 직접 변경하면 로그인과 Refresh Token 재발급은 DB 상태 조회로 차단된다.
기존 Access Token까지 즉시 차단해야 하면 Redis에 `auth:suspended-client:{clientId}` 회원 PK 차단 마커를 직접 등록한다.
해당 회원의 기존 Access Token으로 인증 요청이 들어오면 서버는 요청을 `SUSPENDED_ACCOUNT`로 거부하고, 그 토큰의 `jti`를 Redis 블랙리스트에 등록한다.
회원 PK 마커는 Access Token 만료 시간까지 유지하며, 계정 활성화 시 운영 명령으로 명시적으로 삭제한다.
Redis 데이터는 TTL 만료 시 자동 삭제된다.

## 상품 게시글

### 상품 등록

`POST /api/items`는 인증된 회원이 판매 상품을 등록한다.

요청:

```json
{
  "categoryId": 1,
  "tradeType": "SALE",
  "title": "싱싱한 배추",
  "description": "오늘 수확한 배추입니다.",
  "initialPrice": 12000,
  "conditionType": "NEW",
  "closeDate": "2026-06-30T23:59:59"
}
```

요청 검증:

| 필드 | 규칙 |
|---|---|
| categoryId | 필수, 존재하는 카테고리 ID |
| tradeType | 필수, `SALE` 또는 `AUCTION` |
| title | 필수, 공백 불가, 1~100자 |
| description | 필수, 공백 불가, 1~2000자 |
| initialPrice | 필수, 0 이상 정수 |
| conditionType | 필수, `NEW` 또는 `USED` |
| closeDate | `AUCTION`일 때 필수, 현재 시각 이후 |

성공 응답은 `201 Created`를 사용한다.

```json
{
  "id": 1,
  "sellerId": 1,
  "categoryId": 1,
  "tradeType": "SALE",
  "title": "싱싱한 배추",
  "description": "오늘 수확한 배추입니다.",
  "initialPrice": 12000,
  "conditionType": "NEW",
  "tradeStatus": "ON_SALE",
  "viewCount": 0,
  "likeCount": 0,
  "inquiryCount": 0,
  "isDraft": false,
  "createdAt": "2026-06-24T04:00:00"
}
```

존재하지 않는 카테고리는 `404 Not Found`, 입력 검증 실패는 `400 Bad Request`로 응답한다.

### 상품 목록 조회

`GET /api/items`는 공개 상품 목록을 페이징으로 조회한다.

요청 Query:

| 필드 | 규칙 |
|---|---|
| categoryId | 선택, 해당 카테고리 상품만 조회 |
| tradeStatus | 선택, `ON_SALE`, `RESERVED`, `SOLD_OUT` |
| page | 선택, 0부터 시작 |
| size | 선택 |

응답 `data.content` 항목:

| 필드 | 설명 |
|---|---|
| itemId | 상품 ID |
| title | 상품 제목 |
| initialPrice | 시작가 |
| currentBid | 현재 입찰가, 경매 상태가 없으면 null |
| tradeStatus | 판매 상태 |
| closeDate | 경매 마감 일시, 경매 상태가 없으면 null |
| likeCount | 좋아요 수 |

### 상품 검색 v1

`GET /api/v1/items/search`는 캐시가 적용되지 않은 상품 검색 결과를 페이징으로 조회한다.

요청 Query:

| 필드 | 규칙 |
|---|---|
| keyword | 선택, 최대 100자. 상품 제목 또는 설명에 대해 `LIKE` 검색 |
| categoryId | 선택, 해당 카테고리 상품만 검색 |
| tradeStatus | 선택, `ON_SALE`, `RESERVED`, `SOLD_OUT` |
| tradeType | 선택, `DIRECT`, `AUCTION` |
| conditionType | 선택, `NEW`, `USED` |
| minPrice | 선택, 0 이상 |
| maxPrice | 선택, 0 이상. `minPrice`와 함께 전달 시 `minPrice` 이상이어야 함 |
| page | 선택, 0부터 시작 |
| size | 선택 |
| sort | 선택, `createdAt,desc`(최신순), `initialPrice,asc`(낮은 가격순), `initialPrice,desc`(높은 가격순), `currentBid,desc`(현재 입찰가순) |

응답 `data.content` 항목:

| 필드 | 설명 |
|---|---|
| itemId | 상품 ID |
| thumbnailUrl | 대표 이미지 URL, 없으면 null |
| categoryName | 카테고리명 |
| title | 상품 제목 |
| tradeType | 거래 유형, `DIRECT` 또는 `AUCTION` |
| initialPrice | 시작가 |
| currentBid | 경매 상품의 현재 입찰가, 경매 상태가 없으면 null |
| tradeStatus | 판매 상태 |
| likeCount | 좋아요 수 |
| createdAt | 상품 생성 일시 |

삭제된 상품과 임시저장 상품은 검색 결과에 노출하지 않는다.
`keyword`가 없거나 공백이면 키워드 조건 없이 전체 상품을 페이징 조회한다.
`currentBid,desc` 정렬은 경매 상품만 대상으로 한다.

## 열린 결정

- 이미지 업로드 API 분리, 인기 검색어 집계 저장소는 [docs/adr/README.md](adr/README.md)에서 관리한다.
