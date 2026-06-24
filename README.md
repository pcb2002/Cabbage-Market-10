# Cabbage Market 10

저장소 루트: https://github.com/pcb2002/Cabbage-Market-10

이 프로젝트의 문서와 작업 지침에서 쓰는 파일 경로는 저장소 루트 기준이다.

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

## 작업 기준

- 코드, 설정, 문서, Git 작업 전 [AGENTS.md](AGENTS.md)를 확인한다.
- 기능 구현 흐름은 [docs/workflow.md](docs/workflow.md)를 따른다.
- 외부 API 계약 변경은 [docs/api.md](docs/api.md)를 갱신한다.
- DB 스키마 변경은 [docs/ERD.md](docs/ERD.md)를 갱신한다.
