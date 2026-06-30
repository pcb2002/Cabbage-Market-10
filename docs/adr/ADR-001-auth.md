# ADR-001: Access Token Stateless JWT와 Refresh Token Cookie 인증

## 상태

Accepted

## 배경

로그인·로그아웃·인증 API 구현 전 인증 방식을 확정해야 했다.
상태 비저장(Stateless) 방식과 서버 측 세션 중 하나를 선택해야 했다.
Refresh Token을 클라이언트 저장소에 직접 보관하면 XSS 노출 위험이 커지고,
순수 Stateless JWT만 사용하면 로그아웃·강제 만료 처리가 어렵다.

## 결정

- **인증 방식**: Access Token은 Stateless JWT, Refresh Token은 Cookie 기반 JWT 인증
- **Access Token 전달**: Access Token은 응답 헤더로 전달하고 API 요청마다 `Authorization: Bearer` 헤더로 전달한다.
- **Refresh Token 전달**: Refresh Token은 `HttpOnly` Cookie로 전달한다.
- **JWT 검증**: `JwtAuthenticationFilter`는 DB 조회 없이 Access Token의 JWT Claim, Redis 블랙리스트, 정지 계정 PK 마커를 검증한다.
- **Redis 역할**: Redis는 유효 토큰 저장소가 아니라 폐기된 Access Token의 블랙리스트와 정지 계정 PK 마커 저장소로만 사용한다.
- **로그아웃 처리**: 로그아웃 시 Access Token의 `jti`를 Redis 블랙리스트에 TTL과 함께 저장하고, Refresh Token은 만료 쿠키를 내려 현재 클라이언트에서 제거한다.
- **Refresh Token 재발급**: Refresh Token은 서버 저장소나 블랙리스트 없이 JWT 서명, 만료 시간, 토큰 타입, 회원 상태를 검증해 재발급한다.
- **정지 계정 처리**: 이 PR은 관리자 백오피스의 회원 상태 변경 API를 포함하지 않는다. 운영·테스트에서 회원 상태를 `SUSPENDED`로 직접 변경하면 로그인과 Refresh Token 재발급은 DB 상태 조회로 차단된다.
  기존 Access Token까지 즉시 차단해야 하면 Redis에 `auth:suspended-client:{clientId}` 회원 PK 차단 마커를 직접 등록한다. 해당 계정이 기존 Access Token으로 인증을 시도하면 요청을 거부하고, 그 토큰의 `jti`를 블랙리스트에 등록한다. 회원 PK 마커는 Access Token 만료 시간까지 유지하고, 계정 활성화 시 운영 명령으로 명시적으로 삭제한다.
- **CSRF 보호**: Refresh Token을 Cookie로 전달하므로 Spring Security CSRF 보호를 활성화한다.

## 결과

- 서버는 인증 상태를 세션에 저장하지 않고 Access Token의 JWT Claim을 기준으로 인증 사용자를 복원한다.
- 인증 필터는 사용자 DB를 조회하지 않고 Redis의 정지 계정 PK 마커로 기존 JWT의 정지 여부를 지연 차단한다.
- 로그아웃 이후 폐기 대상 Access Token은 남은 만료 시간 동안 Redis 블랙리스트로 차단된다.
- Redis에는 폐기된 Access Token의 `jti`, 정지 계정 PK 마커, TTL만 저장하므로 유효 토큰 목록과 Refresh Token 상태는 관리하지 않는다.
- Redis 장애 시 폐기 토큰 여부를 확인할 수 없으므로 인증 요청은 실패 처리한다.
- Refresh Token Cookie를 사용하는 요청은 CSRF 토큰 검증 없이는 처리할 수 없다.
- 이 방식은 현재 클라이언트 기준 로그아웃을 보장하며, 이미 유출된 Refresh Token의 서버 측 즉시 무효화는 보장하지 않는다.
- 서버 재시작 후에도 JWT 자체는 유효하지만 Redis 데이터가 유실되면 폐기된 Access Token 차단 상태도 사라질 수 있다.

## 구현 구조

- **AuthFacade**: Controller가 호출하는 인증 유스케이스 진입점이며 `signup`, `login`, `refresh`, `logout` 흐름을 조합한다.
- **AuthService**: 회원가입, 이메일·비밀번호 인증, 활성 회원 조회를 담당한다.
- **TokenService**: Access Token, Refresh Token 발급과 Refresh Token의 서명·만료·타입 검증을 담당한다.
- **BlackListService**: 로그아웃 또는 강제 만료 시 Access Token 블랙리스트 등록을 담당한다.
- **JwtTokenProvider**: JWT 생성과 서명·만료·Claim·토큰 타입 검증을 담당한다.
- **JwtAuthenticationFilter**: 일반 API 요청의 Access Token 인증, 블랙리스트 조회, 정지 회원 Redis 마커 검증, `SecurityContext` 저장을 담당한다.
