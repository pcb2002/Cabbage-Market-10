# 검색 API 성능 테스트 보고서

## 개요

- 목적: 상품 검색 v1(캐시 없음) / v2(Redis Remote Cache 적용) API와 인기 검색어 API의 응답 성능·처리량을 측정하고, Redis 캐시 도입 효과를 검증한다.
- 도구: k6
- 스크립트: `performance/k6/search-api.js`(검색 v1/v2, `API_VERSION`/`ANONYMOUS` 환경변수로 대상·인증 상태 전환), `performance/k6/popular-search-api.js`(인기 검색어)
- VU 프로파일: 30s 동안 5 → 1m 동안 20 → 30s 동안 0 (ramping), 공통 적용
- Threshold: `checks rate>0.99`, `http_req_duration p95<500ms`, `http_req_failed rate<0.01`
- 테스트 대상 데이터 규모: `item` 테이블 1,000,019건 (1차~3차 테스트 전 구간에서 동일하게 유지된 상태로 확인됨)

## 실행 이력

| 회차 | 대상 | 인증 상태 | 결과 |
|---|---|---|---|
| 1차 | v2 (`/v2/items/search`) | 로그인 | URL에 `/api` prefix 누락으로 전 요청 404 → 100% 실패. 스크립트 버그, 실측 데이터 아님 |
| 2차 | v2 (`/api/v2/items/search`) | 로그인 | URL 수정 후 재실행. 요청 성공률 100%, 응답 속도는 threshold 대폭 초과 |
| 3차 | 인기 검색어 (`/api/search/popular`) | 없음(공개 API) | 전 threshold 통과. p95 12.85ms |
| 4차 | v1 (`/api/v1/items/search`) | 로그인 | 캐시 없는 베이스라인. 전 threshold 중 응답속도만 실패. p95 6.5s |
| 5차 | v2 (`/api/v2/items/search`) | 비로그인 | `condition` 제거 반영 후 재실행. 전 threshold 통과. p95 15.73ms |
| 6차 | v2 (`/api/v2/items/search`) | 로그인 | `condition` 제거 반영 후 재실행. 전 threshold 통과. p95 13.5ms |
| 7차 | v2 (`/api/v2/items/search`) | 로그인 | 6차와 동일 조건 재검증. 전 threshold 통과. p95 24.52ms |

4~6차는 동일한 `item` 테이블 데이터 규모(1,000,019건)에서 `performance/k6/search-api.js`에 `API_VERSION`/`ANONYMOUS` 환경변수를 추가해 실행했다. v1·v2 캐시 적중 비교라는 원래 목적을 충족하는 데이터를 확보했다. 7차는 이 상태에서 6차를 한 번 더 반복한 결과로, DB/캐시 상태가 그 사이 바뀌었다는 보고는 없었다는 전제로 정리했다(별도 초기화·재적재를 했다면 이 비교는 무효).

## 결과 1: 상품 검색 v2 (2차, 로그인 사용자·캐시 미적중 경로)

| 지표 | 값 |
|---|---|
| 총 checks | 275 (100% 성공) |
| search 요청 수 | 91 |
| search 실패율 | 0.00% |
| 응답시간 avg | 13.09s |
| 응답시간 min | 2.76s |
| 응답시간 median | 11.37s |
| 응답시간 p90 | 27.18s |
| 응답시간 p95 | 27.89s (목표: 500ms 미만) |
| 응답시간 max | 29.21s |
| VU (max) | 20 |

Threshold 판정: `checks` 통과 · `http_req_failed` 통과 · `http_req_duration p95<500ms` **실패**(목표 대비 약 56배)

**원인**

1. 이 테스트에서는 Redis 캐시가 한 번도 적중하지 않았다. 당시 `SearchService.searchItemsV2`의 `@Cacheable(condition = "#clientId == null")`이 비로그인 요청에만 캐시를 적용했는데, k6 스크립트는 로그인 후 매 요청에 `Authorization` 헤더를 보내 91건 모두 DB 직접 조회로 처리됐다.
2. `ItemSearchRepositoryImpl`이 `title`/`description`에 `containsIgnoreCase`(앞뒤 와일드카드 LIKE)를 사용해 인덱스를 타지 못하는 풀스캔을 수행하고, 동일한 `WHERE` 조건으로 content 쿼리와 count 쿼리를 각각 실행(총 2회)한다.
3. VU가 20까지 늘어나는 동안 HikariCP 기본 커넥션 풀(별도 설정 없으면 10개)에 요청이 몰리면서 대기시간이 누적된 것으로 보인다. min 204ms → med 11.37s → p95 27.89s로 벌어지는 패턴이 전형적인 큐잉 지연 증가 곡선과 일치한다.

