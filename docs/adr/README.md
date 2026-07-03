# ADR

이 폴더는 **현재 코드에 반영된 기술 결정**을 기록한다.

## 확정된 결정

| ADR | 내용 |
|---|---|
| [ADR-001-auth.md](ADR-001-auth.md) | Access Token JWT + Refresh Token Cookie 인증 |
| [ADR-002-auction-bid-concurrency.md](ADR-002-auction-bid-concurrency.md) | 경매 입찰은 Redis Redisson 분산 락 사용 |
| [ADR-003-item-search-v2-redis-cache.md](ADR-003-item-search-v2-redis-cache.md) | 검색 v2는 비로그인 조회 Redis 캐시 사용 |
| [ADR-004-popular-search-redis.md](ADR-004-popular-search-redis.md) | 인기 검색어는 Redis Sorted Set 일별 집계 |
| [ADR-005-item-image-storage-s3.md](ADR-005-item-image-storage-s3.md) | 상품 이미지는 S3 저장 + 별도 업로드 API 사용 |

## 사용 규칙

- 코드에 반영되지 않은 결정은 `Accepted`로 두지 않는다.
- 일반 API/ERD/보안 설명은 ADR이 아니라 책임 문서에서 관리한다.
- 현재 구현과 다르면 ADR보다 코드를 먼저 확인하고, 결정 자체가 바뀐 경우에만 ADR을 수정한다.
