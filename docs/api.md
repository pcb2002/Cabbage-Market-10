# API

현재 문서는 **현재 코드에 구현된 API만** 정리한다.

## 기본 규칙

- REST API prefix는 `/api`다.
- 성공 응답은 `CommonResponse.success(...)` 형식을 사용한다.
- 실패 응답은 `status`, `code`, `message`, `data:null` 형식을 사용한다.
- 목록 응답은 `PageResponse` 또는 전용 목록 DTO를 사용한다.
- Entity를 응답으로 직접 반환하지 않는다.

## 공통 응답 형식

성공:

```json
{
  "status": 200,
  "data": {}
}
```

실패:

```json
{
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "요청값 검증에 실패했습니다.",
  "data": null
}
```

페이지 응답:

```json
{
  "status": 200,
  "data": {
    "content": [],
    "page": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0
  }
}
```

## 구현된 API 목록

| 그룹 | Method | Path | 인증 |
|---|---:|---|---|
| 인증 | POST | `/api/auth/signup` | 불필요 |
| 인증 | POST | `/api/auth/login` | 불필요 |
| 인증 | POST | `/api/auth/refresh` | 불필요 |
| 인증 | POST | `/api/auth/logout` | 필요 |
| 회원 | GET | `/api/clients/me` | 필요 |
| 회원 | PATCH | `/api/clients/me` | 필요 |
| 회원 | GET | `/api/clients/me/items` | 필요 |
| 회원 | GET | `/api/clients/me/likes` | 필요 |
| 회원 | GET | `/api/clients/{clientId}` | 선택 |
| 카테고리 | GET | `/api/categories` | 불필요 |
| 상품 | POST | `/api/items` | 필요 |
| 상품 | POST | `/api/items/drafts` | 필요 |
| 상품 | POST | `/api/items/{itemId}/publish` | 필요 |
| 상품 | GET | `/api/items` | 불필요 |
| 상품 | GET | `/api/items/{itemId}` | 불필요 |
| 상품 | POST | `/api/items/{itemId}/likes` | 필요 |
| 상품 | PUT | `/api/items/{itemId}` | 필요 |
| 상품 | PATCH | `/api/items/{itemId}/status` | 필요 |
| 상품 | DELETE | `/api/items/{itemId}` | 필요 |
| 경매 | POST | `/api/items/{itemId}/auction-status/bid` | 필요 |
| 상품 이미지 | POST | `/api/items/{itemId}/images` | 필요 |
| 상품 이미지 | PATCH | `/api/items/{itemId}/images/{imageId}/thumbnail` | 필요 |
| 상품 이미지 | DELETE | `/api/items/{itemId}/images/{imageId}` | 필요 |
| 문의 | GET | `/api/items/{itemId}/inquiries` | 불필요 |
| 문의 | POST | `/api/items/{itemId}/inquiries` | 필요 |
| 문의 | PUT | `/api/inquiries/{inquiryId}` | 필요 |
| 문의 | DELETE | `/api/inquiries/{inquiryId}` | 필요 |
| 문의 답변 | POST | `/api/inquiries/{inquiryId}/answer` | 필요 |
| 리뷰 | POST | `/api/items/{itemId}/reviews` | 필요 |
| 리뷰 | GET | `/api/clients/{clientId}/reviews` | 불필요 |
| 리뷰 | GET | `/api/clients/me/reviews/written` | 필요 |
| 채팅 | POST | `/api/chat-rooms/{itemId}` | 필요 |
| 채팅 | GET | `/api/chat-rooms/{chatRoomId}/messages` | 필요 |
| 채팅 | GET | `/api/chat-rooms/my` | 필요 |
| 채팅 | DELETE | `/api/chat-rooms/{messageId}` | 필요 |
| 검색 | GET | `/api/v1/items/search` | 선택 |
| 검색 | GET | `/api/v2/items/search` | 선택 |
| 검색 | GET | `/api/search/popular` | 불필요 |

## 구현되지 않은 항목

현재 코드에는 아래 기능이 없다.

- 팔로우 API
- 리뷰 수정 API
- 리뷰 삭제 API
- 문의 답변 수정 API
- 문의 답변 삭제 API
- 채팅방 나가기 API
- 메시지 읽음 처리 API

## WebSocket

