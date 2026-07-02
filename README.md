# 배추마켓

저장소: https://github.com/pcb2002/Cabbage-Market-10

배추마켓은 회원이 상품을 등록하고, 직거래 또는 경매 방식으로 거래할 수 있는 커머스 백엔드 프로젝트다. 상품 검색, 좋아요, 문의, 리뷰, 실시간 채팅, 경매 입찰을 중심으로 실제 커머스 서비스에서 필요한 인증, 권한, 동시성 제어, 문서화 흐름을 함께 다룬다.

## 프로젝트 목표

- 상품 등록부터 거래, 문의, 채팅, 리뷰까지 이어지는 커머스 도메인 백엔드 구현
- JWT 기반 인증과 Redis 기반 토큰 차단으로 인증 API 안정성 확보
- 경매 입찰처럼 동시에 요청이 몰리는 기능에서 데이터 정합성 보장
- API 명세, ERD, 보안 정책, 비즈니스 규칙을 문서로 관리
- PR, 코드 리뷰, 테스트 중심의 협업 흐름 유지

## 기술 스택

| 구분 | 기술 |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 4.1, Spring MVC, Spring Security, Spring WebSocket |
| Persistence | Spring Data JPA, QueryDSL, MySQL |
| Cache/Infra | Redis, Redisson |
| Auth | JWT Access Token, Refresh Token Cookie, Redis blacklist |
| Storage | AWS S3 SDK |
| Test | JUnit 5, Spring Boot Test, H2, embedded Redis |
| Build/CI | Gradle, GitHub Actions |
| Local Infra | Docker Compose, MySQL 8.4, Redis 7.4 |

## 주요 기능

| 도메인 | 기능 |
|---|---|
| 인증 | 회원가입, 로그인, 로그아웃, 토큰 재발급, Access Token 블랙리스트 |
| 회원 | 내 정보 조회·수정, 공개 프로필, 팔로우/팔로워 |
| 상품 | 등록, 임시저장, 게시, 목록·상세 조회, 수정, 상태 변경, 삭제 |
| 상품 이미지 | 이미지 업로드, 대표 이미지 설정, 이미지 삭제 |
| 검색 | 상품 검색, 가격·카테고리·상태·거래 방식 필터, 정렬 |
| 좋아요 | 상품 좋아요 토글, 내 관심목록 조회 |
| 경매 | 경매 상품 입찰, 현재 최고 입찰가·입찰자 갱신 |
| 문의 | 상품 문의 작성·조회·수정·삭제, 판매자 답변 |
| 리뷰 | 거래 완료 상품 리뷰 작성·조회·수정·삭제 |
| 채팅 | 상품 기반 채팅방, WebSocket/STOMP 메시지 송수신, 읽음 처리 |

## 핵심 구현

### 경매 입찰 동시성 제어

경매 입찰은 같은 상품에 여러 사용자가 동시에 입찰할 수 있으므로, 현재 입찰가와 최고 입찰자 갱신을 하나의 임계 구역으로 처리해야 한다. 배추마켓은 Redis 기반 Redisson 분산 락을 사용해 상품 단위로 입찰 요청을 직렬화한다.

| 항목 | 내용 |
|---|---|
| 적용 위치 | `AuctionFacade.bidItem` |
| 락 키 | `auction:bid:{itemId}` |
| 락 구현 | Redisson `RLock.tryLock` |
| 대기/점유 시간 | `app.auction.redis-lock.wait-time-millis`, `lease-time-millis` |
| 실패 처리 | 제한 시간 내 락 획득 실패 시 `AUCTION_BID_LOCK_FAILED` |
| 검증 | 동시 입찰 통합 테스트, 락 획득·실패·unlock 단위 테스트 |

락 방식 비교:

| 방식 | 관리 주체 | 보호 범위 | 장점 | 한계 |
|---|---|---|---|---|
| 낙관적 락 | DB/JPA `@Version` | 단일 row update 충돌 감지 | 읽기 많고 충돌이 적을 때 효율적 | 충돌 후 재시도 로직 필요 |
| 비관적 락 | DB row lock | DB 트랜잭션 내부 | 충돌 자체를 차단 | 락 대기, 처리량 저하, DB 의존 |
| 분산 락 | Redis | 비즈니스 로직 전체 | 다중 서버 환경에서 상품 단위 임계 구역 보호 | Redis 가용성에 영향 |

