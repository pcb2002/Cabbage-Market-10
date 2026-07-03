# 배추마켓 문서 인덱스

현재 `docs/` 문서는 이 저장소의 **현재 코드 구현**을 기준으로 관리한다.

| 문서 | 책임 |
|---|---|
| [docs/api.md](api.md) | 현재 구현된 REST API, WebSocket 경로, 인증 기준 |
| [docs/ERD.md](ERD.md) | 현재 엔티티 기준 테이블, 컬럼, 관계, 삭제 정책 |
| [docs/security.md](security.md) | JWT, Refresh Token Cookie, CSRF, WebSocket 인증 |
| [docs/business-rules.md](business-rules.md) | 도메인별 상태 규칙, 권한 규칙, 삭제 정책 |
| [docs/convention.md](convention.md) | 현재 패키지 구조, 계층 규칙, 테스트/문서 규칙 |
| [docs/workflow.md](workflow.md) | 브랜치, 커밋, 리뷰, PR 작업 흐름 |
| [docs/adr/README.md](adr/README.md) | 현재 코드에 반영된 기술 결정 기록 |
| [docs/plans/README.md](plans/README.md) | 장기 작업 기록 규칙 |

## 현재 코드 기준 빠른 요약

- 인증: Access Token JWT + Refresh Token Cookie
- 상품 검색: `v1` 일반 조회, `v2` 비로그인 검색 Redis 캐시
- 인기 검색어: Redis Sorted Set 일별 집계
- 상품 이미지: S3 업로드 + 별도 이미지 API
- 채팅: STOMP WebSocket, `CONNECT` 시 `Authorization` 헤더 검증
- 경매 입찰: Redis Redisson 분산 락

## 주의

- 문서에 없는 API는 현재 코드에 구현되지 않은 것으로 본다.
- 코드와 문서가 다르면 코드를 우선하고, 문서를 즉시 갱신한다.