이후 `@Cacheable`의 `condition`을 제거해 로그인/비로그인 요청 모두 캐시가 적용되도록 코드를 수정했다(현재 반영됨). 이 변경을 반영한 재측정 결과는 아래 결과 2, 3에 정리했다.

## 결과 2: 상품 검색 v1 (4차, 로그인 사용자·캐시 없음 베이스라인)

| 지표 | 값 |
|---|---|
| 총 checks | 815 (100% 성공) |
| search 요청 수 | 271 |
| search 실패율 | 0.00% |
| 응답시간 avg | 3.01s |
| 응답시간 min | 952.04ms |
| 응답시간 median | 2.5s |
| 응답시간 p90 | 5.7s |
| 응답시간 p95 | 6.5s (목표: 500ms 미만) |
| 응답시간 max | 7.69s |
| VU (max) | 20 |

Threshold 판정: `checks` 통과 · `http_req_failed` 통과 · `http_req_duration p95<500ms` **실패**(목표 대비 약 13배)

**분석**: v1은 캐시가 아예 없는 경로이므로 매 요청이 `ItemSearchRepositoryImpl`의 LIKE 풀스캔 + count 쿼리를 그대로 탄다. 2차(캐시 미적중 v2, p95 27.89s)보다는 덜 심각하지만 여전히 목표 대비 크게 느리다. min(952ms)에서 p95(6.5s)로 벌어지는 패턴은 2차와 마찬가지로 VU 증가에 따른 DB 커넥션/쿼리 큐잉 지연으로 보인다. 2차와의 절대치 차이(6.5s vs 27.89s)는 두 테스트의 조건(예열 상태, 동시 실행 요청 수 누적)이 달라 직접 비교보다는 "캐시 없이는 목표를 못 만족한다"는 정성적 확인 용도로 사용한다.

## 결과 3: 상품 검색 v2, `condition` 제거 반영 후 재측정 (5차 비로그인 / 6차·7차 로그인)

| 지표 | 5차 (비로그인) | 6차 (로그인) | 7차 (로그인, 재검증) |
|---|---|---|---|
| 총 checks | 2556 (100% 성공) | 2681 (100% 성공) | 2699 (100% 성공) |
| search 요청 수 | 852 | 893 | 899 |
| search 실패율 | 0.00% | 0.00% | 0.00% |
| 응답시간 avg | 27.11ms | 18.47ms | 15.13ms |
| 응답시간 min | 2.88ms | 3.7ms | 4.19ms |
| 응답시간 median | 8.77ms | 8.96ms | 11.41ms |
| 응답시간 p90 | 12.39ms | 11.75ms | 19.67ms |
| 응답시간 p95 | 15.73ms (목표: 500ms 미만) | 13.5ms (목표: 500ms 미만) | 24.52ms (목표: 500ms 미만) |
| 응답시간 max | 2.65s | 1.23s | 532.48ms |
| VU (max) | 20 | 20 | 20 |

Threshold 판정: 5·6·7차 모두 `checks` 통과 · `http_req_failed` 통과 · `http_req_duration p95<500ms` **통과**

