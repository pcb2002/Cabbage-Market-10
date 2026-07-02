# ADR-004 인기 검색어 Redis 집계

## Status

Accepted

## Context

인기 검색어 조회는 검색 요청이 발생할 때마다 검색어별 집계를 갱신하고,
조회 시점에는 상위 검색어를 빠르게 반환해야 한다.
회원과 비회원 검색을 모두 반영하되 동일 주체의 짧은 시간 반복 검색은 중복 집계를 줄여야 한다.
애플리케이션 인스턴스가 여러 대로 늘어날 수 있으므로
프로세스 메모리 기반 집계보다 인스턴스 간 공유 가능한 저장소가 필요하다.

## Decision

인기 검색어 집계 저장소는 Redis를 사용하고, 집계 자료구조는 Sorted Set을 사용한다.

- 인기 검색어 조회 API는 `GET /api/search/popular`을 사용한다.
- 검색어 집계는 상품 검색 API `GET /api/v1/items/search`, `GET /api/v2/items/search` 요청 시 수행한다.
- 집계 기준은 조회 당일 `00:00 ~ 현재 시점`의 일별 누적 검색 횟수다.
- 일별 집계 key는 `popular:search:{yyyyMMdd}` 형식을 사용한다.
- 검색어 score는 Redis `ZINCRBY`로 증가시키고, 조회는 score 내림차순 상위 N개를 반환한다.
- 동일 회원은 `userId`, 비회원은 `sessionId`를 기준으로 같은 검색어의 짧은 시간 반복 검색을 중복 집계하지 않는다.
- 중복 방지 key는 `popular:dedup:{subject}:{yyyyMMdd}:{keyword}` 형식을 사용한다.
- 인기 검색어 반환 개수, dedup TTL, 일별 key TTL, key prefix는 `app.search.popular.*` 설정으로 관리한다.
- 검색어가 없거나 공백이면 집계하지 않는다.
- Redis Bean이 없거나 Redis를 사용할 수 없는 환경에서는 인기 검색어 집계/조회 결과를 비활성화된 상태로 처리하고, 조회는 빈 목록을 반환한다.

## Consequences

- 다중 인스턴스 환경에서도 같은 인기 검색어 집계 결과를 공유할 수 있다.
- 검색 요청 시 Redis 쓰기가 추가되므로 Redis 가용성과 네트워크 지연의 영향을 받는다.
- 일별 집계를 선택했기 때문에 최근 24시간 rolling window가 아니라 날짜 경계 기준으로 순위가 초기화된다.
- 회원/비회원 중복 검색 완화는 가능하지만, dedup TTL 안에서는 실제 반복 검색 수보다 낮게 집계될 수 있다.
- 테스트는 회원/비회원 중복 집계 방지, 빈 결과 반환, 검색 API 연동 경로를 함께 검증해야 한다.
