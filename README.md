# Cabbage Market 10

Cabbage Market 10은 중고 거래, 경매 입찰, 상품 검색, 채팅, 문의, 리뷰를 제공하는 Spring Boot 기반 마켓 서비스입니다. 단순 CRUD를 넘어 인증, 캐시, 분산 락, 파일 저장소, 실시간 메시징처럼 실제 서비스에서 자주 만나는 운영 관심사를 함께 다룹니다.

저장소: https://github.com/pcb2002/Cabbage-Market-10

## 주요 기능

| 영역 | 기능 |
|---|---|
| 인증 | 회원가입, 로그인, Access Token JWT 인증, Refresh Token Cookie 재발급, 로그아웃 블랙리스트 |
| 회원 | 내 정보 조회·수정, 공개 프로필 조회, 판매글·관심목록 조회 |
| 상품 | 상품 등록, 임시저장, 게시, 수정, 상태 변경, 삭제, 목록·상세 조회 |
| 이미지 | AWS S3 상품 이미지 업로드, 대표 이미지 지정, 이미지 삭제, 실패 시 보상 삭제 |
| 검색 | QueryDSL 기반 동적 검색, Redis 기반 검색 결과 캐시, Redis Sorted Set 인기 검색어 |
| 좋아요·팔로우 | 상품 좋아요 토글, 회원 팔로우·언팔로우 |
| 문의·리뷰 | 상품 문의와 판매자 답변, 거래 완료 기반 리뷰 작성·조회 |
| 경매 | 현재가 입찰, 입찰 이력 저장, Redisson 분산 락 기반 동시성 제어 |
| 채팅 | STOMP WebSocket 연결, 채팅방 메시지 송수신, 메시지 조회 |

## 기술 스택

| 구분 | 기술 |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 4.1.0 |
| Web | Spring Web MVC, Spring WebSocket, STOMP |
| Persistence | Spring Data JPA, Hibernate, QueryDSL, MySQL, H2(test) |
| Cache / Lock | Spring Cache, Redis, Redisson, Caffeine(local/test cache) |
| Security | Spring Security, JWT(JJWT), BCrypt, CSRF Cookie |
| Storage | AWS SDK S3 |
| Test | JUnit 5, Spring Boot Test, MockMvc, embedded Redis |
| Build | Gradle |

## 아키텍처

```mermaid
flowchart LR
    Client["Client / Browser"] --> API["Spring MVC REST API"]
    Client --> WS["STOMP WebSocket /ws/chat"]

    API --> Security["Security Filter Chain"]
    Security --> Facade["Facade / Service Layer"]
    WS --> StompAuth["StompAuthInterceptor"]
    StompAuth --> Facade

    Facade --> Domain["Domain Services"]
    Domain --> Jpa["Spring Data JPA / QueryDSL"]
    Domain --> Redis["Redis Cache / Blacklist / Lock"]
    Domain --> S3["AWS S3"]

    Jpa --> MySQL["MySQL"]
    Redis --> RedisServer["Redis Server"]
    S3 --> Bucket["S3 Bucket"]
```

계층은 Controller, Facade/Service, Repository 중심으로 나뉩니다. Controller는 HTTP 요청·응답과 인증 사용자 전달에 집중하고, Facade는 여러 도메인 흐름을 조합하며, Service는 도메인 규칙을 처리합니다.

## 핵심 설계 결정

- 인증은 Access Token Stateless JWT와 Refresh Token HttpOnly Cookie를 함께 사용합니다.
- 로그아웃된 Access Token의 `jti`와 정지 계정 마커는 Redis에 TTL과 함께 저장합니다.
- 상품 검색 v2는 Redis remote cache를 사용하고, 상품 변경·좋아요·입찰 성공 이후 트랜잭션 커밋 뒤 캐시를 무효화합니다.
- 인기 검색어는 Redis Sorted Set으로 일별 집계하고, 짧은 시간 반복 검색은 dedup key로 중복 집계를 줄입니다.
- 경매 입찰은 상품 단위 Redisson 분산 락으로 직렬화해 다중 인스턴스 환경에서도 현재가 갱신 순서를 보호합니다.
- 상품 이미지는 S3에 저장하며, 업로드 중 DB 저장이 실패하면 이미 업로드한 S3 객체를 보상 삭제합니다.

자세한 결정 배경은 [docs/adr/README.md](docs/adr/README.md)를 참고하세요.

## k6 성능 테스트와 검색 캐시

### 결론

검색 API는 캐시를 적용할 가치가 높습니다. item 100만 건 기준으로 캐시 없는 v1 검색은 p95 6.5s로 목표인 500ms를 크게 넘겼고, Redis 캐시가 적중한 v2 검색은 p95 13~25ms로 목표를 통과했습니다. 원본 수치는 별도 `REPORT.md`를 참고합니다.

