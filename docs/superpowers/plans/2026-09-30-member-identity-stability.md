# 회원 식별 안정화 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 개인정보를 추가 수집하지 않고 고객 코드, 안전한 OAuth 연결, 결제 이력 보존을 갖춘 안정적인 회원 식별 구조를 구현한다.

**Architecture:** `users.id`와 `(provider, providerUserId)`를 내부 식별 기준으로 유지한다. DB migration이 불변 customer code를 생성하고, OAuth 일반 로그인은 provider 식별자로만 재로그인하며 OAuth 연결은 로그인된 계정의 명시적 연결 흐름에서만 허용한다. 내 프로필 응답만 계정 정보를 확장하고 탈퇴는 구독 이력을 비활성화하여 보존한다.

**Tech Stack:** Java 21, Spring Boot 3, Spring Data JPA, PostgreSQL 17, Flyway, Next.js, TypeScript, Tailwind CSS

**Spec:** `docs/superpowers/specs/2026-09-30-member-identity-stability-design.md`

## Global Constraints

- `users.id`는 모든 내부 관계와 JWT의 canonical user id로 유지한다.
- OAuth 이메일은 nullable이며 로그인, 가입, 유료 기능의 필수 조건으로 사용하지 않는다.
- 실명과 휴대폰번호를 수집하거나 표시하지 않는다.
- 공개 프로필에는 customer code, 이메일, OAuth 연결 수단을 포함하지 않는다.
- 기존 `(provider, providerUserId)` 연결 사용자는 같은 User로 계속 로그인한다.
- 기존 닉네임과 `독자_xxxx` 자동 생성 동작을 보존한다.

## Review Focus

- 카카오 응답에서 이메일이 없을 때도 새 사용자·customer code·provider 연결이 모두 생성되어야 한다. Task 2에서 검증한다.
- 서로 다른 provider가 동일한 이메일을 반환해도 자동 병합되지 않아야 한다. Task 2에서 검증한다.
- 명시적 연결 중 provider가 다른 활성 사용자에 이미 속하면 기존 연결을 바꾸지 않고 거부해야 한다. Task 3에서 검증한다.
- 탈퇴 뒤 같은 OAuth 계정으로 가입하면 과거 익명화 계정이 복구되지 않고 새 customer code를 받아야 한다. Task 4에서 검증한다.
- 이메일 null 계정의 마이페이지는 이메일 행을 표시하지 않지만 customer code·가입일·provider 목록은 표시해야 한다. Task 5에서 검증한다.

---

### Task 1: customer code migration과 엔티티 매핑

**Files:**
- Create: `api/src/main/resources/db/migration/V2026093001__user_customer_code.sql`
- Modify: `api/src/main/java/com/chaekdojang/api/domain/user/User.java`
- Test: `api/src/test/java/com/chaekdojang/api/domain/user/CustomerCodeMigrationTest.java`

**Interfaces:**
- Produces: `User#getCustomerCode(): String`, DB가 생성하는 unique immutable `customer_code`.

- [ ] **Step 1: 기존·새 사용자 customer code의 형식과 중복 불가를 검증하는 failing migration test를 작성한다**

- [ ] **Step 2: 해당 테스트를 실행해 migration/매핑 부재로 실패함을 확인한다**

- [ ] **Step 3: 별도 sequence·before-insert trigger·backfill·unique/not-null 제약을 가진 Flyway migration과 read-only 엔티티 매핑을 구현한다**

- [ ] **Step 4: customer code 테스트를 재실행해 통과를 확인한다**

- [ ] **Step 5: 변경 파일을 커밋한다**

### Task 2: provider 식별만 사용하는 OAuth 신규가입과 재로그인

**Files:**
- Modify: `api/src/main/java/com/chaekdojang/api/domain/user/OAuthRegistrationService.java`
- Modify: `api/src/main/java/com/chaekdojang/api/domain/user/UserAuthProviderRepository.java`
- Test: `api/src/test/java/com/chaekdojang/api/domain/user/OAuthRegistrationServiceTest.java`

**Interfaces:**
- Consumes: `User#getCustomerCode()` from Task 1.
- Produces: `getOrRegister(AuthProvider, Map<String, Object>)` that uses only provider identity for an existing account lookup.

- [ ] **Step 1: 이메일 없는 카카오 가입, 동일 provider 재로그인, 같은 이메일의 다른 provider 독립가입을 검증하는 failing service tests를 작성한다**

- [ ] **Step 2: 해당 테스트를 실행해 현재 이메일 자동 병합 동작이 실패로 드러나는지 확인한다**

- [ ] **Step 3: `OAuthRegistrationService`에서 이메일 사용자 조회·병합을 제거하고 provider identity 미연결 시 항상 새 User를 생성하도록 구현한다**

