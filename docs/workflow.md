# 작업 흐름

현재 저장소에서 사용하는 작업 흐름이다.

## 브랜치

| 브랜치 | 용도 |
|---|---|
| `main` | 배포 |
| `develop` / `dev` | 통합 |
| `feature/{issue}-{topic}` | 기능 개발 |
| `fix/{topic}` 또는 `fix/{issue}-{topic}` | 버그 수정 |
| `docs/{topic}` | 문서 수정 |

## 작업 시작

1. `git status --short --branch`로 상태를 확인한다.
2. 현재 작업 범위와 무관한 변경은 건드리지 않는다.
3. 코드 변경 전 관련 실제 구현을 먼저 읽는다.
4. 문서는 코드 변경과 같은 턴에 맞춘다.

## 커밋

형식:

```text
type: subject
```

예시:

```text
feat: 리뷰 작성 조회 구현
fix: 정적 리소스 접근 수정
docs: API 문서 정리
```

## PR

- PR 제목은 커밋 타입과 맞춘다.
- PR 본문에는 변경 요약, 테스트 결과, 남은 위험을 적는다.
- 코드 기준으로 문서가 바뀌었으면 문서 변경도 함께 포함한다.

## 문서 작업 규칙

- `docs/api.md`: 현재 구현된 API만 유지
- `docs/ERD.md`: 현재 엔티티만 유지
- `docs/security.md`: 실제 `SecurityConfig`, JWT, STOMP 인증 기준
- `docs/business-rules.md`: 실제 서비스/엔티티 규칙 기준

## 리뷰 기준

- 문서가 코드보다 앞서가면 안 된다.
- 구현되지 않은 기능 설명은 제거한다.
- 경로, 응답 필드, 삭제 정책, 인증 기준이 실제 코드와 맞는지 우선 본다.
