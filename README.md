# chaekdojang-api

## 2026-06 스테이징/공개 범위 업데이트

- 스테이징 API는 운영과 분리된 EC2 인스턴스에서 실행하며, GitHub Actions가 `staging` 브랜치를 배포합니다.
- GitHub Actions가 Docker 이미지를 빌드해 GHCR에 올립니다. EC2는 선택된 이미지만 pull한 뒤 Docker Compose를 재시작합니다.
- 운영 DB와 스테이징 DB 접근은 보안 그룹 규칙으로 분리합니다.
- 운영과 스테이징은 RDS 안에서 각각 `chaekdojang`, `chaekdojang_staging` PostgreSQL 데이터베이스를 사용합니다.
- 스테이징 테스트 데이터는 운영 데이터로 갱신할 수 있습니다. 이때 스테이징 API를 중지하고, 스테이징 DB를 백업한 뒤, 스테이징 `public` 스키마를 삭제/재생성하고, 운영 덤프를 복원한 다음 스테이징 API를 다시 시작합니다.
- 독후감 공개 여부는 `reviews.hidden` 값으로 관리합니다.
- 공개 피드, 책별 독후감 모음, 공개 사용자 페이지, 사이트맵, SEO 페이지에는 `hidden=false` 독후감만 노출합니다.
- 작성자는 `PATCH /api/reviews/{id}/hidden`으로 본인 독후감의 공개 여부를 변경할 수 있습니다.
- 관리자는 기존처럼 `PATCH /api/admin/reviews/{id}/hidden`으로 독후감 공개 여부를 관리할 수 있습니다.
- `GET /api/users/me/reviews`는 로그인한 사용자가 본인 독후감을 관리할 수 있도록 공개/비공개 독후감을 모두 반환합니다.
- 캘린더 관련 프론트 동작은 `GET /api/users/me/reviews` 응답의 `hidden` 필드에 의존합니다. 이를 통해 비공개/삭제 독후감을 캘린더에서 제외합니다.

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

Swagger UI 주소:

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
STORAGE_TYPE=local
S3_UPLOAD_BUCKET=
AWS_REGION=ap-northeast-2
S3_PUBLIC_BASE_URL=
BACKUP_S3_BUCKET=
BACKUP_S3_PREFIX=db-backups/prod
CORS_ALLOWED_ORIGINS=
RATE_LIMIT_ENABLED=true
RATE_LIMIT_API_PER_MINUTE=600
RATE_LIMIT_UPLOAD_PER_MINUTE=20
SERVER_TOMCAT_THREADS_MAX=100
SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=10
```

민감한 값은 커밋하지 않습니다. 로컬에서는 `application-local.yaml` 또는 환경 변수로 관리합니다.

## 운영 구조

운영 EC2에서는 Docker Compose로 API와 Redis를 실행합니다. PostgreSQL은 AWS RDS를 사용합니다. API 이미지는 GitHub Actions가 GHCR에 빌드해서 올리고, EC2는 이미지를 내려받아 실행합니다.

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

## 스테이징 운영

스테이징 API는 `https://staging-api.chaekdojang.com`에서 확인합니다. 스테이징 프론트는 `https://staging.chaekdojang.com`입니다.

스테이징 서버는 운영과 분리된 EC2에서 실행하고, DB는 같은 RDS 인스턴스 안의 별도 데이터베이스 `chaekdojang_staging`을 사용합니다. 운영 데이터로 스테이징을 맞춰 테스트해야 할 때는 운영 DB를 직접 수정하지 않고 스테이징 DB만 덮어씁니다.

## 개발 요청 처리 흐름

앞으로 개발 요청은 아래 순서로 처리합니다.

1. 요청을 체크리스트로 정리하고 우선순위와 개발 방법을 설명합니다.
2. 백엔드/프론트 영향 범위, 개발자가 할 일, 사용자가 할 일을 분리합니다.
3. 개발자가 할 수 있는 로컬 개발과 검증을 먼저 완료합니다.
4. 처음 체크리스트를 다시 보여주며 완료/미완료/사용자 확인 필요 항목을 표시합니다.
5. 사용자가 스테이징 배포를 요청하면 `staging` 브랜치에 커밋·푸시하고 스테이징에 배포합니다.
6. 스테이징 테스트 항목을 전달하고, 사용자가 테스트 후 요청한 수정사항을 반영합니다.
7. 사용자가 테스트 완료 후 `main` 푸시를 명시하면 그때 `main` 브랜치에 반영합니다.
8. 스테이징 확인이 끝나면 비용 절감을 위해 스테이징 EC2를 중지합니다.

권장 순서:

```text
1. 스테이징 API 컨테이너 중지
2. 현재 스테이징 DB 덤프 백업
3. 운영 DB 덤프 생성
4. 스테이징 DB public 스키마 DROP/CREATE
5. 운영 덤프를 스테이징 DB에 복원
6. 스테이징 API 재시작
7. 준비 상태와 주요 API 스모크 테스트 확인
```

스테이징 DB를 운영 데이터로 초기화한 뒤에는 운영 계정/글 기준으로 테스트할 수 있습니다. 단, 소셜 로그인은 운영/스테이징 OAuth 리다이렉트 URI와 소셜 제공자 식별자에 따라 새 가입처럼 보일 수 있습니다.

## Flyway

운영 DB는 Flyway 베이스라인 등록이 완료된 상태입니다.