- [ ] **Step 4: OAuth service tests를 재실행해 통과를 확인한다**

- [ ] **Step 5: 변경 파일을 커밋한다**

### Task 3: 명시적 OAuth 로그인 수단 연결

**Files:**
- Modify: `api/src/main/java/com/chaekdojang/api/domain/user/UserController.java`
- Modify: `api/src/main/java/com/chaekdojang/api/domain/user/OAuthRegistrationService.java`
- Modify: `api/src/main/java/com/chaekdojang/api/global/security/OAuthUserService.java`
- Modify: `api/src/main/java/com/chaekdojang/api/global/security/OAuthSuccessHandler.java`
- Modify: `api/src/main/java/com/chaekdojang/api/global/exception/ErrorCode.java`
- Test: `api/src/test/java/com/chaekdojang/api/domain/user/OAuthRegistrationServiceTest.java`

**Interfaces:**
- Produces: `POST /api/users/me/auth-providers/{provider}/link`, OAuth callback result that indicates a completed link without creating a second account.

- [ ] **Step 1: 같은 현재 사용자에 provider를 연결하고, 다른 활성 사용자에 연결된 provider를 거부하는 failing tests를 작성한다**

- [ ] **Step 2: 테스트를 실행해 명시적 연결 API/service 부재로 실패함을 확인한다**

- [ ] **Step 3: JWT 인증 사용자 id를 OAuth session marker에 저장하는 시작 endpoint와, callback에서 marker를 소비하여 provider 연결을 수행하는 최소 흐름을 구현한다**

- [ ] **Step 4: 명시적 연결 tests를 재실행해 통과를 확인한다**

- [ ] **Step 5: 변경 파일을 커밋한다**

### Task 4: 탈퇴 익명화와 구독 이력 보존

**Files:**
- Modify: `api/src/main/java/com/chaekdojang/api/domain/subscription/SubscriptionRepository.java`
- Modify: `api/src/main/java/com/chaekdojang/api/domain/user/UserService.java`
- Test: `api/src/test/java/com/chaekdojang/api/domain/user/UserServiceDeletionTest.java`

**Interfaces:**
- Produces: 탈퇴 시 provider 연결 제거, profile 익명화, active subscription 비활성화 및 이력 보존.

- [ ] **Step 1: 탈퇴 후 subscription 행이 삭제되지 않고 비활성화되며 동일 provider 재가입이 새 User가 되는 failing tests를 작성한다**

- [ ] **Step 2: 테스트를 실행해 현재 `deleteAllByUserId` 동작으로 실패함을 확인한다**

- [ ] **Step 3: subscription 삭제를 bulk inactive update로 교체하고 기존 익명화·provider 삭제를 유지한다**

- [ ] **Step 4: 탈퇴 tests를 재실행해 통과를 확인한다**

- [ ] **Step 5: 변경 파일을 커밋한다**

### Task 5: 본인 전용 계정 정보 API와 마이페이지

**Files:**
- Modify: `api/src/main/java/com/chaekdojang/api/domain/user/dto/UserProfileResponse.java`
- Modify: `api/src/main/java/com/chaekdojang/api/domain/user/UserService.java`
- Modify: `app/profile/page.tsx`
- Test: `api/src/test/java/com/chaekdojang/api/domain/user/UserProfileResponseTest.java`

**Interfaces:**
- Produces: `GET /api/users/me`의 `customerCode`, nullable `email`, `createdAt`, `authProviders`와 마이페이지 계정 정보 섹션.

- [ ] **Step 1: 내 프로필 응답에 account fields가 포함되고 public profile 변환에는 포함되지 않는 failing DTO tests를 작성한다**

- [ ] **Step 2: 테스트를 실행해 새 필드 부재로 실패함을 확인한다**

- [ ] **Step 3: provider repository를 사용해 본인 응답을 확장하고, 프론트 타입·계정 정보 카드·provider 연결 시작 버튼을 구현한다**

- [ ] **Step 4: DTO tests와 프론트 type/build 검증을 실행해 통과를 확인한다**

- [ ] **Step 5: 변경 파일을 커밋한다**

### Task 6: 전체 회귀 검증

**Files:**
- Modify: 필요한 경우 Task 1-5 테스트 파일만

**Interfaces:**
- Consumes: Tasks 1-5의 API·migration·UI.

- [ ] **Step 1: Flyway migration 적용 테스트와 customer code uniqueness를 확인한다**

- [ ] **Step 2: `./gradlew.bat test`를 실행해 전체 백엔드 회귀를 확인한다**

- [ ] **Step 3: `npm run build`를 실행해 프론트 production build를 확인한다**

- [ ] **Step 4: 요구사항 체크리스트와 변경 diff를 대조해 누락을 확인한다**

- [ ] **Step 5: 최종 변경을 커밋한다**
