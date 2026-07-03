# 도전 기능 회고 — 캐싱을 이용한 성능 개선

과제 가이드의 "캐싱을 이용한 성능 개선" 도전 기능 4개 항목에 대한 진행 상태와, 가이드가 요구하는 고민 포인트에 대한 답을 실제 코드 근거와 함께 정리한다.

## 1. Local Cache → Redis Remote Cache 전환

**상태: 완료.** `ItemSearchCacheConfig`에 local(Caffeine)/redis 두 `CacheManager`가 모두 구현돼 있고, `app.cache.item-search-v2.type` 설정값으로 스위칭한다. 기본값은 `redis`(`matchIfMissing = true`)이므로 별도 설정이 없으면 이미 Redis Remote Cache로 동작한다.

**왜 Redis로 전환했는가 (Scale-out 관점):** 로컬 메모리 캐시(Caffeine)는 애플리케이션 인스턴스마다 독립적인 캐시 공간을 가진다. 인스턴스를 여러 대로 늘리면(Scale-out) 같은 검색 조건이라도 어느 인스턴스가 요청을 받았는지에 따라 캐시 적중 여부가 달라지고, 상품 변경 후 무효화도 인스턴스별로 따로 해줘야 해서 일관성이 깨진다. `docs/adr/ADR-003`에도 이 이유가 명시돼 있다: "애플리케이션 인스턴스가 여러 대로 늘어나면 프로세스 메모리 기반 local cache는 인스턴스마다 값이 갈라져 캐시 적중률과 일관성이 떨어진다." Redis는 모든 인스턴스가 공유하는 단일 캐시 저장소라 이 문제가 없다.

**Serializer 구성 — 가이드와 실제 구현의 차이:** 가이드는 `StringRedisSerializer`(Key) + `GenericJackson2JsonRedisSerializer`(Value) 조합을 권장하지만, 실제 `ItemSearchCacheConfig`는 Key는 `StringRedisSerializer`, Value는 `JdkSerializationRedisSerializer`를 쓴다. Java 직렬화 방식이라 Jackson을 안 거치므로 가이드가 경고한 "`LocalDateTime` 캐싱 시 `ObjectMapper`에 `JavaTimeModule` 등록 필요" 문제 자체가 발생하지 않는다. 다만 트레이드오프가 있다: JDK 직렬화는 Jackson JSON 직렬화보다 페이로드가 크고, Redis에 저장된 값을 다른 언어/도구에서 읽거나 디버깅하기 어렵고, `SearchItemResponse` 클래스 구조가 바뀌면 기존 캐시가 역직렬화에 실패할 수 있다(배포 시 캐시 clear 필요). Jackson 방식으로 바꾸는 게 운영 관점에서는 더 유리할 수 있다.

**고민 POINT 1 — Memcached 대신 Redis를 선택한 이유:** 이 프로젝트는 검색 캐시 이전에 이미 Redis를 두 곳에 쓰고 있었다 — 경매 입찰 동시성 제어(`docs/adr/ADR-002`, Redisson 분산 락)와 JWT 블랙리스트(`RedisTokenBlacklistStore`). 인프라를 새로 추가하지 않고 재사용할 수 있다는 게 가장 큰 이유다. 기능적으로도 Memcached는 단순 Key-Value만 지원하는데, 인기 검색어 기능(`docs/adr/ADR-004`)은 Sorted Set(ZSet) 같은 정렬 자료구조가 필요해서 Memcached로는 애초에 구현이 어렵다. 같은 Redis 인스턴스로 캐싱과 랭킹 집계를 모두 처리할 수 있다는 점도 Redis를 선택한 이유다.

**고민 POINT 2 — Redis에 저장할 때 쓴 자료구조와 선택 이유:** 두 가지를 구분해서 봐야 한다.
- 검색 v2 캐시(`RedisCacheManager`)는 내부적으로 Redis String에 직렬화된 `Page<SearchItemResponse>` 객체 하나를 통째로 저장한다. "이 검색 조건의 결과 페이지"를 그대로 저장했다가 그대로 꺼내 쓰는 단순 lookup 패턴이라 String(Key-Value)이면 충분하다.
- 인기 검색어(`PopularSearchService`)는 Sorted Set을 쓴다. `ZINCRBY`로 검색어별 점수를 실시간으로 누적하고 `ZREVRANGE`로 상위 N개를 점수 내림차순으로 즉시 뽑아야 하는데, 이런 "누적 집계 + 정렬된 순위 조회"는 String으로는 못하고 Sorted Set의 정렬 자료구조가 필요하다.