- 마이그레이션 위치: `api/src/main/resources/db/migration`
- 초기 스키마: `V1__init_schema.sql`
- 운영 Flyway: `SPRING_FLYWAY_ENABLED=true`
- 운영 Hibernate: `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`

앞으로 DB 구조를 바꿀 때는 엔티티 변경과 함께 `V2__...sql`, `V3__...sql` 파일을 추가해야 합니다. 운영에서는 Hibernate가 테이블을 자동 변경하지 않습니다.

## 수동 Docker 배포

```bash
cd ~/chaekdojang-api/api
./gradlew clean bootJar
cd ..
APP_IMAGE=ghcr.io/hyesug/chaekdojang-api:<tag> sudo -E docker compose --env-file .env.production -f docker-compose.prod.yml up -d --no-build
```

운영 자동 배포는 EC2에서 직접 빌드하지 않습니다. GitHub Actions가 `ghcr.io/hyesug/chaekdojang-api:<commit-sha>` 이미지를 만들고, EC2에서는 해당 이미지를 내려받은 뒤 `APP_IMAGE`로 지정해 실행합니다.

## GitHub Actions 배포

`main` 브랜치에 push하면 GitHub Actions가 테스트를 먼저 실행합니다. 성공하면 jar와 Docker 이미지를 GitHub Actions 실행 환경에서 빌드해 GHCR에 올리고, SSM으로 EC2에 명령을 보내 이미지를 내려받은 뒤 Docker Compose 배포를 진행합니다. `staging` 브랜치도 같은 방식으로 분리된 스테이징 EC2에 배포합니다.

GitHub 저장소의 `Settings` > `Secrets and variables` > `Actions`에 아래 Secrets를 등록해야 합니다.

```text
EC2_HOST=EC2 공개 IP 또는 도메인
EC2_SSH_KEY=비공개 SSH 키 내용
```

GHCR 업로드와 다운로드는 워크플로의 `GITHUB_TOKEN`으로 처리합니다. 별도 GHCR 토큰은 필요 없습니다.

## 운영 스모크 테스트

배포 완료 기준은 단순히 Docker 컨테이너가 떠 있는 것이 아니라, 사용자가 실제로 접근하는 공개 경로가 정상인 것입니다.

```bash
./scripts/smoke-prod.sh
```

이 스크립트는 아래를 확인합니다.

- `https://api.chaekdojang.com/actuator/health`가 `UP`인지
- `https://www.chaekdojang.com/api/reviews?page=0&size=5&sort=recent`가 독후감 데이터를 1개 이상 반환하는지
- `https://www.chaekdojang.com` 홈이 200인지

GitHub Actions 배포도 이 스모크 테스트를 실행합니다. 프론트 rewrite 설정이나 Nginx HTTPS 설정이 깨지면 배포 성공으로 처리하지 않습니다.

## 운영 DB 백업

배포 워크플로는 새 API 컨테이너를 올리기 전에 RDS PostgreSQL 백업을 먼저 생성합니다.

```bash
./scripts/backup-prod-db.sh
```

기본 백업 위치는 아래와 같습니다.

```text
/home/ubuntu/db_backups
```

백업 파일은 `pg_dump --format=custom` 형식이며 기본 14일 동안 보관합니다. 수동 복구가 필요하면 먼저 새 RDS나 별도 DB에 복원해서 확인한 뒤 운영 DB에 적용합니다.

## 운영 상태 점검

EC2에서 아래 명령으로 컨테이너, 디스크, 상태, 최근 백업, 최근 로그를 한 번에 확인할 수 있습니다.

```bash
cd ~/chaekdojang-api
./scripts/ops-status.sh
```

## CloudWatch 로그

CloudWatch Agent 설정 파일과 설치 스크립트를 준비해두었습니다.

```bash
cd ~/chaekdojang-api
./scripts/install-cloudwatch-agent.sh
```

이 스크립트는 Nginx 접근/오류 로그와 EC2 디스크/메모리 지표를 CloudWatch로 보냅니다. 실행 전에 EC2 인스턴스에 CloudWatch Agent 권한이 있는 IAM Role을 연결해야 합니다.

필요 IAM 권한:

```text
CloudWatchAgentServerPolicy
```

## 자동 배포 실패 시 동작

배포 워크플로는 새 이미지를 만들기 전에 현재 정상 이미지를 `chaekdojang-api:rollback`으로 저장합니다.

새 컨테이너가 `/actuator/health`에서 `UP`을 반환하지 않으면:

1. 새 컨테이너 로그를 출력합니다.
2. 이전 Docker 이미지를 `chaekdojang-api:local`로 되돌립니다.
3. 이전 이미지로 API 컨테이너를 다시 띄웁니다.
4. 롤백 후 상태 확인을 다시 실행합니다.
5. GitHub Actions는 실패로 표시합니다.

DB 마이그레이션 자체가 이미 실행된 뒤 실패한 경우에는 별도 판단이 필요합니다. DB 변경 마이그레이션은 작게 나누고 배포 전 신중히 확인합니다.

## 수동 롤백

긴급 수동 롤백이 필요하면 EC2에서 실행합니다.

```bash
cd ~/chaekdojang-api
sudo docker tag chaekdojang-api:rollback chaekdojang-api:local
sudo docker compose --env-file .env.production -f docker-compose.prod.yml up -d app
curl http://localhost:8080/actuator/health
```

기존 `java -jar` systemd 서비스도 남아 있지만, 현재 운영 기본 방식은 Docker입니다.
