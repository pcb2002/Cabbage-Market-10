# ADR

중요한 기술 결정은 이 폴더에 기록한다.

## 열린 결정

확정 전 구현을 막는 결정을 한곳에서 관리한다. 확정되면 `ADR-{번호}` 문서로 옮기고 상태를 Accepted로 바꾼다.

| 결정 | 상태 | 막는 작업 |
|---|---|---|
| 이미지 저장소(로컬/S3)와 업로드 API 분리 여부 | Proposed | 상품 이미지 업로드 |
| 인기 검색어 집계 저장소(Redis 여부) | Proposed | 인기 검색어 조회 |
| 경매 입찰 동시성 제어 방식 | Proposed | 입찰 |

## 확정된 결정

| ADR | 내용 |
|---|---|
| [docs/adr/ADR-001-auth.md](ADR-001-auth.md) | JWT Access Token + Redis Refresh Token 인증 |
