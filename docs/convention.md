# 개발 표준

코드 구조, 구현 규칙, 네이밍, 테스트, 문서 동기화 기준을 정의한다.

---

## 기술 스택

Java 17 / Spring Boot 4.1.x / Spring MVC / Spring WebSocket / Spring Security / Spring Data JPA / QueryDSL / MySQL / Redis / Gradle

---

## 패키지 구조

```text
src/main/java/com/example/cabbagemarket10
├── CabbageMarket10Application.java
├── domain
│   ├── auth          # 로그인, 토큰 발급
│   ├── client        # 회원, 마이페이지, 팔로우
│   ├── category      # 카테고리
│   ├── item          # 상품 게시글
│   ├── itemimage     # 상품 이미지
│   ├── itemlike      # 상품 좋아요
│   ├── inquiry       # 상품 문의·답변
│   ├── follow        # 팔로우
│   ├── chat          # 채팅방·메시지
│   ├── review        # 회원 리뷰
│   └── auction       # 경매 입찰
└── global
    ├── config        # Spring 설정
    ├── error         # 전역 예외 처리
    ├── security      # JWT 필터, Security 설정
    └── common        # 공통 응답, 유틸
```

각 도메인은 `com.example.cabbagemarket10.domain.{domain}` 아래에 두고, 필요에 따라 `controller`, `service`, `repository`, `domain`, `exception`, `dto` 하위 패키지를 둔다.
Controller와 직접 통신하는 DTO는 `dto/request`, `dto/response`로 분리한다.

```text
domain/{domain}
├── controller
├── service
├── repository
├── domain
├── exception
└── dto
    ├── request
    └── response
```

---

## 계층 책임

```text
Controller → Service → Repository → DB
Controller → WebSocket Handler → Service → Repository → DB
```

| 계층 | 책임 | 금지 |
|---|---|---|
| Controller | HTTP 요청 검증, 인증 사용자 추출, DTO 변환 | 비즈니스 로직, DB 직접 접근 |
| WebSocket Handler | 연결, 구독, 메시지 라우팅 | 도메인 규칙 직접 처리 |
| Service | 트랜잭션, 권한 검증, 상태 변경, 비즈니스 규칙 | HTTP 의존, 응답 직접 생성 |
| Repository | Entity 조회·영속화 | 비즈니스 판단 |
| Entity | 도메인 상태, 최소 행위 | 외부 의존성 |
| DTO | API 요청·응답 계약 | 비즈니스 로직 |

---

## Java·Spring 규칙

- Service 메서드는 하나의 유스케이스를 표현한다.
- Controller에 비즈니스 로직을 두지 않는다.
- Controller 응답은 `CommonResponse.success()` 또는 `CommonResponse.fail()`을 생성한 뒤, `toResponseEntity()`로 반환한다.
- Controller에서 `ResponseEntity.ok(...)`, `ResponseEntity.status(...).body(...)`를 직접 사용하지 않는다.
- 생성자 주입만 사용한다.
- `@Data`를 Entity에 사용하지 않는다. `@Getter`, `@Builder`만 허용.
- Entity에는 클래스 레벨 `@Setter`를 사용하지 않는다. 상태 변경은 의미 있는 메서드로 표현한다.
- 의미 없는 범용 이름(`Util`, `Manager`, `Data`, `Helper`)을 남용하지 않는다.
- Java 코드는 Google Java Style을 기본 기준으로 삼는다.

---

## DTO·예외

- Request DTO와 Response DTO를 분리한다.
- Controller와 직접 통신하는 DTO는 `Dto` 접미사를 쓰지 않고 `Request`, `Response`를 사용한다.
- Request DTO는 `{domain}.dto.request`, Response DTO는 `{domain}.dto.response`에 둔다.
- Entity를 API 응답으로 직접 반환하지 않는다.
- Request DTO는 Bean Validation으로 검증한다. `@NotNull`, `@NotBlank`, `@Size` 등.
  - primitive 타입은 `null` 검증이 불가하므로 Wrapper 타입으로 선언하고 `@NotNull`을 적용한다.
  - 예: `long initialPrice` → `Long initialPrice` + `@NotNull @PositiveOrZero`
- Request DTO 검증 메시지는 한글 문장과 마침표로 통일한다.
  - 예: `"이메일은 필수입니다."`, `"상품명은 최대 100자까지 입력할 수 있습니다."`
