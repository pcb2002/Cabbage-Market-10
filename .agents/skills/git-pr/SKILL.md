---
name: git-pr
description: |
  현재 브랜치의 변경사항을 기준 브랜치와 비교해 PR 본문 또는 GitHub Issue 초안을 작성한다.
  트리거: "PR 해줘", "PR 만들어줘", "PR 본문 써줘", "이슈 초안 써줘", "$git-pr"
---

# git-pr

## 목적

변경된 diff만 기준으로 파악해 바로 PR에 붙일 수 있는 본문 또는 이슈 초안을 만든다.
변경되지 않은 소스 파일은 읽지 않는다.

## 입력

```text
$git-pr <draft-pr|create-pr|issue> base=<branch>
```

기본값: `base` 없으면 `develop`, `develop` 없으면 `dev`, `dev` 없으면 `main`. mode 없으면 `draft-pr`.
코드 리뷰는 `/review`를 사용한다. `$git-pr`은 PR 본문과 이슈 작성에만 사용한다.

## 절차

### 1단계 — 상태·범위 파악

```bash
git status --short --branch
git diff --name-only <base>...HEAD
git diff --stat <base>...HEAD
```

관련 이슈가 있으면 `gh issue view <number>`로 Goal, Scope, Acceptance Criteria만 확인한다.

### 2단계 — diff 읽기

```bash
git diff --unified=10 <base>...HEAD -- <file>
```

변경된 파일별 diff만 읽는다. 긴 diff는 파일별 핵심만 요약한다.

### 3단계 — 산출물 작성

mode에 따라 아래 형식으로 작성한다.

## mode별 산출물

### draft-pr — PR 본문 초안

```markdown
## 변경 요약

Closes #이슈번호

## 변경 이유

## 주요 변경 파일

| 파일 | 역할 |
|---|---|

## 테스트

- [ ] 실행한 테스트

## 확인 필요

- 리뷰어가 볼 내용

## 범위 밖 변경

- 없으면 "없음"
```

### create-pr — PR 생성

PR 본문 초안(draft-pr 형식)을 먼저 보여주고 사용자 승인을 받은 뒤에만 `gh pr create`를 실행한다.
승인 전에는 제목, base, head, 본문을 표시한다.
PR 제목은 `feat`, `fix`, `refactor`, `docs`, `test`, `chore` 중 하나를 사용해 `type: 한글 제목`으로 작성한다.
예: `feat: 1:1 채팅 메시지 전송 기능 구현`

### issue — GitHub Issue 초안

`.github/ISSUE_TEMPLATE/feature.yml` 구조에 맞춰 아래 항목만 작성한다.
인증 방식 자체, Redis Refresh Token, 외부 연동, 아키텍처 결정이 관련되면 Notes에 `docs/adr/README.md`를 반드시 포함한다.
DB 스키마 변경은 Notes에 `docs/ERD.md`를 포함하고 ADR은 포함하지 않는다.

```markdown
## Goal

## Scope

## Acceptance Criteria

## Notes
```

## 주의

- 사용자 승인 없이 커밋, push, PR 생성, 병합을 실행하지 않는다.
- 변경되지 않은 소스 파일 전체를 읽지 않는다.
- PR 본문에는 관련 이슈와 테스트 결과를 포함한다.