이 프로젝트는 서버 확장 가능성과 입찰 검증부터 갱신까지의 전체 흐름 보호가 필요하다고 보고 Redisson 분산 락을 선택했다. 상세 결정은 [ADR-002](docs/adr/ADR-002-auction-bid-concurrency.md)에 기록한다.

### 검색과 캐싱 미션

상품 검색 API는 제목·설명 키워드, 카테고리, 거래 상태, 거래 방식, 상품 상태, 가격 범위, 정렬 조건을 지원한다.

| 항목 | 현재 상태 |
|---|---|
| 상품 검색 API | `GET /api/v1/items/search` 구현 |
| 인기 검색어 API | `GET /api/search/popular` 명세화 |
| 캐싱 적용 | 측정 및 적용 결과 기록 필요 |
| 성능 비교 | 캐시 적용 전/후 응답 시간, TPS 측정 후 기록 필요 |

프로젝트 요구사항상 캐시 전략과 성능 비교는 README에 기록해야 한다. 아직 저장소에서 확인 가능한 측정 결과가 없으므로, 수치가 준비되면 아래 형식으로 추가한다.

```text
검색 API 성능 비교
- 조건: 데이터 수, 부하 도구, 요청 파라미터, 반복 횟수
- 캐시 미적용: 평균 응답 시간 / p95 / TPS
- 캐시 적용: 평균 응답 시간 / p95 / TPS
- 캐시 키: 예) search:items:{normalized-query}:{page}:{size}:{sort}
- TTL/무효화 정책:
- 개선 결과:
```

### 실시간 채팅

채팅은 `/ws/chat` WebSocket 연결 후 STOMP destination으로 메시지를 주고받는다. HTTP 필터가 아닌 STOMP 인터셉터에서 인증을 처리하고, 채팅방 참여자만 구독과 메시지 조회가 가능하도록 제한한다.

| 기능 | 경로 |
|---|---|
| WebSocket 연결 | `/ws/chat` |
| 메시지 전송 | `SEND /api/chat-rooms/{chatRoomId}/messages` |
| 메시지 목록 조회 | `GET /api/chat-rooms/{chatRoomId}/messages` |
| 메시지 읽음 처리 | `POST /api/chat-rooms/{chatRoomId}/read` |

## 아키텍처

```text
Controller
  -> Facade
    -> Domain Service
      -> Repository
        -> MySQL / Redis / S3
```

- Controller는 HTTP 요청·응답과 인증 사용자 추출을 담당한다.
- Facade는 여러 도메인 서비스가 함께 필요한 유스케이스를 조정한다.
- Domain Service는 도메인 규칙과 상태 변경을 담당한다.
- Repository는 JPA, QueryDSL, Redis, S3 등 외부 저장소 접근을 담당한다.

패키지 구조:

```text
src/main/java/com/example/cabbagemarket10
├── application/facade
├── common
├── domain
│   ├── auth
│   ├── auction
│   ├── category
│   ├── chat
│   ├── client
│   ├── follow
│   ├── inquiry
│   ├── item
│   ├── itemImage
│   ├── itemLike
│   ├── review
│   └── search
└── global
```

## API

REST API prefix는 `/api`이며, WebSocket 연결 경로는 `/ws/chat`이다.

대표 API:

| 기능 | Method | Path |
|---|---:|---|
| 회원가입 | POST | `/api/auth/signup` |
| 로그인 | POST | `/api/auth/login` |
| 내 정보 조회 | GET | `/api/clients/me` |
| 상품 등록 | POST | `/api/items` |
| 상품 목록 조회 | GET | `/api/items` |
| 상품 검색 | GET | `/api/v1/items/search` |
| 상품 좋아요 | POST | `/api/items/{itemId}/likes` |
| 입찰하기 | POST | `/api/items/{itemId}/auction-status/bid` |
| 채팅방 생성 | POST | `/api/items/{itemId}/chat-rooms` |
| 메시지 목록 조회 | GET | `/api/chat-rooms/{chatRoomId}/messages` |