**추가 학습 POINT — RDBMS와 NoSQL(Redis)의 차이:** RDBMS(MySQL)는 스키마가 고정된 테이블에 데이터를 저장하고, 여러 테이블을 JOIN해서 복잡한 관계형 쿼리와 트랜잭션(ACID)을 지원한다. 이 프로젝트에서 상품, 회원, 채팅 같은 핵심 도메인 데이터가 MySQL에 있는 이유다. Redis는 스키마가 없는 Key-Value 저장소로, String/Hash/List/Set/Sorted Set 같은 몇 가지 자료구조만 제공하는 대신 메모리 기반이라 응답이 매우 빠르고, TTL로 자동 만료를 지원한다. 대신 복잡한 JOIN이나 강한 일관성 트랜잭션은 지원하지 않는다. 그래서 이 프로젝트는 "정합성이 중요한 원본 데이터"는 MySQL에, "빠른 조회가 중요하고 유실돼도 재계산 가능한 파생 데이터"(검색 캐시, 인기 검색어 집계, 락, 토큰 블랙리스트)는 Redis에 두는 방식으로 역할을 나눴다.

## 2. 대용량 Dummy 데이터 적재

**상태: 완료(운영 환경 기준 확인됨).** 2026-07-02 기준 `item` 테이블에 1,000,019건이 있는 걸 `SELECT COUNT(*)`로 직접 확인했다. 요구사항(5만 건 이상)은 충족한다.

다만 저장소 안에는 이 데이터를 적재한 스크립트(Stored Procedure, JDBC Batch, Datafaker 등)가 커밋돼 있지 않다. 가이드가 요구하는 "어떤 방법으로, 왜 그 방법을 선택했는지"에 대한 근거는 코드베이스에서 확인할 수 없었다 — 이 부분은 실제로 어떤 방식으로 100만 건을 넣었는지 직접 채워 넣어야 한다(예: JDBC Batch Insert를 썼다면 1건씩 INSERT 대비 체감한 성능 차이, commit 단위를 어떻게 잡았는지 등).

## 3. Redis Cache 적용 후 v1/v2 성능테스트 + 보고서

**상태: 진행 중.** `performance/k6/search-api.js`, `performance/k6/popular-search-api.js`로 k6 부하 테스트를 진행했고 결과는 `performance/k6/REPORT.md`에 정리돼 있다. 100만 건 데이터 기준으로 확인된 핵심 결과는 다음과 같다.

- 검색 v2(로그인, 캐시 미적중 경로)는 p95 27.89s로 목표(500ms) 대비 크게 미달했다. 원인은 `title`/`description`에 인덱스를 못 타는 LIKE 풀스캔 + 캐시 조건 미스였다.
- 인기 검색어 API는 p95 12.85ms로 목표를 여유 있게 통과했다. Redis Sorted Set 조회만 하고 item 테이블을 안 건드리기 때문에 데이터 규모(100만 건)와 무관하게 빠르다.
- 이후 `@Cacheable` 조건을 수정(로그인/비로그인 모두 캐시 적용)했지만, 이 변경을 반영한 재측정은 아직 하지 않았다.

가이드가 요구하는 "평균 응답속도 개선폭"과 "처리 가능한 동시 사용자 수(TPS, 포화지점) 비교"를 채우려면 v1 베이스라인 테스트와, 캐시가 실제로 적중하는 v2 재측정이 남아 있다. `REPORT.md`의 "다음 단계" 항목 참고.

## 4. Cache Eviction으로 캐시 동기화 문제 해결