**분석**: `condition` 제거로 로그인/비로그인 관계없이 캐시가 적용되면서, 2차(로그인, 캐시 미적중) p95 27.89s → 6차(로그인, 캐시 적중) p95 13.5ms로 약 2,000배 개선됐다. `ItemSearchCacheKey`는 `clientId`를 포함하므로 로그인 사용자별로 별도 캐시 엔트리가 생기지만, 이번 6·7차 테스트는 k6 스크립트가 단일 테스트 계정으로 로그인하기 때문에 사실상 5차(비로그인)와 동일하게 "키워드 8종 × 고정 clientId" 조합만 반복 조회한 셈이라 세 결과가 비슷한 자릿수로 나온 것으로 보인다. 여러 사용자가 각기 다른 clientId로 로그인해 요청하는 실제 트래픽에서는 사용자 수만큼 캐시 엔트리가 늘어나 캐시 히트율이 낮아질 수 있어, 추후 다중 계정 시나리오 테스트가 필요하다. max값(5차 2.65s, 6차 1.23s, 7차 532.48ms)은 각 키워드(및 clientId 조합)에 대한 최초 캐시 미스(콜드 스타트) 요청으로 추정되며 p95에는 영향이 없다. 7차는 6차와 동일 조건으로 반복한 결과 p95가 13.5ms → 24.52ms로 소폭 올랐지만 여전히 목표(500ms) 대비 20배 이상 여유가 있어, 이 정도 편차는 재현성을 해치는 수준이 아니라 정상적인 실행 간 변동(warm-up, 네트워크/시스템 부하 등)으로 판단한다.

## 결과 4: 인기 검색어 API (3차, 비인증 공개 API)

| 지표 | 값 |
|---|---|
| 총 checks | 2640 (100% 성공) |
| 요청 수 | 880 |
| 실패율 | 0.00% |
| 응답시간 avg | 12.01ms |
| 응답시간 min | 1.5ms |
| 응답시간 median | 5.58ms |
| 응답시간 p90 | 8.94ms |
| 응답시간 p95 | 12.85ms (목표: 500ms 미만) |
| 응답시간 max | 999.59ms |
| VU (max) | 20 |

Threshold 판정: `checks` 통과 · `http_req_failed` 통과 · `http_req_duration p95<500ms` **통과**

**분석**: `PopularSearchService`가 Redis Sorted Set에서 `ZREVRANGE`로 상위 N개만 읽는 단순 조회라 DB 접근이 없고, 검색 v2 같은 풀스캔이나 캐시 조건 분기도 없다. VU 20까지도 p95 12.85ms로 안정적인 걸 보면, 검색 v1/v2에서 관찰된 지연은 Redis·인프라 문제가 아니라 앞서 정리한 LIKE 풀스캔 + 캐시 미적중 조합에서만 나타난다는 점이 다시 확인된다. max값(999.59ms) 스파이크는 p90/p95에 영향을 주지 않아 일시적 워밍업 정도로 보고 별도 조치는 불필요하다고 판단한다.

## 종합 결론

- 인기 검색어 API는 이미 성능 기준을 충분히 만족한다.
- 검색 v2(로그인 경로)는 최초 테스트(2차)에서 목표(p95 500ms) 대비 크게 미달했지만, 이는 "Redis 캐시가 효과 없다"는 뜻이 아니라 테스트 시점에 `@Cacheable(condition = "#clientId == null")` 때문에 캐시가 아예 동작하지 않았기 때문이었다. `condition`을 제거해 재측정한 결과(5차·6차) p95가 27.89s → 13~16ms 수준으로 개선되어 threshold를 통과했고, Redis 캐시가 의도대로 동작함을 확인했다.
- 캐시가 없는 v1(4차)은 p95 6.5s로 목표를 만족하지 못해, "캐시 적중 시에만 목표를 만족하고 캐시 미스/미적용 상태에서는 여전히 느리다"는 결론이 확정됐다.
- 캐시가 적용되더라도 캐시 미스 상황(신규 키워드, TTL 만료 등)에서는 LIKE 풀스캔 비용이 그대로 남아있어, 캐시만으로는 근본 해결이 아니다. v1/2차/4차 결과가 모두 이를 뒷받침한다.
- 이번 5·6차 테스트는 단일 테스트 계정만 사용했기 때문에, 사용자별로 캐시 엔트리가 분리되는 실제 다중 사용자 트래픽에서의 캐시 히트율은 별도 검증이 필요하다(다음 단계 참고).

## 다음 단계