전체 API 명세는 [docs/api.md](docs/api.md)를 기준으로 관리한다.

## 로컬 실행

### 사전 준비

- JDK 17
- Docker Desktop 또는 Docker Engine
- Git

### 환경 변수

로컬 실행 예시는 [env/local.env.example](env/local.env.example)에 있다. 처음 실행할 때는 예시 파일을 기준으로 `env/local.env`를 준비한다.

주요 값:

```properties
SPRING_PROFILES_ACTIVE=local
LOCAL_DB_URL=jdbc:mysql://127.0.0.1:3306/cabbage_market?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
MYSQL_USER=cabbage
MYSQL_PASSWORD=cabbage
LOCAL_REDIS_HOST=127.0.0.1
LOCAL_REDIS_PORT=6379
LOCAL_REDIS_PASSWORD=cabbage-local-redis
JWT_SECRET=change-this-local-jwt-secret-at-least-32-bytes
```

### MySQL, Redis 실행

```bash
docker compose -f compose.local.yaml --env-file env/local.env up -d
```

### 애플리케이션 실행

Windows:

```powershell
$env:SPRING_PROFILES_ACTIVE="local"
.\gradlew.bat bootRun
```

macOS/Linux:

```bash
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

## 테스트

전체 테스트:

```bash
./gradlew test
```

Windows:

```powershell
.\gradlew.bat test
```

테스트 원칙:

- 테스트 프로필은 H2와 embedded Redis를 사용해 운영 MySQL, Redis, S3에 직접 접근하지 않는다.
- 테스트 메서드명은 한글로 작성해 기대 동작을 드러낸다.
- 동시성 테스트는 단순 단위 테스트만으로 판단하지 않고, 입찰 흐름을 통합 테스트로 검증한다.

## CI

GitHub Actions는 PR 대상 브랜치가 `develop`, `main`일 때 Gradle build를 수행한다. 현재 워크플로우는 빌드와 테스트 중심이며, Docker 이미지 push와 자동 배포는 별도 확장 대상이다.

워크플로우: [.github/workflows/ci.yml](.github/workflows/ci.yml)

## 문서

| 문서 | 책임 |
|---|---|
| [docs/README.md](docs/README.md) | 문서 인덱스 |
| [docs/workflow.md](docs/workflow.md) | 이슈, 브랜치, 커밋, PR, 리뷰 흐름 |
| [docs/convention.md](docs/convention.md) | 기술 스택, 계층, 구현 규칙, 테스트, 네이밍 |
| [docs/api.md](docs/api.md) | REST API와 WebSocket 경로 |
| [docs/ERD.md](docs/ERD.md) | Entity, 컬럼, 관계, 삭제 정책 |
| [docs/security.md](docs/security.md) | 인증, 권한, 민감정보, WebSocket 보안 |
| [docs/business-rules.md](docs/business-rules.md) | 도메인 규칙과 상태 기준 |
| [docs/adr/README.md](docs/adr/README.md) | 중요한 기술 결정 기록 |
| [docs/plans/README.md](docs/plans/README.md) | 장기 작업 진행 상태 |

## 협업 규칙

- 코드, 설정, 문서, Git 작업 전 [AGENTS.md](AGENTS.md)를 확인한다.
- 기능 구현 흐름은 [docs/workflow.md](docs/workflow.md)를 따른다.
- API 계약 변경은 [docs/api.md](docs/api.md)를 함께 갱신한다.
- DB 스키마 변경은 [docs/ERD.md](docs/ERD.md)를 함께 갱신한다.
- 인증, 권한, 민감정보 변경은 [docs/security.md](docs/security.md)를 확인하고 관련 테스트를 실행한다.
- PR 없이 `develop`, `main`에 직접 머지하지 않는다.
- 코드 리뷰는 버그, 회귀, 테스트 누락, 문서 불일치를 중심으로 진행한다.

## 제출 및 발표 준비 체크리스트

- GitHub 저장소 링크와 zip 파일 준비
- 핵심 기능 시연 영상 준비
- API 명세, ERD, README 최신화
- 경매 입찰 동시성 제어 전/후 테스트 결과 정리
- 검색 캐싱 적용 전/후 성능 측정 결과 정리
- 트러블슈팅 기록 정리
