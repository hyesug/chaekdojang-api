# chaekdojang-api

책도장 백엔드 API입니다. 독서 SNS의 독후감, 팔로우, 좋아요, 댓글, 책 검색, 내 서재, OAuth 로그인 기능을 제공합니다.

## 기술 스택

- Java 21
- Spring Boot 3.x
- Spring Security + JWT
- Spring Data JPA
- PostgreSQL 17
- Redis
- Flyway
- Swagger/OpenAPI

## 로컬 실행

```bash
cd api
./gradlew bootRun
```

기본 API 주소는 `http://localhost:8080`입니다.

Docker로 로컬 PostgreSQL과 Redis까지 함께 실행하려면:

```bash
docker compose up -d --build
```

## 주요 환경 변수

```text
SPRING_DATASOURCE_URL=
SPRING_DATASOURCE_USERNAME=
SPRING_DATASOURCE_PASSWORD=
JWT_SECRET=
FRONTEND_URL=
BACKEND_URL=
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
DEV_LOGIN_ENABLED=false
```

민감한 값은 커밋하지 않습니다. 로컬에서는 `application-local.yaml` 또는 환경 변수로 관리합니다.

## 자주 쓰는 명령

```bash
cd api
./gradlew test
./gradlew build
```

상태 확인:

```bash
curl http://localhost:8080/actuator/health
```

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

## Flyway

운영 DB는 Flyway로 baseline 등록이 완료된 상태입니다.

- migration 위치: `api/src/main/resources/db/migration`
- 초기 스키마: `V1__init_schema.sql`
- 운영 Flyway: `SPRING_FLYWAY_ENABLED=true`
- 운영 Hibernate: `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`

앞으로 DB 구조를 바꿀 때는 엔티티 변경과 함께 `V2__...sql`, `V3__...sql` 파일을 추가해야 합니다. 운영에서는 Hibernate가 테이블을 자동 변경하지 않습니다.

## 운영 Docker 배포

운영 EC2에서는 RDS PostgreSQL을 사용하고, Docker compose로 API와 Redis만 실행합니다.

```bash
cd api
./gradlew clean bootJar
cd ..
docker compose --env-file .env.production -f docker-compose.prod.yml up -d --build
```

운영 `.env.production`은 EC2에만 두고 Git에 커밋하지 않습니다.

현재 compose는 EC2에서 빌드한 jar를 런타임 이미지에 담습니다. Docker Hub 배포 파이프라인을 붙이면 `image` 기반 배포로 바꿀 수 있습니다.

## 운영 전환/롤백 메모

Docker 전환 전에는 기존 `chaekdojang.service`가 `java -jar` 방식으로 API를 실행했습니다. Docker 전환 후에도 서비스 파일은 보존할 수 있으므로, 긴급 롤백이 필요하면 Docker compose를 내리고 기존 systemd 서비스를 다시 시작합니다.

```bash
docker compose --env-file .env.production -f docker-compose.prod.yml down
sudo systemctl start chaekdojang
```

## GitHub Actions 배포

`main` 브랜치에 push하면 GitHub Actions가 테스트를 먼저 실행하고, 성공하면 EC2에 SSH로 접속해 Docker compose 배포를 진행합니다.

GitHub 저장소의 `Settings` > `Secrets and variables` > `Actions`에 아래 Secrets를 등록해야 합니다.

```text
EC2_HOST=EC2 public IP or domain
EC2_SSH_KEY=private SSH key content
```

EC2에는 운영 환경 파일이 있어야 합니다.

```text
~/chaekdojang-api/.env.production
```

이 파일은 Git에 커밋하지 않습니다.
