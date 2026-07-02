# ADR-005 상품 이미지 저장소와 업로드 API

## Status

Accepted

## Context

상품 이미지는 여러 애플리케이션 인스턴스에서 동일하게 접근 가능해야 하고,
업로드 실패 시 DB 저장과 외부 파일 저장 간 정합성을 관리해야 한다.
또한 상품 본문 수정과 이미지 파일 업로드는 요청 형식과 실패 처리 방식이 달라
하나의 API로 합치면 검증과 예외 처리가 복잡해진다.

## Decision

상품 이미지 저장소는 AWS S3를 사용하고, 이미지 업로드 API는 상품 정보 수정 API와 분리한다.

- 상품 이미지 파일은 AWS S3 bucket에 저장한다.
- 이미지 업로드 API는 `POST /api/items/{itemId}/images`를 유지한다.
- 대표 이미지 변경과 이미지 삭제도 별도 API(`PATCH /api/items/{itemId}/images/{imageId}/thumbnail`, `DELETE /api/items/{itemId}/images/{imageId}`)로 유지한다.
- 상품 본문 수정 API `PUT /api/items/{itemId}`는 이미지 파일 자체를 직접 받지 않는다.
- 업로드 중 일부 파일 저장이나 DB 저장이 실패하면, 이미 업로드된 S3 객체는 보상 삭제한다.

## Consequences

- 다중 인스턴스 환경에서도 같은 이미지 URL을 안정적으로 제공할 수 있다.
- 이미지 업로드와 상품 본문 수정의 책임이 분리되어 API 계약과 예외 처리가 단순해진다.
- S3 가용성, 네트워크 지연, bucket 설정이 이미지 기능의 운영 품질에 직접 영향을 준다.
