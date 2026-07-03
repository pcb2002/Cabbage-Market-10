# 보안 정책

현재 문서는 `SecurityConfig`, JWT 필터, STOMP 인터셉터 기준이다.

## 인증

- Access Token은 Stateless JWT를 사용한다.
- Refresh Token은 Cookie로 전달한다.
- Access Token은 `Authorization: Bearer {token}` 헤더로 받는다.
- Refresh Token 재발급은 `refresh_token` Cookie로 처리한다.
- 로그아웃 시 Access Token은 Redis 블랙리스트에 등록하고 Refresh Token은 만료 쿠키로 비운다.
- 정지 회원은 로그인과 Refresh Token 재발급이 차단된다.
- JWT 인증 필터는 블랙리스트와 정지 회원 마커를 검사한다.

## 현재 공개 경로

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
- `GET /`
- `GET /index.html`
- `GET /app.js`
- `GET /styles.css`
- `/ws/chat`, `/ws/chat/**`

그 외 요청은 인증이 필요하다.

## CSRF

- CSRF 보호는 Refresh Token Cookie를 사용하는 요청에만 적용한다.
- 대상은 `POST /api/auth/refresh`, `POST /api/auth/logout`이다.
- 서버는 `XSRF-TOKEN` Cookie와 `X-XSRF-TOKEN` 헤더를 사용한다.

## CORS

- CORS는 `app.cors.allowed-origins` 설정을 사용한다.
- 기본 허용 origin은 설정이 없을 때 `http://localhost:5173`이다.
- 허용 메서드: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`
- 허용 헤더: `Authorization`, `Content-Type`, `Cache-Control`, `X-XSRF-TOKEN`
- 노출 헤더: `Authorization`
- Credentials 허용: `true`

## WebSocket / STOMP

- HTTP handshake endpoint는 `/ws/chat`이다.
- STOMP publish prefix는 `/pub`, subscribe prefix는 `/sub`다.
- 실제 인증은 `StompAuthInterceptor`가 `CONNECT` 프레임에서 처리한다.
- `CONNECT`에는 `Authorization: Bearer {accessToken}` 헤더가 필요하다.
- 토큰이 없으면 `ACCESS_TOKEN_MISSING`, 유효하지 않으면 `ACCESS_TOKEN_INVALID` 예외를 던진다.
- 현재 구현은 WebSocket 구독 시 별도 채팅방 멤버십 검증을 두지 않고, 메시지 저장/조회 단계에서 참여자 검증을 한다.

## 권한

- 내 정보 조회/수정은 본인만 가능하다.
- 상품 등록/수정/상태 변경/삭제/이미지 관리는 판매자만 가능하다.
- 문의 수정/삭제는 작성자만 가능하다.
- 문의 답변 작성은 상품 판매자만 가능하다.
- 채팅 메시지 조회는 참여자만 가능하다.
- 채팅 메시지 삭제는 작성자만 가능하다.
- 리뷰 작성은 거래 완료 상품의 실제 구매자만 가능하다.
- 입찰은 인증 회원만 가능하고, 본인 상품에는 할 수 없다.

## 민감정보

- 비밀번호는 BCrypt로 해시 저장한다.
- Access Token과 Refresh Token을 응답 본문에 넣지 않는다.
- Access Token은 응답 헤더, Refresh Token은 Cookie로만 전달한다.
- 로그와 예외 응답에 비밀번호/토큰 원문을 노출하지 않는다.

## 오류 처리

- 인증 실패는 `UNAUTHORIZED`
- 권한 실패는 `FORBIDDEN`
- 블랙리스트 토큰은 `BLACKLISTED_TOKEN`
- Access Token 누락은 `ACCESS_TOKEN_MISSING`
- Access Token 만료는 `ACCESS_TOKEN_EXPIRED`
- Refresh Token 만료는 `REFRESH_TOKEN_EXPIRED`