### 왜 검색 API에 캐시를 적용했는가

캐시 적용 여부는 "쿼리가 비싼가"와 "결과가 반복 재사용되는가" 두 가지로 판단했습니다.

첫째, 검색 쿼리 자체가 비쌉니다. `ItemSearchRepositoryImpl`은 제목·설명에 `containsIgnoreCase` 기반 `LIKE` 조건을 쓰고, 페이징을 위해 content 쿼리와 count 쿼리를 함께 실행합니다. 인덱스를 타기 어려운 풀스캔에 가까운 구조라 데이터가 커질수록, 동시 요청이 늘어날수록 응답 시간이 급격히 나빠집니다. v1의 p95 6.5s가 이를 보여줍니다.

둘째, 검색은 캐시로 이득을 볼 수 있는 접근 패턴을 가집니다. `keyword=아이폰`, `tradeStatus=ON_SALE`, `page=0`, `size=20` 같은 조합은 여러 사용자가 짧은 시간에 반복 요청할 수 있습니다. 또한 검색 결과는 몇 분 정도 늦게 갱신돼도 무방한 읽기 중심 데이터라 강한 일관성이 필요하지 않습니다. "비싼 쿼리 + 반복되는 요청 + 약한 일관성 요구"라는 조건이 맞아 검색 API는 캐시 적중 시 이득이 큽니다.

반대로 상품 상세 조회는 조회수 증가라는 쓰기 부작용이 있어 캐시 적용을 보류했고, 인기 검색어는 이미 Redis Sorted Set 조회만으로 충분히 빨라 p95 12.85ms를 기록했으므로 별도 캐시가 불필요했습니다. 즉 "느리고 반복되는 읽기"라는 조건에 검색 API가 가장 잘 맞았습니다.

### k6 결과 요약

| 대상 | 조건 | p95 | 판정 |
|---|---|---:|---|
| 검색 v1 | 캐시 없음 | 6.5s | 실패 |
| 검색 v2 | 캐시 미적중(로그인) | 27.89s | 실패 |
| 검색 v2 | 캐시 적중(비로그인) | 15.73ms | 통과 |
| 검색 v2 | 캐시 적중(로그인, 실험) | 13.5ms / 24.52ms | 통과 |
| 인기 검색어 | Redis Sorted Set 조회 | 12.85ms | 통과 |

캐시 미스는 여전히 DB 비용을 그대로 받습니다. 따라서 캐시 적용 이후의 핵심은 적중률을 얼마나 높이느냐입니다.

현재 `develop`의 `SearchService.searchItemsV2`는 `@Cacheable(condition = "#clientId == null")` 조건으로 비로그인 요청만 캐시합니다. 로그인 v2 결과인 13.5ms / 24.52ms는 이 조건을 제거한 실험 결과이며, 현재 코드 그대로라면 로그인 검색은 캐시가 적중하지 않습니다.

### 테스트 대상

- `search-api.js`: `/api/v1/items/search`, `/api/v2/items/search`
- `popular-search-api.js`: `/api/search/popular`
- VU 프로파일: 5명(30s) -> 20명(1m) -> 0명(30s)
- Threshold: `checks rate>0.99`, `http_req_duration p95<500ms`, `http_req_failed rate<0.01`

```bash
k6 run -e API_VERSION=v1 performance/k6/search-api.js
k6 run -e API_VERSION=v2 -e ANONYMOUS=true performance/k6/search-api.js
k6 run performance/k6/popular-search-api.js

# 로그인 검색
k6 run -e API_VERSION=v2 -e K6_EMAIL=user@example.com -e K6_PASSWORD='password123!' performance/k6/search-api.js
```

### 캐시 전략

Cache-aside(Lazy Loading)를 선택했습니다. 조회 시 캐시를 먼저 확인하고, 없으면 DB 조회 후 캐시에 채웁니다. Write-through나 write-back을 쓰지 않은 이유는 검색 결과가 원본 데이터가 아니라 필터·정렬로 만든 파생 데이터이기 때문입니다. 쓰기 시점마다 가능한 모든 검색 조건의 결과를 미리 계산하는 것은 조합이 너무 많아 비효율적입니다.

캐시 key는 캐시 이름 `itemSearchV2`와 Redis prefix `item-search:v2:`를 사용합니다. `ItemSearchCacheKey`는 `clientId`, `keyword`, `categoryId`, `tradeStatus`, `tradeType`, `conditionType`, `likedOnly`, `minPrice`, `maxPrice`, `page`, `size`, `sort`를 포함해 서로 다른 조건이 캐시를 공유하지 않도록 합니다. `keyword`는 trim 후 빈 문자열을 `null`로 정규화합니다. 단, 현재는 `condition = "#clientId == null"` 때문에 실제 캐시 엔트리의 `clientId`는 대부분 `null`입니다.

