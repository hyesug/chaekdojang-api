# chaekdojang-api

책도장 백엔드 API입니다. 독서 SNS의 독후감, 팔로우, 좋아요, 댓글, 책 검색, 내 서재, 관리자 기능을 제공합니다.

## 기술 스택

- Java 21
- Spring Boot 3.x
- Spring Security + JWT
- Spring Data JPA
- PostgreSQL 17
- Redis
- Flyway
- Docker
- Swagger/OpenAPI

## 주요 기능

- OAuth/JWT 기반 인증
- 독후감 CRUD, 공개/비공개 관리
- 피드, 좋아요, 댓글, 북마크
- 팔로우/팔로워
- 책 검색 및 책 상세 API
- 내 서재
- 관리자 사용자/독후감/로그 관리
- 공개 SEO 페이지용 책/독후감 조회 API

## 프로젝트 구조

```text
api/
├─ src/main/java/com/chaekdojang/api
│  ├─ config
│  ├─ domain
│  ├─ global
│  └─ infra
├─ src/main/resources
│  ├─ db/migration
│  └─ application.yaml
└─ build.gradle.kts
```

## 로컬 실행

```bash
cd api
./gradlew bootRun
```

상태 확인:

```bash
curl http://localhost:8080/actuator/health
```

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

## 테스트

```bash
cd api
./gradlew test
```

## 환경 변수

로컬 민감 정보는 커밋하지 않습니다. 로컬에서는 환경 변수 또는 `application-local.yaml`을 사용합니다.

```text
SPRING_DATASOURCE_URL=
SPRING_DATASOURCE_USERNAME=
SPRING_DATASOURCE_PASSWORD=
JWT_SECRET=
FRONTEND_URL=
BACKEND_URL=
CORS_ALLOWED_ORIGINS=
KAKAO_API_KEY=
KAKAO_CLIENT_ID=
KAKAO_CLIENT_SECRET=
NAVER_CLIENT_ID=
NAVER_CLIENT_SECRET=
GOOGLE_OAUTH_CLIENT_ID=
GOOGLE_OAUTH_CLIENT_SECRET=
GOOGLE_API_KEY=
REDIS_HOST=
REDIS_PORT=
STORAGE_TYPE=local
AWS_REGION=ap-northeast-2
S3_UPLOAD_BUCKET=
S3_PUBLIC_BASE_URL=
```

## 데이터베이스

- 마이그레이션 위치: `api/src/main/resources/db/migration`
- DB 구조 변경 시 엔티티 수정과 Flyway migration 파일을 함께 추가합니다.
- 운영 환경에서는 Hibernate 자동 스키마 변경에 의존하지 않습니다.

## 배포

- `staging` 브랜치 push: 스테이징 API 배포
- `main` 브랜치 push: 운영 API 배포
- GitHub Actions가 Docker 이미지를 빌드해 GHCR에 올리고, EC2는 이미지를 내려받아 실행합니다.

스테이징 API:

```text
https://staging-api.chaekdojang.com
```

운영 API:

```text
https://api.chaekdojang.com
```

## 운영 참고

운영/스테이징 세부 절차, AWS 비용/보안 점검, 백업, CloudWatch, 장애 대응은 `docs/` 아래 문서를 참고합니다.