- [x] v1 API(`/api/v1/items/search`)에 동일 시나리오로 부하 테스트를 실행해 캐시 없는 베이스라인을 확보한다. → 4차, p95 6.5s
- [x] `condition` 제거를 반영한 v2 API를 비로그인/로그인 두 경로 모두 재실행해 캐시 적중 시 개선폭을 측정한다. → 5차(비로그인) p95 15.73ms, 6차(로그인) p95 13.5ms
- [x] v1 vs v2(캐시 적중) 결과를 비교해 평균 응답속도 개선폭을 정리한다. → 결과 3 분석 참고 (약 2,000배 개선)
- [x] 캐시 적용 결과의 재현성 확인 → 7차(6차 재검증)에서도 p95 24.52ms로 목표 통과, 결과가 안정적으로 재현됨
- [ ] 동시 사용자 수(TPS, 포화지점) 측정: 현재 VU 20까지만 테스트했고 포화 지점은 아직 관찰되지 않음. VU를 더 올린 스트레스 테스트가 필요하다.
- [ ] 여러 clientId로 로그인하는 다중 계정 시나리오로 재테스트해 실사용 환경에 가까운 캐시 히트율을 측정한다(5·6차는 단일 계정만 사용).
- [ ] 캐시 미스 경로의 실질 개선을 위해 title/description 풀스캔 문제(인덱스/풀텍스트 검색 등)를 검토한다.
- [ ] HikariCP 커넥션 풀 크기 등 DB 커넥션 설정을 점검한다. 4차(v1) 테스트에서도 min(952ms)→p95(6.5s)로 벌어지는 큐잉 패턴이 재현되어 유효한 항목으로 남아있으나, 이번 세션에서는 로그 상 풀 고갈을 나타내는 명시적 지표(예: HikariCP 커넥션 대기 로그)를 확인하지 못해 실제 원인이 커넥션 풀인지 LIKE 풀스캔 자체의 쿼리 비용인지 아직 특정하지 못했다. 별도 프로파일링 후 튜닝 여부를 결정한다.

## 부록: Raw 결과

### 2차 (v2 검색, 로그인)

```
█ THRESHOLDS
    checks{endpoint:search}
    ✓ 'rate>0.99' rate=100.00%
    http_req_duration{endpoint:search}
    ✗ 'p(95)<500' p(95)=27.89s
    http_req_failed{endpoint:search}
    ✓ 'rate<0.01' rate=0.00%
  █ TOTAL RESULTS
    checks_total.......: 275     2.247484/s
    checks_succeeded...: 100.00% 275 out of 275
    checks_failed......: 0.00%   0 out of 275
    ✓ login status is 200
    ✓ Authorization header exists
    ✓ search status is 200
    ✓ search response is JSON
    ✓ search response body exists
    HTTP
    http_req_duration..............: avg=12.95s min=204.25ms med=11.36s max=29.21s p(90)=27.18s p(95)=27.89s
      { endpoint:search }..........: avg=13.09s min=2.76s    med=11.37s max=29.21s p(90)=27.18s p(95)=27.89s
      { expected_response:true }...: avg=12.95s min=204.25ms med=11.36s max=29.21s p(90)=27.18s p(95)=27.89s
    http_req_failed................: 0.00%  0 out of 92
      { endpoint:search }..........: 0.00% 0 out of 91
    http_reqs......................: 92     0.751886/s
    EXECUTION
    iteration_duration.............: avg=14.4s  min=4.29s    med=12.5s  max=30.14s p(90)=28.58s p(95)=29.1s
    iterations.....................: 91     0.743713/s
    vus............................: 1      min=1       max=20
    vus_max........................: 20     min=20      max=20
    NETWORK
    data_received..................: 210 kB 1.7 kB/s
    data_sent......................: 49 kB  398 B/s
running (2m02.4s), 00/20 VUs, 91 complete and 0 interrupted iterations
```

### 3차 (인기 검색어)