| 기능 | 경로 |
|---|---|
| HTTP Handshake | `/ws/chat` |
| STOMP Publish Prefix | `/pub` |
| STOMP Subscribe Prefix | `/sub` |
| 메시지 전송 | `SEND /pub/{roomId}/messages` |
| 메시지 구독 | `SUBSCRIBE /sub/{roomId}/messages` |

주의:

- HTTP handshake 자체는 `permitAll`이다.
- 실제 인증은 STOMP `CONNECT` 시 `Authorization: Bearer {accessToken}` 헤더로 검증한다.

## 현재 인증 기준

공개:

- `POST /api/auth/signup`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `GET /api/categories/**`
- `GET /api/items/**`
- `GET /api/search/popular`
- `GET /api/v1/items/search`
- `GET /api/v2/items/search`
- `GET /api/clients/{clientId}`
- `GET /api/clients/{clientId}/reviews`
- `/ws/chat`, `/ws/chat/**`

인증 필요:

- 그 외 모든 `/api/**`

## 주요 요청 검증

### 인증

- `SignupRequest.email`: 필수, 이메일 형식, 최대 254자
- `SignupRequest.password`: 필수, 8~64자, 영문/숫자/특수문자 포함
- `SignupRequest.nickname`: 필수, 2~20자
- `SignupRequest.name`: 필수, 최대 50자
- `SignupRequest.phone`: 필수, 휴대폰 형식
- `LoginRequest.email`: 필수, 이메일 형식
- `LoginRequest.password`: 필수

### 회원

- `ClientMyInfoUpdateRequest.nickname`: 선택, 전달 시 공백 불가, 2~20자
- `ClientMyInfoUpdateRequest.name`: 선택, 전달 시 공백 불가, 최대 50자
- `ClientMyInfoUpdateRequest.phone`: 선택, 휴대폰 형식
- `ClientMyInfoUpdateRequest.profileImageUrl`: 선택, `http/https` URL, 최대 500자

### 상품

- `ItemCreateRequest.categoryId`: 필수
- `ItemCreateRequest.title`: 필수
- `ItemCreateRequest.tradeType`: 필수
- `ItemCreateRequest.conditionType`: 필수
- `ItemCreateRequest.description`: 필수
- `ItemCreateRequest.initialPrice`: 필수
- `ItemCreateRequest.closeDate`: 필수

- `ItemDraftRequest.closeDate`: 선택
- `ItemUpdateRequest.initialPrice`: 필수, 0 이상
- `ItemUpdateRequest.closeDate`: 선택, 전달 시 미래 시각
- `ItemStatusUpdateRequest.tradeStatus`: 필수
- `ItemBidRequest.bidPrice`: 필수, 0 이상

### 문의 / 리뷰 / 채팅 / 검색

- 문의 생성/답변 생성: `title` 필수, 최대 200자 / `contents` 필수, 최대 2000자
- 문의 수정: `title` 선택, 공백 불가 / `contents` 필수
- 리뷰 작성: `rating` 필수, 1~5 / `content` 선택, 최대 500자
- 채팅 메시지: `content` 필수 / `contentType` 필수
- 검색어: `keyword` 최대 100자
- 검색 가격 범위: `minPrice`, `maxPrice`는 0 이상, 둘 다 있으면 `minPrice <= maxPrice`
- `likedOnly=true`는 로그인 회원만 허용

## 현재 페이지네이션 방식

- 일반 목록 API는 `Pageable` 또는 `page`, `size` 기반 offset 페이지네이션이다.
- 채팅 메시지 조회 `GET /api/chat-rooms/{chatRoomId}/messages`도 offset 기반이다.
- 채팅 메시지는 `createdAt DESC`로 정렬한다.
- 채팅방 목록 `GET /api/chat-rooms/my`는 `lastMessageAt DESC`로 정렬한다.

## 현재 구현 기준 주요 응답 포인트

- 로그인/토큰 재발급 성공 시 Access Token은 `Authorization` 헤더, Refresh Token은 Cookie로 내려간다.
- 상품 상세 응답은 `sellerId`와 `images` 배열을 포함한다.
- 채팅방 목록 응답은 `id`, `itemId`, `itemName`, `date`를 포함한다.
- 받은 리뷰 목록과 작성한 리뷰 목록은 모두 페이지 응답이다.
