---
name: git-setup
description: 저장소의 GitHub 템플릿과 로컬 Git hook을 생성하거나 수정한다. 일반 PR 리뷰에는 사용하지 않는다.
---

# git-setup

사용 목적:

- `$git-setup hooks`
- `$git-setup pr-template`
- `$git-setup issue-template`

## 수정 범위

아래 파일만 수정한다:

- `.githooks/pre-commit`
- `.githooks/pre-push`
- `.github/pull_request_template.md`
- `.github/ISSUE_TEMPLATE/*.yml`
- 셋업을 설명하는 docs

## 규칙

- hook을 전역으로 설치하지 않는다.
- commit, push, PR 생성, merge를 실행하지 않는다.
- 프로젝트 로컬 hook 경로를 우선 사용한다:

```bash
git config core.hooksPath .githooks
```

- hook 명령은 빠르게 실패하고 실패 이유를 보여줘야 한다.
- pre-push는 pre-commit보다 무거운 검증을 실행할 수 있다.