무효화는 TTL 5분과 이벤트 기반 전체 삭제를 함께 사용합니다. 상품 생성·게시·수정·상태 변경·삭제, 좋아요 토글, 입찰 성공 시 `SearchCacheEvictionService.evictItemSearchV2AfterCommit()`가 트랜잭션 커밋 후 `itemSearchV2` 전체를 비웁니다. 조건별로 정밀하게 무효화하지 않고 전체 삭제를 택한 이유는 검색 조건 조합 수가 많아 정확한 영향 범위 계산이 복잡하기 때문입니다. 단순성과 정합성을 우선한 선택이며, 쓰기가 잦으면 hit rate가 낮아질 수 있는 트레이드오프가 있습니다.

Redis를 쓰는 이유는 scale-out 환경에서 캐시를 공유하기 위해서입니다. 로컬 캐시는 서버마다 캐시가 달라져 동일 조건도 서버별로 hit/miss가 갈리고 무효화도 서버마다 맞춰야 합니다. Redis는 서버 간 공유되는 remote cache라 일관성 유지가 쉽고, 이 프로젝트에서는 이미 경매 락, 토큰 블랙리스트, 인기 검색어에 사용하고 있어 추가 인프라 부담도 작습니다.

```yaml
app:
  cache:
    item-search-v2:
      type: redis
      ttl: 5m
      maximum-size: 1000
      key-prefix: "item-search:v2:"
```

### 남은 과제

- 로그인 검색 캐시 적용 여부 결정: 적용 시 `condition = "#clientId == null"` 제거 또는 변경 필요
- 다중 계정 k6 시나리오로 사용자별 key 분산 시 hit rate 측정
- `LIKE` 풀스캔 대체용 full-text index 또는 검색 엔진 검토
- 운영 환경 Redis `maxmemory` 정책 명시

## 도메인 모델

주요 엔티티는 `Client`, `Category`, `Item`, `ItemImage`, `ItemLike`, `InquiryLog`, `Follow`, `ChatRoom`, `ChatMessage`, `Review`, `AuctionStatus`, `AuctionBidHistory`입니다.

```mermaid
erDiagram
    client ||--o{ item : sells
    category ||--o{ item : classifies
    item ||--o{ itemImage : has
    item ||--o{ itemLike : receives
    client ||--o{ itemLike : likes
    item ||--o{ inquiry : has
    inquiry |o--|| inquiry : answer
    client ||--o{ follow : follows
    item ||--o{ chatRoom : discussed_in
    chatRoom ||--o{ chatMessage : contains
    item ||--o{ review : reviewed_by
    item |o--|| auctionStatus : auction
    auctionStatus ||--o{ auctionBidHistory : records
```

전체 컬럼, 관계, 삭제 정책은 [docs/ERD.md](docs/ERD.md)에 정리되어 있습니다.

## 프로젝트 구조

```text
src/main/java/com/example/cabbagemarket10
├── application/facade        # 여러 도메인 흐름을 조합하는 유스케이스 계층
├── domain
│   ├── auth                  # 인증, 토큰, 쿠키, 블랙리스트
│   ├── auction               # 경매 상태, 입찰 이력, 분산 락 입찰 Facade
│   ├── category              # 카테고리
│   ├── chat                  # 채팅방, 메시지, WebSocket 메시지
│   ├── client                # 회원 프로필, 내 정보
│   ├── follow                # 팔로우
│   ├── inquiry               # 상품 문의와 답변
│   ├── item                  # 상품 게시글, 좋아요, 목록·상세
│   ├── itemImage             # 상품 이미지
│   ├── itemLike              # 좋아요 엔티티
│   ├── review                # 리뷰
│   └── search                # 상품 검색, 인기 검색어
└── global
    ├── common                # 공통 설정, 응답, BaseEntity
    ├── config/cache          # 검색 캐시 설정과 무효화
    ├── exception             # 공통 예외 처리
    ├── security              # Security, JWT, CSRF
    └── util                  # S3 업로드 유틸
```

## 로컬 실행

### 1. 준비물

- JDK 17
- Docker 또는 Docker Desktop
- Gradle Wrapper(`gradlew`, `gradlew.bat`)

### 2. 환경 변수 파일 생성

```bash
cp env/local.env.example env/local.env
```

`env/local.env`에 로컬 실행 값을 채웁니다. S3 설정은 `application-local.yml`에서 필요하므로 상품 이미지 기능까지 실행하려면 아래 값도 추가합니다.