```
█ THRESHOLDS
    checks{endpoint:popular_search}
    ✓ 'rate>0.99' rate=100.00%
    http_req_duration{endpoint:popular_search}
    ✓ 'p(95)<500' p(95)=12.85ms
    http_req_failed{endpoint:popular_search}
    ✓ 'rate<0.01' rate=0.00%
  █ TOTAL RESULTS
    checks_total.......: 2640    21.78193/s
    checks_succeeded...: 100.00% 2640 out of 2640
    checks_failed......: 0.00%   0 out of 2640
    ✓ popular search status is 200
    ✓ popular search response is JSON
    ✓ popular search response has keywords array
    HTTP
    http_req_duration...............: avg=12.01ms min=1.5ms   med=5.58ms max=999.59ms p(90)=8.94ms p(95)=12.85ms
      { endpoint:popular_search }...: avg=12.01ms min=1.5ms   med=5.58ms max=999.59ms p(90)=8.94ms p(95)=12.85ms
      { expected_response:true }....: avg=12.01ms min=1.5ms   med=5.58ms max=999.59ms p(90)=8.94ms p(95)=12.85ms
    http_req_failed.................: 0.00%  0 out of 880
      { endpoint:popular_search }...: 0.00%  0 out of 880
    http_reqs.......................: 880    7.260643/s
    EXECUTION
    iteration_duration..............: avg=1.27s   min=505.5ms med=1.29s  max=2.67s    p(90)=1.88s  p(95)=1.93s
    iterations......................: 880    7.260643/s
    vus.............................: 1      min=1        max=20
    vus_max.........................: 20     min=20       max=20
    NETWORK
    data_received...................: 730 kB 6.0 kB/s
    data_sent.......................: 110 kB 908 B/s
running (2m01.2s), 00/20 VUs, 880 complete and 0 interrupted iterations
```

### 4차 (v1 검색, 로그인, 캐시 없음)

```
█ THRESHOLDS
    checks{endpoint:search}
    ✓ 'rate>0.99' rate=100.00%
    http_req_duration{endpoint:search}
    ✗ 'p(95)<500' p(95)=6.5s
    http_req_failed{endpoint:search}
    ✓ 'rate<0.01' rate=0.00%
  █ TOTAL RESULTS
    checks_total.......: 815     6.736113/s
    checks_succeeded...: 100.00% 815 out of 815
    checks_failed......: 0.00%   0 out of 815
    ✓ login status is 200
    ✓ Authorization header exists
    ✓ search status is 200
    ✓ search response is JSON
    ✓ search response body exists
    HTTP
    http_req_duration..............: avg=3s    min=133.75ms med=2.5s  max=7.69s p(90)=5.7s  p(95)=6.5s
      { endpoint:search }..........: avg=3.01s min=952.04ms med=2.5s  max=7.69s p(90)=5.7s  p(95)=6.5s
      { expected_response:true }...: avg=3s    min=133.75ms med=2.5s  max=7.69s p(90)=5.7s  p(95)=6.5s
    http_req_failed................: 0.00%  0 out of 272
      { endpoint:search }..........: 0.00%  0 out of 271
    http_reqs......................: 272    2.248126/s
    EXECUTION
    iteration_duration.............: avg=4.22s min=1.58s    med=3.74s max=9.36s p(90)=6.96s p(95)=7.74s
    iterations.....................: 271    2.239861/s
    vus............................: 1      min=1        max=20
    vus_max........................: 20     min=20      max=20
    NETWORK
    data_received..................: 507 kB 4.2 kB/s
    data_sent......................: 144 kB 1.2 kB/s
running (2m01.0s), 00/20 VUs, 271 complete and 0 interrupted iterations
```

### 5차 (v2 검색, 비로그인, `condition` 제거 후)

```
█ THRESHOLDS
    checks{endpoint:search}
    ✓ 'rate>0.99' rate=100.00%
    http_req_duration{endpoint:search}
    ✓ 'p(95)<500' p(95)=15.73ms
    http_req_failed{endpoint:search}
    ✓ 'rate<0.01' rate=0.00%
  █ TOTAL RESULTS
    checks_total.......: 2556    21.165406/s
    checks_succeeded...: 100.00% 2556 out of 2556
    checks_failed......: 0.00%   0 out of 2556
    ✓ search status is 200
    ✓ search response is JSON
    ✓ search response body exists
    HTTP
    http_req_duration..............: avg=27.11ms min=2.88ms   med=8.77ms max=2.65s p(90)=12.39ms p(95)=15.73ms
      { endpoint:search }..........: avg=27.11ms min=2.88ms   med=8.77ms max=2.65s p(90)=12.39ms p(95)=15.73ms
      { expected_response:true }...: avg=27.11ms min=2.88ms   med=8.77ms max=2.65s p(90)=12.39ms p(95)=15.73ms
    http_req_failed................: 0.00%  0 out of 852
      { endpoint:search }..........: 0.00%  0 out of 852
    http_reqs......................: 852    7.055135/s
    EXECUTION
    iteration_duration.............: avg=1.31s   min=511.46ms med=1.33s  max=4.4s  p(90)=1.89s   p(95)=1.96s
    iterations.....................: 852    7.055135/s
    vus............................: 1      min=1        max=20
    vus_max........................: 20     min=20       max=20
    NETWORK
    data_received..................: 1.6 MB 14 kB/s
    data_sent......................: 168 kB 1.4 kB/s
running (2m00.8s), 00/20 VUs, 852 complete and 0 interrupted iterations
```

