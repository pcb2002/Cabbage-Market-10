# ADR-002 경매 입찰 동시성 제어

## Status

Accepted

## Context

경매 입찰은 같은 상품에 대해 여러 사용자가 동시에 더 높은 금액을 제시할 수 있다. 현재 최고 입찰가와 최고 입찰자 갱신은 하나의 임계 구역으로 처리되어야 하며, 애플리케이션 인스턴스가 여러 개일 때도 같은 상품에 대한 입찰 순서가 보장되어야 한다.

## Decision

경매 입찰 동시성 제어는 Redis 기반 Redisson 분산 락을 사용한다.

- 락 키는 상품 단위로 `auction:bid:{itemId}` 형식을 사용한다.
- 락 획득 후 상품 판매자 검증과 현재가 갱신을 수행한다.
- 제한 시간 내 락을 얻지 못하면 `AUCTION_BID_LOCK_FAILED` 예외를 반환한다.
- Redis 접속 정보는 `spring.data.redis.*` 설정을 사용한다.
- `app.auction.redis-lock.enabled=false`이면 Redisson Bean과 입찰 Facade를 비활성화한다. 테스트 프로필은 Redis 서버 없이 애플리케이션 컨텍스트를 띄우기 위해 이 값을 사용한다.

## Consequences

- 애플리케이션 다중 인스턴스 환경에서도 같은 상품의 입찰 갱신을 직렬화할 수 있다.
- 입찰 API는 Redis 가용성에 영향을 받는다.
- 로컬과 운영 환경은 Redis host, port, password 설정을 제공해야 한다.
- 입찰 동시성 검증은 Redisson 락 획득 성공, 실패, unlock 경로를 포함해야 한다.
