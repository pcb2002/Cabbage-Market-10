# 보안 정책

## 인증

> 인증 방식 결정 상세: [docs/adr/ADR-001-auth.md](adr/ADR-001-auth.md)

- 회원가입, 로그인, 토큰 재발급, 공개 조회를 제외한 API는 인증을 요구한다.
- 비밀번호는 Spring Security `PasswordEncoder`로 단방향 해시한다.
- 인증 방식은 Access Token Stateless JWT와 Refresh Token Cookie 인증을 사용한다.
- Access Token은 응답 헤더로 전달하고 API 요청마다 `Authorization: Bearer` 헤더로 전달한다.
- Refresh Token은 `HttpOnly`, `Secure`, `SameSite=Lax` Cookie로만 전달한다.
- Refresh Token Cookie의 `Secure` 속성은 운영 환경에서 `true`, 로컬 환경(`local`)에서 `false`로 설정한다.
- API 인증은 Access Token으로 처리한다.
- 토큰 재발급은 `refresh_token` Cookie의 Refresh Token으로 처리한다.
- JWT에는 `jti`를 포함하고, 로그아웃·강제 만료 토큰의 `jti`는 Redis 블랙리스트에 저장한다.
- 로그아웃 시 Access Token과 Refresh Token을 Redis 블랙리스트에 등록하고 만료 쿠키를 내려준다.
- 관리자에 의해 정지된 계정은 현재 발급된 토큰 `jti`를 서버가 알 수 없으므로 회원 상태를 `SUSPENDED`로 변경한 뒤 Redis에 회원 PK를 임시 차단 마커로 저장한다.
- 정지 계정의 기존 Access Token으로 인증을 시도하면 서버는 회원 PK 마커를 확인해 접근을 거부하고, 해당 토큰의 `jti`를 Redis 블랙리스트에 등록한 뒤 회원 PK 마커를 삭제한다.
- 블랙리스트 토큰과 정지 계정 PK 마커는 TTL을 설정해 Redis가 만료 후 자동 삭제한다.
- Refresh Token Cookie를 사용하는 요청은 `XSRF-TOKEN` Cookie와 `X-XSRF-TOKEN` Header로 CSRF를 검증한다.
- Redis 블랙리스트 통합 테스트는 로컬 공유 Redis가 아니라 embedded Redis로 격리한다.

## 권한

- 내 정보 조회·수정은 본인만 가능하다.
- 상품 수정, 상태 변경, 삭제는 판매자만 가능하다.
- 상품 문의 답변은 판매자만 가능하다.
- 문의 수정·삭제는 작성자만 가능하다.
- 팔로우 취소는 팔로우한 본인만 가능하다.
- 채팅방과 메시지는 참여자만 조회할 수 있다.
- 리뷰 수정·삭제는 작성자만 가능하다.
- 입찰은 인증 회원만 가능하다.

## WebSocket

- `/ws/chat` 연결 시 인증 정보를 검증한다.
- 채팅방 구독과 메시지 전송은 ChatMember 여부를 확인한다.
- 서버는 클라이언트가 보낸 sender 값을 신뢰하지 않고 인증 사용자로 판단한다.

## 민감정보

- 비밀번호, 토큰, 전화번호는 로그에 남기지 않는다.
- Client 응답 DTO에 password를 포함하지 않는다.
- Access Token과 Refresh Token을 응답 본문에 포함하지 않는다.
- Access Token과 Refresh Token을 로그, 예외 메시지, URL에 노출하지 않는다.
- 운영 비밀키와 DB 계정은 환경변수 또는 Secret Store로 관리한다.
- `.env`, 운영 설정, 인증서, 실제 키는 커밋하지 않는다.

## 입력 검증

- Request DTO는 Bean Validation을 사용한다.
- 파일 업로드는 크기, 확장자, Content-Type을 검증한다.
- 사용자 입력을 로그나 쿼리에 안전하지 않게 결합하지 않는다.
