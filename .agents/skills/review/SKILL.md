---
name: review
description: |
  현재 브랜치의 변경사항을 기준 브랜치와 비교해 코드 리뷰를 수행한다.
  문서 정합성, 컨벤션, 도메인 규칙, 테스트 시나리오 커버리지, 테스트 실행 결과를 포함한다.
  코드는 직접 수정하지 않는다.
  트리거: "리뷰해줘", "코드 봐줘", "머지 전에 확인해줘", "코드리뷰", "/review", "/rv"
---

# review

`/review base=<branch>` — base 미지정 시 PR 대상 → 원격 기본 브랜치 순. 확정 불가 시 추측하지 않는다.

변경된 파일 diff만 읽는다. 추가로 읽는 파일은 변경된 파일에서 직접 참조하는 클래스에 한정한다.
추가 파일에서 또 다른 파일로 연쇄 탐색하지 않는다.
diff만으로 확인 불가한 사항은 Open Questions로 표시하고 더 읽지 않는다.

---

## 문서 읽기 (조건부)

경로 없으면 `⚪ 추가 문맥 필요`.

| 조건 | 문서 |
|---|---|
| 인증·Redis·아키텍처 결정 | `docs/adr/README.md` |
| Entity·DB·Migration | `docs/ERD.md` |
| Controller·DTO·API | `docs/api.md` |
| 인증·권한·Security | `docs/security.md` |
| Service·도메인 로직 | `docs/business-rules.md` |

Proposed ADR 미결정 사항을 승인 없이 확정하거나 확정 ADR과 충돌 → P1.

---

## 컨벤션

Controller 로직 없음 / 생성자 주입 / DTO 분리 / Entity 직접 반환 금지 / LAZY 우선 / Service 트랜잭션 + 조회 readOnly / GlobalExceptionHandler / Entity @Data 금지 / PascalCase·camelCase·snake_case

---

## 테스트 시나리오

변경 도메인과 직접 영향 도메인만 확인한다.

| 도메인 | 필수 시나리오 |
|---|---|
| auth / client | 중복 이메일 차단 |
| client | 탈퇴 회원 조회 제외 |
| category | 비활성 카테고리 목록 제외 |
| item | 임시저장/판매중 구분, 판매자 외 수정 차단 |
| itemLike | 중복 차단, like_count 정합성 |
| inquiry | 작성자 외 수정·삭제, 판매자 외 답변 차단 |
| follow | 자기 자신 팔로우 차단 |
| chat | 참여자 외 조회 차단 |
| review | 작성자 외 수정·삭제 차단 |
| auction | 마감 후 입찰, 현재가 이하 입찰 차단 |

---

## 우선순위

| | 기준 |
|---|---|
| P1 | ADR 충돌·미결정 확정 / 데이터 손실·권한 우회·인증 실패·민감정보 노출 / ERD·API 계약 위반 |
| P2 | 트랜잭션·상태·카운트 정합성 / Soft Delete / 동시성·소유권 누락 / 핵심 시나리오 누락 |
| P3 | 컨벤션 위반 / 선택적 가독성 |


---

## 판정 기준

| 판정 | 조건 |
|---|---|
| 🟢 Approve | P1·P2·P3 없음 |
| 🟡 Approve with Comments | P3만 존재 — 머지 가능, 다음 기회에 수정 권장 |
| 🔴 Request Changes | P1 또는 P2 존재 — 머지 전 수정 필요 |
| ⚫ Blocked | 기준 브랜치·핵심 문서·필수 코드 없어 판단 불가 |

---

## 출력 규칙

- 첫 번째 줄은 반드시 `## 리뷰 결과`로 시작한다. 그 전에 어떤 텍스트도 출력하지 않는다.
- "형님", "결론부터", "리뷰하겠습니다" 같은 서문을 절대 출력하지 않는다.
- 출력 형식에 없는 섹션(참고, 비고, 추가 의견 등)을 임의로 추가하지 않는다.
- 변경 도메인이 있으면 테스트 시나리오 섹션을 반드시 출력한다.

---

## 출력

### P3만 있을 때

```
## 리뷰 결과

🟡 Approve with Comments — P3 1건

### Findings

**[P3]** `ItemService.java:12` — 메서드명이 동작을 드러내지 않음
근거: getItem() → findItemById()가 더 명확 / 제안: 다음 수정 시 반영

### 테스트 시나리오

| 판매자 외 수정 차단 | ✅ |

### 테스트 실행
`./gradlew test` — ✅ 8건 통과
```

### 문제 있을 때

```
## 리뷰 결과

🔴 Request Changes — P1 1건 · P2 1건

### Findings

**[P1]** `ItemService.java:42` — Soft Delete 미적용
근거: is_deleted 필터 없음 / 영향: 삭제 상품 노출 / 제안: findAllByIsDeletedFalse()

### 테스트 시나리오

| 판매자 외 수정 차단 | ✅ |
| 임시저장/판매중 구분 | ❌ |

### 테스트 실행
`./gradlew test` — ✅ 12건 통과
```

### 문제 없을 때

```
## 리뷰 결과

🟢 Approve

### 테스트 시나리오

| 중복 이메일 차단 | ✅ |
| 판매자 외 수정 차단 | ✅ |

### 테스트 실행
`./gradlew test` — ✅ 12건 통과
```

> Open Questions가 있으면 테스트 실행 아래에 추가한다.
