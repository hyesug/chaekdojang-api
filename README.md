# chaekdojang-api

책도장 백엔드 API입니다. 독서 SNS의 독후감, 팔로우, 좋아요, 댓글, 책 검색, 내 서재, 도서 구매 링크 기능을 제공합니다.

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

## 주요 환경 변수

```text
JWT_SECRET=
FRONTEND_URL=http://localhost:3000
BACKEND_URL=http://localhost:8080
KAKAO_API_KEY=
KAKAO_CLIENT_ID=
KAKAO_CLIENT_SECRET=
NAVER_CLIENT_ID=
NAVER_CLIENT_SECRET=
GOOGLE_OAUTH_CLIENT_ID=
GOOGLE_OAUTH_CLIENT_SECRET=
GOOGLE_API_KEY=
DEEPL_API_KEY=
TOSS_SECRET_KEY=
REDIS_HOST=localhost
REDIS_PORT=6379
DEV_LOGIN_ENABLED=false
```

민감한 값은 커밋하지 말고 로컬에서는 `application-local.yaml` 또는 환경 변수로 관리합니다.

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

## Flyway migration plan

현재 운영 환경은 JPA `ddl-auto=validate`를 유지합니다. Flyway는 도입 준비 상태이며, 실제 마이그레이션 실행은 운영 RDS 스키마를 안전하게 추출하고 로컬 검증을 마친 뒤 진행합니다.

### 현재 정책

- 로컬 기본값: `spring.jpa.hibernate.ddl-auto=update`
- 운영 `docker-compose.prod.yml`: `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`
- Flyway 기본값: `SPRING_FLYWAY_ENABLED=false`
- Flyway migration 위치: `api/src/main/resources/db/migration`

### 운영 RDS 변경 전 백업 절차

운영 DB에 Flyway를 적용하기 전 반드시 백업을 먼저 만듭니다.

1. AWS 콘솔에서 RDS 인스턴스를 선택합니다.
2. `Actions` > `Take snapshot`을 선택합니다.
3. 스냅샷 이름은 날짜와 목적을 포함합니다.

```text
chaekdojang-before-flyway-YYYYMMDD-HHMM
```

4. 스냅샷 상태가 `Available`이 될 때까지 기다립니다.
5. 복구 테스트가 필요한 경우 스냅샷으로 별도 RDS 인스턴스를 만든 뒤 애플리케이션 연결 없이 스키마만 확인합니다.

운영 RDS에 직접 마이그레이션을 실행하기 전에는 이 백업 확인이 끝나야 합니다.

### V1 초기 스키마 작성 절차

`V1__init_schema.sql`은 JPA Entity를 보고 임의 작성하지 않습니다. 반드시 실제 운영 RDS의 현재 스키마를 기준으로 만듭니다.

```bash
pg_dump --schema-only --no-owner --no-privileges "$DATABASE_URL" \
  > api/src/main/resources/db/migration/V1__init_schema.sql
```

작성 후 다음 항목을 확인합니다.

- `DROP` 문이 없는지 확인합니다.
- `TRUNCATE` 문이 없는지 확인합니다.
- 데이터 삭제 목적의 `DELETE` 문이 없는지 확인합니다.
- owner, privilege, extension 등 환경 의존 구문이 운영/로컬 양쪽에서 안전한지 확인합니다.
- 테이블, 인덱스, 제약조건 이름이 현재 운영 DB와 일치하는지 확인합니다.

### 로컬 검증 순서

처음에는 Flyway를 끈 상태로 기존 동작을 확인합니다.

```bash
cd api
./gradlew build
./gradlew test
```

운영 스키마에서 만든 `V1__init_schema.sql`이 준비된 뒤에는 빈 로컬 PostgreSQL DB에서 Flyway를 켜고 실행합니다.

```bash
SPRING_FLYWAY_ENABLED=true \
SPRING_JPA_HIBERNATE_DDL_AUTO=validate \
./gradlew bootRun
```

검증 기준:

- Flyway가 `V1__init_schema.sql`을 성공적으로 적용합니다.
- 애플리케이션이 `ddl-auto=validate`로 정상 기동합니다.
- 주요 API가 기존처럼 동작합니다.
- 데이터 손실 SQL이 포함되지 않습니다.

### 운영 반영 순서

1. RDS 스냅샷을 생성하고 `Available` 상태를 확인합니다.
2. 실제 운영 스키마에서 추출한 `V1__init_schema.sql`을 리뷰합니다.
3. 로컬 빈 DB에서 Flyway 적용과 `ddl-auto=validate` 기동을 확인합니다.
4. PR 리뷰 후 `main`에 머지합니다.
5. 운영 배포 때 `SPRING_FLYWAY_ENABLED=true` 적용 여부를 별도 배포 계획에서 결정합니다.
6. 운영에서는 `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`를 유지합니다.

운영 RDS에는 수동으로 직접 접속해 임의 migration을 실행하지 않습니다.
