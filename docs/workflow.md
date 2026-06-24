# 작업 흐름

이슈 생성부터 PR 머지까지 전 과정의 규칙을 정의한다.

저장소 루트는 `https://github.com/pcb2002/Cabbage-Market-10`이며, 이 문서의 파일 경로는 저장소 루트 기준이다.

---

## 브랜치 구조

| 브랜치 | 용도 | 직접 push |
|---|---|---|
| `main` | 제품 배포 | 금지 |
| `develop` / `dev` | 기능 통합·검증 | 금지 |
| `feature/{issueNumber}-{topic}` | 기능 구현 | 허용 |
| `fix/{issueNumber}-{topic}` | 버그 수정 | 허용 |
| `docs/{topic}` | 문서 작업 | 허용 |
| `refactor/{topic}` | 리팩터링 | 허용 |
| `chore/{topic}` | 빌드·설정·도구 | 허용 |

- 로컬에 `develop`이 없으면 `dev`를 개발 기준 브랜치로 사용한다.
- 브랜치명은 소문자 영문, 숫자, 하이픈만 사용한다.

```text
feature/6-signup
feature/5-login
fix/7-duplicate-email
docs/erd-update
```

---

## 이슈 생성

사용자가 `{기능명} 기능 구현 이슈 만들어줘`라고 요청하면:

1. `git status --short --branch`로 현재 브랜치와 작업 트리를 확인한다.
2. `gh issue list`로 같은 제목의 열린 이슈가 있는지 확인한다.
3. `.github/ISSUE_TEMPLATE/feature.yml` 항목에 맞춰 이슈를 만든다.
4. 이슈 본문에 구현 브랜치명 `feature/{issueNumber}-{topic}`을 적는다.
5. Notes에 참조 문서와 주의사항을 적는다. ADR·ERD 등 필요 문서는 `$git-pr issue`가 자동으로 판단한다.

이슈 본문 구조:

```
## Goal
무엇을 구현하는가

## Scope
구현 범위 목록

## Acceptance Criteria
- [ ] 검증 가능한 완료 기준
- [ ] 각 항목은 테스트로 검증 가능한 수준으로 작성

## Notes
- 참고 문서: docs/ERD.md, docs/api.md 등
- 주의사항 또는 의존 이슈
```

---

## 구현 시작

사용자가 이슈를 토대로 구현을 요청하면:

1. **이슈 본문을 가장 먼저 읽는다.** Notes에서 필요한 문서 목록을 파악한다.
2. Notes에 적힌 문서만 읽는다. Notes에 없는 문서는 읽지 않는다.
3. Notes에 `docs/adr/README.md`가 포함되거나 기술 결정이 관련된 경우 ADR을 먼저 확인한다.
   - ADR이 Proposed 상태이고 해당 결정이 필요하면 구현을 멈추고 사용자에게 알린다.
4. 개발 기준 브랜치에서 `feature/{issueNumber}-{topic}` 브랜치를 만든다.
5. 이슈의 Scope와 Acceptance Criteria에 있는 항목만 구현한다.

---

## 범위 제한

이슈에 없는 기능은 구현하지 않는다.

아래 변경이 필요하면 구현 전 사용자에게 먼저 알린다.

- 공통 예외 핸들러, 공통 응답 포맷 추가
- 인증 필터, Security 설정 변경
- 새 의존성 추가
- 문서 대량 수정

기능이 컴파일되기 위한 최소 기반 코드는 허용하되, 기능 커밋과 분리한다.

```text
feat: 회원가입 API 구현
test: 회원가입 API 테스트 추가
chore: Flyway 설정 추가    ← 기반 변경은 별도 커밋
```

범위 밖 기반 변경이 승인되지 않으면 stash 또는 별도 브랜치로 분리하고 기능 PR에 포함하지 않는다.

---

## 커밋 메시지

형식:

```text
type: subject
```

| type | 용도 |
|---|---|
| `feat` | 기능 구현 |
| `fix` | 버그 수정 |
| `test` | 테스트 추가·수정 |
| `docs` | 문서 수정 |
| `refactor` | 동작 변경 없는 구조 개선 |
| `chore` | 빌드, 설정, 의존성, 기타 |

규칙:

- `type`은 영문 소문자.
- `subject`는 한글 명사형 단답 (`구현`, `추가`, `수정`, `삭제`, `분리`).
- 한 커밋에 한 목적만 담는다.
- PR 제목과 커밋 type은 일치한다.
- 이슈 범위 밖 작업을 같은 커밋에 넣지 않는다.

```text
feat: 회원가입 API 구현
test: 회원가입 이메일 중복 검증 테스트 추가
docs: 회원가입 응답 DTO 계약 정리
chore: H2 테스트 의존성 추가
```

---

## 문서 수정 기준

기능 구현마다 문서를 자동으로 추가하지 않는다. 아래 경우에만 수정한다.

| 변경 내용 | 수정 문서 |
|---|---|
| API 경로·메서드·DTO·오류 계약 변경 | `docs/api.md` |
| DB 스키마·인덱스·관계·영속 enum 변경 | `docs/ERD.md` |
| 인증·권한·민감정보 정책 변경 | `docs/security.md` |
| 도메인 상태 전이·삭제 정책·복합키 규칙 변경 | `docs/business-rules.md` |
| ADR 열린 결정 확정 | `docs/adr/README.md` + `docs/adr/ADR-{번호}.md` |

이슈 내용을 docs에 반복해서 복사하지 않는다.

---

## 테스트

- 테스트 메서드명은 한글로 작성한다.
- 한 테스트는 한 기대 동작만 검증한다.
- 이슈 Acceptance Criteria에 해당하는 정상·실패·권한·경계값 시나리오를 우선 작성한다.
- 외부 의존성(MySQL, Redis, S3)은 격리한다.
- 테스트를 삭제하거나 완화해 빌드를 통과시키지 않는다.

```java
@Test
void 회원가입_성공시_비밀번호를_제외한_회원정보를_반환한다() { }

@Test
void 이미_가입된_이메일이면_회원가입에_실패한다() { }

@Test
void 미인증_상태에서_상품_등록_요청은_401을_반환한다() { }
```

---

## 리뷰와 PR

1. 구현 후 `/review base=<develop|dev>`를 실행한다.
2. P1·P2 문제가 있으면 수정 후 다시 리뷰한다. PR을 먼저 만들지 않는다.
3. 테스트 통과 확인 후 `$git-pr create-pr base=<develop|dev>`로 PR 본문을 만든다.
4. PR 제목 형식: `type: 한글 제목` — type은 `feat`, `fix`, `refactor`, `docs`, `test`, `chore` 중 하나.
5. PR 본문에 `Closes #{issueNumber}` 포함.
6. PR 본문에 구현 요약, 테스트 결과, 리뷰 결과, 범위 밖 변경 여부를 적는다.

```text
feat: 회원가입 API 구현
fix: 회원가입 중복 이메일 검증 수정
docs: ERD 컬럼 누락 정리
```

---

## 토큰 절약

- 이슈 생성 시 전체 docs를 읽지 않는다.
- 구현 시작 시 이슈 Notes에 적힌 문서의 관련 구간만 읽는다.
- `docs/README.md`는 어떤 문서가 필요한지 모를 때만 인덱스로 사용한다.
- 변경되지 않은 소스 파일 전체를 읽지 않는다.
- 리뷰는 diff 기준으로만 수행한다.
