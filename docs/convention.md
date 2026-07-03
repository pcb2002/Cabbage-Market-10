# 개발 표준

현재 프로젝트의 실제 패키지 구조와 구현 방식을 기준으로 정리한다.

## 기술 스택

- Java 17
- Spring Boot
- Spring MVC
- Spring WebSocket + STOMP
- Spring Security
- Spring Data JPA
- QueryDSL
- MySQL
- Redis
- Gradle

## 현재 패키지 구조

```text
src/main/java/com/example/cabbagemarket10
├── application
│   └── facade
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
    ├── common
    ├── config
    ├── exception
    ├── init
    ├── security
    └── util
```

## 계층 흐름

기본:

```text
Controller -> Service -> Repository -> DB
```

복합 유스케이스:

```text
Controller -> Facade -> Domain Service(s) -> Repository -> DB
```

WebSocket:

```text
STOMP Client -> Interceptor -> Controller -> Service -> Repository -> DB
```

## 계층 책임

| 계층 | 책임 | 금지 |
|---|---|---|
| Controller | 요청 검증, 인증 사용자 추출, 응답 반환 | 비즈니스 로직, DB 직접 접근 |
| Facade | 여러 도메인 서비스 조합, 유스케이스 조정 | Repository 직접 접근 |
| Service | 트랜잭션, 권한 검증, 상태 변경 | HTTP 세부사항 처리 |
| Repository | 조회/저장 | 비즈니스 판단 |
| Entity | 도메인 상태와 최소 행위 | 외부 인프라 의존 |
| DTO | 요청/응답 계약 | 비즈니스 로직 |

## DTO 규칙

- Request DTO와 Response DTO를 분리한다.
- Controller 계약 DTO는 `Request`, `Response` 접미사를 사용한다.
- 목록 항목은 `ListItemResponse`, 페이지 응답 래퍼는 `PageResponse`를 사용한다.
- Entity를 API 응답으로 직접 반환하지 않는다.
- Bean Validation은 Request DTO에 둔다.

## JPA 규칙

- 연관관계는 기본 LAZY다.
- Soft Delete 대상은 `@SQLDelete`, `@SQLRestriction`으로 숨긴다.
- 현재 복합키 Entity는 `@IdClass`를 사용한다.
- `BaseEntity`는 `createdAt`, `updatedAt` 감사 필드를 제공한다.

## 현재 구현에서 눈에 띄는 패턴

- `ItemFacade`, `ReviewFacade`, `AuthFacade`, `ItemImageFacade`가 복합 유스케이스를 조정한다.
- 검색 v2 캐시는 `@Cacheable`로 비로그인 조회만 캐시한다.
- 채팅은 HTTP API와 WebSocket API가 같이 있다.
- 상품 이미지는 파일 업로드와 도메인 저장을 facade에서 함께 조정한다.

## 네이밍

| 대상 | 예시 |
|---|---|
| Entity | `Item`, `Review`, `InquiryLog` |
| Service | `ItemService`, `SearchService` |
| Facade | `ItemFacade`, `ReviewFacade` |
| Request DTO | `ItemCreateRequest` |
| Response DTO | `ItemDetailResponse` |
| 목록 항목 DTO | `ReceivedReviewListItemResponse` |
| Enum | `TradeStatus`, `ConditionType` |

## 테스트 규칙

- 테스트 메서드명은 한글로 작성한다.
- 정상, 실패, 권한, 경계값을 함께 검증한다.
- 외부 의존성은 Mock, Embedded, 설정 분리로 격리한다.
- Redis 락/캐시 동작은 설정과 조건부 빈을 함께 고려한다.
- 테스트를 약화해서 통과시키지 않는다.

## 문서 규칙

- 문서는 코드보다 앞서지 않는다.
- 구현 안 된 API를 문서에 쓰지 않는다.
- 경로, DTO, 보안 정책이 바뀌면 `docs/api.md`, `docs/security.md`를 함께 갱신한다.
- 엔티티, 관계, 삭제 정책이 바뀌면 `docs/ERD.md`, `docs/business-rules.md`를 함께 갱신한다.