```properties
AWS_ACCESS_KEY=your-access-key
AWS_SECRET_KEY=your-secret-key
AWS_REGION=ap-northeast-2
AWS_S3_BUCKET=your-bucket
```

로컬에서 실제 S3를 쓰지 않을 경우에도 애플리케이션 컨텍스트 생성을 위해 placeholder 값을 넣어야 합니다.

### 3. MySQL과 Redis 실행

```bash
docker compose -f compose.local.yaml up -d
```

기본 포트는 MySQL `3306`, Redis `6379`입니다. 포트와 계정은 `env/local.env`에서 바꿀 수 있습니다.

### 4. 애플리케이션 실행

Windows:

```powershell
.\gradlew.bat bootRun --args='--spring.profiles.active=local'
```

macOS/Linux:

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

정상 실행 후 기본 정적 페이지는 `http://localhost:8080/`에서 확인할 수 있습니다.

## 테스트

전체 테스트:

```powershell
.\gradlew.bat test
```

테스트 기본 프로필은 `test`입니다. 일반 테스트는 H2와 local cache를 사용하고, Redis 동작 검증이 필요한 통합 테스트는 embedded Redis로 격리합니다.

Redis cache 또는 인기 검색어 통합 테스트처럼 별도 프로필이 필요한 경우:

```powershell
.\gradlew.bat test -Dspring.profiles.active=redis-test
```

## API와 인증

REST API prefix는 `/api`, WebSocket 연결 path는 `/ws/chat`입니다. REST 응답은 공통적으로 아래 형태를 사용합니다.

```json
{
  "status": 200,
  "data": {}
}
```

오류 응답:

```json
{
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "요청값 검증에 실패했습니다.",
  "data": null
}
```

인증 API 흐름:

1. `POST /api/auth/login`
2. 응답 헤더 `Authorization: Bearer {accessToken}` 확인
3. Refresh Token은 `refresh_token` HttpOnly Cookie로 저장
4. 인증 필요 API 요청 시 `Authorization` 헤더 전달
5. 토큰 재발급은 `POST /api/auth/refresh`
6. 로그아웃은 `POST /api/auth/logout`

상세 API 목록, 요청 검증, 오류 코드는 [docs/api.md](docs/api.md)를 참고하세요.

## 설정 요약

| 설정 | 기본값 | 설명 |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `local` | 로컬 실행 프로필 |
| `LOCAL_DB_URL` | `jdbc:mysql://127.0.0.1:3306/cabbage_market...` | MySQL 연결 URL |
| `LOCAL_REDIS_HOST` | `127.0.0.1` | Redis host |
| `LOCAL_REDIS_PORT` | `6379` | Redis port |
| `JWT_SECRET` | 직접 설정 | JWT 서명 비밀키 |
| `JWT_ACCESS_TOKEN_EXPIRE_SECONDS` | `3600` | Access Token 만료 시간 |
| `JWT_REFRESH_TOKEN_EXPIRE_SECONDS` | `1209600` | Refresh Token 만료 시간 |
| `ALLOWED_ORIGINS` | `http://localhost:5173` | CORS 허용 origin |
| `AUCTION_REDIS_LOCK_ENABLED` | `true` | 경매 입찰 Redisson 락 활성화 |
| `AWS_S3_BUCKET` | 직접 설정 | 상품 이미지 S3 bucket |

## 문서

| 문서 | 내용 |
|---|---|
| [docs/workflow.md](docs/workflow.md) | 이슈, 브랜치, 커밋, PR, 리뷰 흐름 |
| [docs/convention.md](docs/convention.md) | 기술 스택, 계층, 구현 규칙, 테스트, 네이밍 |
| [docs/api.md](docs/api.md) | REST API와 WebSocket 계약 |
| [docs/ERD.md](docs/ERD.md) | Entity, 컬럼, 관계, 삭제 정책 |
| [docs/security.md](docs/security.md) | 인증, 권한, 민감정보, WebSocket 보안 |
| [docs/business-rules.md](docs/business-rules.md) | 도메인 규칙과 상태 기준 |
| [docs/adr/README.md](docs/adr/README.md) | 확정된 기술 결정 기록 |

## 작업 규칙

- 코드, 설정, 문서, Git 작업 전 [AGENTS.md](AGENTS.md)를 확인합니다.
- 기능 구현 흐름은 [docs/workflow.md](docs/workflow.md)를 따릅니다.
- API 계약 변경 시 [docs/api.md](docs/api.md)를 함께 갱신합니다.
- DB 스키마, 관계, 영속 enum 변경 시 [docs/ERD.md](docs/ERD.md)를 함께 갱신합니다.
- 인증, 권한, 민감정보 정책 변경 시 [docs/security.md](docs/security.md)를 함께 갱신합니다.
