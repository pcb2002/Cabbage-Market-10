# ADR

중요한 기술 결정은 이 폴더에 기록한다.

## 확정된 결정

| ADR | 내용 |
|---|---|
| [docs/adr/ADR-001-auth.md](ADR-001-auth.md) | Access Token Stateless JWT와 Refresh Token Cookie 인증 |
| [docs/adr/ADR-002-auction-bid-concurrency.md](ADR-002-auction-bid-concurrency.md) | 경매 입찰 동시성 제어는 Redis 기반 Redisson 분산 락 사용 |
| [docs/adr/ADR-003-item-search-v2-redis-cache.md](ADR-003-item-search-v2-redis-cache.md) | 상품 검색 v2 캐시는 Redis remote cache를 기본 저장소로 사용 |
| [docs/adr/ADR-004-popular-search-redis.md](ADR-004-popular-search-redis.md) | 인기 검색어 집계는 Redis Sorted Set 기반 일별 집계를 사용 |
| [docs/adr/ADR-005-item-image-storage-s3.md](ADR-005-item-image-storage-s3.md) | 상품 이미지는 AWS S3에 저장하고 업로드 API는 상품 수정 API와 분리한다 |
