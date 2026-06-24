# ADR-001: 인증 방식

## 상태

Accepted

## 배경

로그인·로그아웃·인증 API 구현 전 인증 방식을 확정해야 했다.
상태 비저장(Stateless) 방식과 서버 측 세션 중 하나를 선택해야 했다.

## 결정

- **인증 방식**: JWT (Access Token + Refresh Token)
- **Access Token**: 단기 만료, API 요청마다 Authorization 헤더로 전달
- **Refresh Token**: 장기 만료, Redis에 저장하고 재발급에 사용
- **로그인 응답 필드**: `accessToken`, `refreshToken`, `tokenType`, `expiresIn`
- **토큰 재발급**: `POST /api/auth/refresh` — Redis의 Refresh Token 검증 후 재발급
- **로그아웃**: Redis에서 Refresh Token 삭제

## 결과

- Spring Security FilterChain에 JwtAuthenticationFilter를 추가한다.
- Redis 의존성이 추가된다 (Refresh Token 저장소).
- 토큰 만료 시 클라이언트가 재발급 API를 호출하는 흐름을 가진다.
- 서버 재시작 시 Access Token은 유효하지만 Redis가 초기화되면 Refresh Token이 소멸한다.