**상태: 완료했지만 가이드가 제시한 방식과는 다르게 구현.** `SearchCacheEvictionService.evictItemSearchV2AfterCommit()`이 상품 생성/수정/상태변경/삭제/좋아요 토글/입찰 성공 트랜잭션이 커밋된 이후 `itemSearchV2` 캐시 전체를 `cache.clear()`로 비운다. `ItemFacade`, `ItemLikeFacade`, `AuctionFacade`에서 각 쓰기 작업 뒤에 이 메서드를 호출한다. 가이드가 언급한 `@CacheEvict`/`@CachePut` 어노테이션은 코드베이스 어디에도 없다 — 특정 key만 지우는 선언적 방식 대신, `CacheManager`를 직접 주입받아 캐시 전체를 프로그래매틱하게 비우는 방식을 택했다.

**고민 POINT 1 — TTL 설계:** 이 프로젝트에서 TTL이 걸린 캐시성 데이터는 세 종류다.
- 검색 v2 결과: TTL 5분(`ItemSearchCacheProperties.ttl`, 기본값). 검색 결과는 쓰기 작업 시 즉시 전체 무효화되므로, 이 TTL은 "무효화 로직이 실패했을 때의 안전망" 역할에 가깝다.
- 인기 검색어 일별 집계 key: TTL 8일(`daily-key-ttl`). 하루 단위로 순위가 갱신되지만, 조회 시점 기준 자정 근처의 날짜 경계 문제(자정 직후 재조회 등)를 감안해 여유 있게 며칠 더 살려둔다.
- 인기 검색어 중복 집계 방지 key: TTL 1분(`dedup-ttl`). 같은 사용자가 짧은 시간 안에 같은 검색어를 반복 검색해도 한 번만 집계되도록 하는 용도라, 값이 크면 정상적인 재검색까지 집계가 안 되고 너무 작으면 중복 집계 방지 효과가 없어서 1분 정도로 짧게 잡았다.

**고민 POINT 2 — Eviction Policy(캐시가 꽉 찼을 때):** 검색 v2 캐시는 LRU/LFU 같은 항목별 축출 정책을 별도로 설정하지 않았다. Local(Caffeine) 모드에서는 `maximumSize`(기본 1000)를 넘으면 Caffeine의 기본 정책(Window TinyLFU, 사실상 LFU에 가까운 적응형 정책)이 자동으로 적용된다. Redis 모드는 이 프로젝트가 캐시 항목별 max size를 별도로 제한하지 않고 TTL(5분)로만 자연 만료시키므로, Redis 서버 자체의 `maxmemory-policy`(기본 noeviction) 설정에 캐시 전체 메모리 관리를 맡기는 구조다. 검색 조건 조합이 많아 캐시 키 수가 많아질 수 있는데, 이 부분은 지금 구조에서 명확히 관리되고 있지 않은 지점이라 운영 전에는 Redis `maxmemory-policy`를 `allkeys-lru` 등으로 명시적으로 설정하는 걸 검토할 필요가 있다.

**고민 POINT 3 — 즉시 무효화 vs TTL 자연 만료 trade-off:** 이 프로젝트는 상품 데이터 변경에 대해 TTL을 기다리지 않고 트랜잭션 커밋 직후 즉시 캐시를 비우는 쪽을 택했다(`docs/adr/ADR-003`: "상품 생성, 게시, 수정, 상태 변경, 삭제, 좋아요 토글, 입찰 성공 후 검색 캐시는 트랜잭션 커밋 이후 무효화한다"). 상품 재고 상태(품절, 예약 등)나 좋아요 수는 사용자가 바로 확인하고 싶어 하는 정보라 최대 5분(TTL)까지 낡은 정보를 보여주는 건 사용자 경험상 허용하기 어렵다고 판단한 것으로 보인다. 대신 트레이드오프도 있다: 쓰기가 잦은 서비스라면 캐시가 자주 통째로 비워지면서 캐시 적중률 자체가 떨어질 수 있고, 지금처럼 "전체 clear" 방식은 방금 캐싱된, 이번 쓰기와 무관한 다른 검색 조건의 캐시까지 함께 날아간다는 비효율이 있다. 특정 key 단위로만 무효화하려면 상품 하나의 변경이 어떤 검색 캐시 키들에 영향을 주는지 역추적해야 하는데, 검색 조건 조합이 매우 다양해서(카테고리, 가격범위, 정렬 등) 이 프로젝트는 그 복잡도 대신 단순함을 택한 것으로 보인다.
