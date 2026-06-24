# AGENTS.md

* Check branches and working trees using `git status --short --branch` before working on code, configuration, documentation, or any Git work.
* Do not overwrite or revert any works already pushed into develop and main branches unless it is really necessary to do so.
* Do not perform commits, pushes, create PRs, merges, or deployments without any approval.
* Follow the `docs/workflow.md` flow for branch creation, commits, PRs, and feature implementation.
* Do not perform unrelated files, large-scale formatting, or refactoring together.
* Explain the necessity and scope of impact when adding new dependencies, environment variables, or external services.


## Project Rules

* Read `docs/adr/README.md` only when verification of open technology decisions is required. If the status is 'Proposed', do not implement it and notify.
* When an ADR decision is made, remove the corresponding row from the open decision table in `docs/adr/README.md` and create the `docs/adr/ADR-{number}.md` file.
* Find and read relevant documentation in `docs/README.md` only when detailed design is required.
* Use the `$git-pr <draft-pr|create-pr|issue> base=<branch>` flow for PR and issue drafts.
* Use `/review` for working trees reviews.
* About API endpoints, Use `/api` for API prefixes and `/ws/chat` for WebSocket connection paths.
* Update `docs/ERD.md` when DB schemas, relationships, indexes, or persistent enums are changed.
* Update `docs/api.md` when changing the path, HTTP method, request/response DTO, or error contract of third parties APIs.
* When adding or changing a API specs, update the `permitAll` setting in `SecurityConfig` and the authentication criteria table in `docs/api.md` together.
* Record the Goal, Decisions, Current Status, and Next Step in `docs/plans/{topic}.md` only for tasks that will span multiple sessions.


## Code Principles

* Follow `docs/convention.md` for implementation rules if necessary.
* For authentication, authorization, and sensitive information changes, check `docs/security.md` and run relevant tests.
* If necessary, follow `docs/business-rules.md` for domain rules, deletion policies, composite keys, and state transitions.
* If tests could not be run, specify the reason and the scope of unverified tests in the final response.