- Request DTO에는 Entity 변환용 정적 팩토리 메서드를 두지 않는다. Entity 변환은 Service 책임이다.
- Response DTO는 `from(Entity)` 정적 팩토리 메서드로 생성한다. 인자가 2개 이상이면 `of(...)`도 허용한다.
- Response DTO 필드는 불변으로 관리하고, null 가능 필드는 필요 시 응답에서 제외한다.
- 도메인 예외는 `GlobalExceptionHandler`에서 HTTP 응답으로 변환한다.
- 공통 예외와 공통 응답은 `global` 하위 패키지에서 관리한다.
- 내부 구현 메시지와 stack trace를 응답에 노출하지 않는다.
- 오류 응답 형식은 전체 API에서 일관되게 유지한다.

---

## JPA

- 연관관계는 필요한 방향만 매핑한다. 양방향은 신중하게 결정한다.
- 연관관계 fetch는 기본 LAZY로 둔다. EAGER가 필요하면 근거를 주석으로 남긴다.
- N+1이 예상되면 fetch join 으로 해결한다 단, 검색 기능 쪽 문제는 QueryDSL 사용하여 해결한다.
- 컬렉션을 JSON으로 직접 직렬화하지 않는다. DTO로 변환한다.
- `equals`/`hashCode`에 변경 가능한 필드나 연관관계를 포함하지 않는다.
- Soft Delete 대상은 일반 조회 쿼리에서 반드시 제외한다.
  - `WHERE deleted_at IS NULL` 또는 `WHERE is_deleted = false`
- 복합키 Entity는 `@EmbeddedId`로 식별자 클래스를 분리한다.

---

## 트랜잭션

- 트랜잭션 경계는 Service에 둔다. Controller에 `@Transactional` 금지.
- 데이터 변경이 없는 조회 메서드는 `@Transactional(readOnly = true)`를 적용한다.
- 카운트 컬럼(`like_count`, `inquiry_count`, `view_count`) 변경은 원본 행 변경과 같은 트랜잭션에서 처리한다.
- 동시성 영향을 받는 입찰·좋아요·팔로우는 동시성 제어 전략을 정한 뒤 구현한다.

---

## 네이밍

| 대상 | 규칙 | 예시 |
|---|---|---|
| Java 클래스 | PascalCase | `ItemService`, `ClientRepository` |
| Java 메서드·필드 | camelCase | `findByEmail`, `likeCount` |
| 상수·Enum 값 | UPPER_SNAKE_CASE | `ON_SALE`, `MAX_LOGIN_ATTEMPTS` |
| boolean 필드 | `is~`, `has~` 의미 유지 | `isDeleted`, `hasStock` |
| DB 테이블·컬럼 | snake_case | `item_like`, `created_at` |
| API path | kebab-case | `/api/chat-rooms`, `/api/items/{itemId}` |
| 테스트 메서드 | 한글, 기대 동작 명시 | `이미_가입된_이메일이면_회원가입에_실패한다` |
| 브랜치 | 소문자·하이픈 | `feature/6-signup` |

| 용도 | 접미사 | 예시 |
|---|---|---|
| 생성 요청 | `CreateRequest` | `ItemCreateRequest` |
| 수정 요청 | `UpdateRequest` | `ClientUpdateRequest` |
| 부분 수정 요청 | `FieldUpdateRequest` | `ItemStatusUpdateRequest` |
| 단건 응답 | `Response` | `ClientResponse` |
| 상세 응답 | `DetailResponse` | `ItemDetailResponse` |
| 목록 응답 | `ListResponse` | `ReviewListResponse` |
| 목록 항목 | `ListItemResponse` | `ItemListItemResponse` |
| 내부 전달용 | `Dto` | `LoginClientDto` |

---

## 테스트

- 테스트 메서드명은 한글로 작성해 기대 동작을 바로 드러낸다.
- 한 테스트는 한 기대 동작만 검증한다.
- 정상, 실패, 권한, 경계값 시나리오를 함께 작성한다.
- 외부 시스템(MySQL, Redis, S3)은 격리한다. 운영 환경에 우연히 의존하지 않는다.
- 동시성은 단위 테스트만으로 판단하지 않고 통합 테스트를 둔다.
- 테스트를 삭제하거나 완화해 빌드를 통과시키지 않는다.

| 계층 | 검증 대상 |
|---|---|
| 단위 테스트 | 상태 변경, 권한 판단, 입찰 검증 |
| Repository 테스트 | 복합키, Soft Delete, 카운트 조회 |
| API 테스트 | 요청 검증, 응답 DTO, 상태 코드 |
| Security 테스트 | 인증, 소유권, 채팅 참여자 검증 |
| 동시성 테스트 | 좋아요 중복, 팔로우 중복, 경매 입찰 |
