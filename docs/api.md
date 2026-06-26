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
| 상품 목록 조회 | 상품 게시글 | GET | `/api/items` |
| 상품 상세 조회 | 상품 게시글 | GET | `/api/items/{itemId}` |
| 인기 검색어 조회 | 검색어 | GET | `/api/search/popular` |
| 상품 검색 | 검색어 | GET | `/api/items?keyword={keyword}` |
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
| 회원 리뷰 작성 | 리뷰 | POST | `/api/clients/{clientId}/reviews` |
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
| 회원가입, 로그인, 토큰 재발급, 상품 목록·상세, 카테고리, 회원 공개 프로필 | 로그아웃, 내 정보, 상품 등록·수정·삭제, 좋아요, 문의 작성, 팔로우, 채팅, 리뷰, 입찰 |

## Notion DB 상세 명세

Notion `DB` 페이지의 API 명세 데이터베이스를 기준으로 정리한다.

### 요청 검증 요약

| 기능 | 필드 | 규칙 |
|---|---|---|
| 회원가입 | email | 필수, 공백 불가, 이메일 형식, 최대 254자 |
| 회원가입 | password | 필수, 공백 불가, 8~64자, 영문·숫자·특수문자 각 1자 이상 |
| 회원가입 | nickname | 필수, 공백 불가, 2~20자 |
| 회원가입 | name | 필수, 공백 불가, 1~50자 |
| 회원가입 | phone | 선택, 형식 `01[0-9]-?\d{3,4}-?\d{4}` |
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
| 상품 검색 | keyword | 선택, 최대 100자, 공백이면 전체 목록 |
| 판매 상태 변경 | tradeStatus | 필수, `ON_SALE`, `RESERVED`, `SOLD_OUT` |
| 입찰하기 | bidPrice | 필수, 0 이상 정수, 현재 입찰가 초과 |
| 상품 문의 작성 | title | 필수, 1~200자 |
| 상품 문의 작성 | contents | 필수, 1~2000자 |
| 상품 문의 수정 | contents | 필수, 1~2000자 |
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
- 로그아웃 성공 시 서버는 폐기 대상 토큰을 Redis 블랙리스트에 등록하고 만료 쿠키를 응답한다.
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
| 토큰 재발급 | 401 | `BLACKLISTED_TOKEN`     | 로그아웃 또는 강제 만료된 토큰     |
| 로그아웃 | 401 | `UNAUTHORIZED`          | 인증 토큰 없음·만료           |

### 마이 페이지

| 기능 | Method | Path | 인증 | 요청 | 성공 |
|---|---:|---|---|---|---|
| 내 정보 조회 | GET | `/api/clients/me` | 필요 | 없음 | `200 OK` |
| 내 정보 수정 | PATCH | `/api/clients/me` | 필요 | `nickname`, `name`, `phone`, `profileImageUrl` 선택 | `200 OK` |
| 회원 프로필 조회 | GET | `/api/clients/{clientId}` | 불필요 | Path `clientId` | `200 OK` |
| 내 판매글 목록 | GET | `/api/clients/me/items` | 필요 | 페이징 | `200 OK` |
| 내 관심목록 조회 | GET | `/api/clients/me/likes` | 필요 | 페이징 | `200 OK` |

### 상품 게시글

| 기능 | Method | Path | 인증 | 요청 | 성공 |
|---|---:|---|---|---|---|
| 카테고리 목록 조회 | GET | `/api/categories` | 불필요 | 없음 | `200 OK` |
| 상품 등록 | POST | `/api/items` | 필요 | 상품 필수 필드 | `201 Created` |
| 상품 임시저장 | POST | `/api/items/drafts` | 필요 | 상품 필수 필드, `closeDate` 선택 | `201 Created` |
| 상품 목록 조회 | GET | `/api/items` | 불필요 | `page`, `size` 선택 | `200 OK` |
| 상품 상세 조회 | GET | `/api/items/{itemId}` | 불필요 | Path `itemId` | `200 OK` |
| 상품 검색 | GET | `/api/items?keyword={keyword}` | 불필요 | Query `keyword` | `200 OK` |
| 상품 정보 수정 | PUT | `/api/items/{itemId}` | 필요 | 수정할 상품 필드 | `200 OK` |
| 판매 상태 변경 | PATCH | `/api/items/{itemId}/status` | 필요 | `tradeStatus` | `200 OK` |
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