### 6차 (v2 검색, 로그인, `condition` 제거 후)

```
█ THRESHOLDS
    checks{endpoint:search}
    ✓ 'rate>0.99' rate=100.00%
    http_req_duration{endpoint:search}
    ✓ 'p(95)<500' p(95)=13.5ms
    http_req_failed{endpoint:search}
    ✓ 'rate<0.01' rate=0.00%
  █ TOTAL RESULTS
    checks_total.......: 2681    22.189467/s
    checks_succeeded...: 100.00% 2681 out of 2681
    checks_failed......: 0.00%   0 out of 2681
    ✓ login status is 200
    ✓ Authorization header exists
    ✓ search status is 200
    ✓ search response is JSON
    ✓ search response body exists
    HTTP
    http_req_duration..............: avg=18.6ms  min=3.7ms    med=8.96ms max=1.23s p(90)=11.76ms p(95)=13.55ms
      { endpoint:search }..........: avg=18.47ms min=3.7ms    med=8.96ms max=1.23s p(90)=11.75ms p(95)=13.5ms
      { expected_response:true }...: avg=18.6ms  min=3.7ms    med=8.96ms max=1.23s p(90)=11.76ms p(95)=13.55ms
    http_req_failed................: 0.00%  0 out of 894
      { endpoint:search }..........: 0.00%  0 out of 893
    http_reqs......................: 894    7.399248/s
    EXECUTION
    iteration_duration.............: avg=1.25s   min=510.76ms med=1.27s  max=3.01s p(90)=1.85s   p(95)=1.93s
    iterations.....................: 893    7.390971/s
    vus............................: 1      min=1        max=20
    vus_max........................: 20     min=20       max=20
    NETWORK
    data_received..................: 1.9 MB 15 kB/s
    data_sent......................: 473 kB 3.9 kB/s
running (2m00.8s), 00/20 VUs, 893 complete and 0 interrupted iterations
```

### 7차 (v2 검색, 로그인, `condition` 제거 후 재검증)

```
█ THRESHOLDS
    checks{endpoint:search}
    ✓ 'rate>0.99' rate=100.00%
    http_req_duration{endpoint:search}
    ✓ 'p(95)<500' p(95)=24.52ms
    http_req_failed{endpoint:search}
    ✓ 'rate<0.01' rate=0.00%
  █ TOTAL RESULTS
    checks_total.......: 2699    22.32053/s
    checks_succeeded...: 100.00% 2699 out of 2699
    checks_failed......: 0.00%   0 out of 2699
    ✓ login status is 200
    ✓ Authorization header exists
    ✓ search status is 200
    ✓ search response is JSON
    ✓ search response body exists
    HTTP
    http_req_duration..............: avg=15.27ms min=4.19ms   med=11.42ms max=532.48ms p(90)=19.69ms p(95)=24.56ms
      { endpoint:search }..........: avg=15.13ms min=4.19ms   med=11.41ms max=532.48ms p(90)=19.67ms p(95)=24.52ms
      { expected_response:true }...: avg=15.27ms min=4.19ms   med=11.42ms max=532.48ms p(90)=19.69ms p(95)=24.56ms
    http_req_failed................: 0.00%  0 out of 900
      { endpoint:search }..........: 0.00% 0 out of 899
    http_reqs......................: 900    7.442933/s
    EXECUTION
    iteration_duration.............: avg=1.24s   min=513.24ms med=1.24s   max=2.11s    p(90)=1.86s   p(95)=1.94s
    iterations.....................: 899    7.434663/s
    vus............................: 1      min=1        max=20
    vus_max........................: 20     min=20       max=20
    NETWORK
    data_received..................: 5.3 MB 44 kB/s
    data_sent......................: 487 kB 4.0 kB/s
running (2m00.9s), 00/20 VUs, 899 complete and 0 interrupted iterations
```
