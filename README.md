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
- Docker
- Swagger/OpenAPI

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

## 운영 구조

운영 EC2에서는 Docker compose로 API와 Redis를 실행합니다. PostgreSQL은 AWS RDS를 사용합니다. API 이미지는 GitHub Actions가 GHCR에 빌드해서 올리고, EC2는 이미지를 pull해서 실행합니다.

```text
EC2
├─ Nginx
├─ Docker
│  ├─ chaekdojang-app
│  └─ chaekdojang-redis
└─ RDS PostgreSQL은 외부 AWS RDS
```

운영 `.env.production`은 EC2에만 두고 Git에 커밋하지 않습니다.

```text
~/chaekdojang-api/.env.production
```

## Flyway

운영 DB는 Flyway baseline 등록이 완료된 상태입니다.

- migration 위치: `api/src/main/resources/db/migration`
- 초기 스키마: `V1__init_schema.sql`
- 운영 Flyway: `SPRING_FLYWAY_ENABLED=true`
- 운영 Hibernate: `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`

앞으로 DB 구조를 바꿀 때는 엔티티 변경과 함께 `V2__...sql`, `V3__...sql` 파일을 추가해야 합니다. 운영에서는 Hibernate가 테이블을 자동 변경하지 않습니다.

## 수동 Docker 배포

```bash
cd ~/chaekdojang-api/api
./gradlew clean bootJar
cd ..
sudo docker compose --env-file .env.production -f docker-compose.prod.yml up -d --build
```

운영 자동 배포는 EC2에서 직접 빌드하지 않습니다. GitHub Actions가 `ghcr.io/hyesug/chaekdojang-api:<commit-sha>` 이미지를 만들고, EC2에서는 해당 이미지를 pull한 뒤 `APP_IMAGE`로 지정해 실행합니다.

## GitHub Actions 배포

`main` 브랜치에 push하면 GitHub Actions가 테스트를 먼저 실행합니다. 성공하면 jar와 Docker 이미지를 GitHub Actions runner에서 빌드해 GHCR에 push하고, EC2에 SSH로 접속해 이미지를 pull한 뒤 Docker compose 배포를 진행합니다.

GitHub 저장소의 `Settings` > `Secrets and variables` > `Actions`에 아래 Secrets를 등록해야 합니다.

```text
EC2_HOST=EC2 public IP or domain
EC2_SSH_KEY=private SSH key content
```

GHCR push와 pull은 workflow의 `GITHUB_TOKEN`으로 처리합니다. 별도 GHCR 토큰은 필요 없습니다.

## 운영 Smoke Test

배포 완료 기준은 단순히 Docker 컨테이너가 떠 있는 것이 아니라, 사용자가 실제로 접근하는 공개 경로가 정상인 것입니다.

```bash
./scripts/smoke-prod.sh
```

이 스크립트는 아래를 확인합니다.

- `https://api.chaekdojang.com/actuator/health`가 `UP`인지
- `https://www.chaekdojang.com/api/reviews?page=0&size=5&sort=recent`가 독후감 데이터를 1개 이상 반환하는지
- `https://www.chaekdojang.com` 홈이 200인지

GitHub Actions 배포도 이 smoke test를 실행합니다. 프론트 rewrite나 Nginx HTTPS 설정이 깨지면 배포 성공으로 처리하지 않습니다.

## 자동 배포 실패 시 동작

배포 workflow는 새 이미지를 만들기 전에 현재 정상 이미지를 `chaekdojang-api:rollback`으로 저장합니다.

새 컨테이너가 `/actuator/health`에서 `UP`을 반환하지 않으면:

1. 새 컨테이너 로그를 출력합니다.
2. 이전 Docker 이미지를 `chaekdojang-api:local`로 되돌립니다.
3. 이전 이미지로 API 컨테이너를 다시 띄웁니다.
4. rollback health check를 다시 확인합니다.
5. GitHub Actions는 실패로 표시합니다.

DB migration 자체가 이미 실행된 뒤 실패한 경우에는 별도 판단이 필요합니다. DB 변경 migration은 작게 나누고 배포 전 신중히 확인합니다.

## 수동 롤백

긴급 수동 롤백이 필요하면 EC2에서 실행합니다.

```bash
cd ~/chaekdojang-api
sudo docker tag chaekdojang-api:rollback chaekdojang-api:local
sudo docker compose --env-file .env.production -f docker-compose.prod.yml up -d app
curl http://localhost:8080/actuator/health
```

기존 `java -jar` systemd 서비스도 남아 있지만, 현재 운영 기본 방식은 Docker입니다.