### 상품 이미지·좋아요·경매

| 기능 | Method | Path | 인증 | 요청 | 성공 |
|---|---:|---|---|---|---|
| 대표 이미지 설정 | PATCH | `/api/items/{itemId}/images/{imageId}/thumbnail` | 필요 | Path `itemId`, `imageId` | `200 OK` |
| 상품 이미지 삭제 | DELETE | `/api/items/{itemId}/images/{imageId}` | 필요 | Path `itemId`, `imageId` | `204 No Content` |
| 상품 좋아요 토글 | POST | `/api/items/{itemId}/likes` | 필요 | Path `itemId` | `200 OK` |
| 입찰하기 | POST | `/api/items/{itemId}/auction-status/bid` | 필요 | `bidPrice` | `200 OK` |

### 문의

| 기능 | Method | Path | 인증 | 요청 | 성공 |
|---|---:|---|---|---|---|
| 상품 문의 작성 | POST | `/api/items/{itemId}/inquiries` | 필요 | `title`, `contents` | `201 Created` |
| 상품 문의 목록 조회 | GET | `/api/items/{itemId}/inquiries` | 불필요 | Path `itemId` | `200 OK` |
| 상품 문의 수정 | PATCH | `/api/inquiries/{inquiryId}` | 필요 | `contents` | `200 OK` |
| 상품 문의 삭제 | DELETE | `/api/inquiries/{inquiryId}` | 필요 | Path `inquiryId` | `204 No Content` |
| 상품 문의 답변 등록 | POST | `/api/inquiries/{inquiryId}/answer` | 필요 | `contents` | `201 Created` |
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

### 팔로우·리뷰

| 기능 | Method | Path | 인증 | 요청 | 성공 |
|---|---:|---|---|---|---|
| 회원 팔로우 | POST | `/api/clients/{clientId}/follows` | 필요 | Path `clientId` | `201 Created` |
| 회원 팔로우 취소 | DELETE | `/api/clients/{clientId}/follows` | 필요 | Path `clientId` | `204 No Content` |
| 내가 팔로우한 회원 목록 | GET | `/api/clients/me/followings` | 필요 | 페이징 | `200 OK` |
| 나를 팔로우한 회원 목록 | GET | `/api/clients/me/followers` | 필요 | 페이징 | `200 OK` |
| 회원 리뷰 작성 | POST | `/api/clients/{clientId}/reviews` | 필요 | `rating`, `content` | `201 Created` |
| 받은 리뷰 목록 조회 | GET | `/api/clients/{clientId}/reviews` | 불필요 | Path `clientId` | `200 OK` |
| 내가 작성한 리뷰 목록 | GET | `/api/clients/me/reviews/written` | 필요 | 페이징 | `200 OK` |
| 리뷰 수정 | PATCH | `/api/reviews/{reviewId}` | 필요 | `rating`, `content` 선택 | `200 OK` |
| 리뷰 삭제 | DELETE | `/api/reviews/{reviewId}` | 필요 | Path `reviewId` | `204 No Content` |

리뷰 오류 기준:

| Status | Code | 설명 |
|---:|---|---|
| 400 | `VALIDATION_ERROR` | 요청값 검증 실패 |
| 401 | `UNAUTHORIZED` | 인증 실패 |
| 403 | `FORBIDDEN` | 작성자가 아님 |
| 404 | `CLIENT_NOT_FOUND`, `REVIEW_NOT_FOUND` | 대상 회원 또는 리뷰 없음 |

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
| phone | 선택, 형식 `01[0-9]-?\d{3,4}-?\d{4}` |

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

### 로그인

`POST /api/auth/login` 성공 응답은 Access Token을 응답 헤더로 전달하고 Refresh Token을 `Set-Cookie`로 전달한다.

```http
Authorization: Bearer {accessToken}
```

```http
Set-Cookie: refresh_token={jwt}; HttpOnly; Secure; SameSite=Lax; Path=/; Max-Age=1209600
```

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

`POST /api/auth/logout`은 폐기 대상 토큰을 Redis 블랙리스트에 등록하고 Refresh Token 만료 쿠키를 응답한다.

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

## 열린 결정

- 이미지 업로드 API 분리, 인기 검색어 집계 저장소, 경매 입찰 동시성 제어는 [docs/adr/README.md](adr/README.md)에서 관리한다.
