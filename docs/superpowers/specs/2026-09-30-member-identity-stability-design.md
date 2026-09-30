# 회원 식별 안정화 설계

## 목적

유료 서비스 준비를 위해 `users.id`를 내부 canonical user id로 유지하면서,
개인정보를 추가 수집하지 않는 고객지원용 식별자와 안전한 OAuth 연결 구조를 마련한다.

## 범위와 제약

- OAuth 식별은 `(provider, providerUserId)`만 신뢰한다.
- OAuth 이메일은 nullable이며 가입·유료 기능의 필수 조건이 아니다.
- 실명과 휴대폰번호는 수집하거나 표시하지 않는다.
- 기존 활성 사용자는 현재 OAuth 로그인 수단으로 계속 같은 `User`에 로그인한다.
- 공개 프로필에는 내부 id, customer code, 이메일, OAuth 수단을 노출하지 않는다.

## 데이터 모델

`users.customer_code`를 추가한다.

- 형식: `CD-` 뒤의 6자리 이상 숫자 (`CD-000184`).
- DB의 별도 sequence와 before-insert trigger가 생성하며, unique 및 not null 제약을 둔다.
- 기존 사용자에게는 생성 시각/id 순서로 값을 backfill하고 sequence를 다음 값으로 맞춘다.
- customer code는 변경하거나 재사용하지 않는다. 탈퇴 사용자도 과거 결제·고객지원 참조를 위해 보존한다.
- `users.id`는 모든 내부 관계와 JWT subject의 canonical id로 유지한다.

`user_auth_providers`의 `(provider, provider_user_id)` unique 제약은 유지한다.

## OAuth 흐름

일반 로그인은 먼저 `(provider, providerUserId)`를 조회한다.

1. 기존 활성 연결이 있으면 연결된 동일 `User`로 로그인한다.
2. 연결이 없으면 이메일 유무·동일 이메일 여부와 무관하게 새 `User` 및 provider 연결을 생성한다.
3. 탈퇴 사용자에 남아 있는 provider 연결은 제거된 상태이므로 같은 social account의 재가입은 새 사용자로 생성된다.

자동 이메일 병합은 제거한다. 이메일은 OAuth 제공 범위 변경, 재사용, 공유 계정 등의 경우 계정 소유 증명이 아니어서 자동 병합 근거가 될 수 없다.

여러 로그인 수단을 하나의 계정에 추가하는 경우에만 명시적 연결을 허용한다.

1. 로그인된 사용자가 `POST /api/users/me/auth-providers/{provider}/link`를 호출한다.
2. 서버는 현재 JWT 사용자 id를 OAuth state 검증용 HTTP session에 한 번만 저장하고 해당 provider 인가 URL로 redirect한다.
3. OAuth callback에서 해당 session이 있으면 새 OAuth 인증 결과를 현재 사용자에 연결한다.
4. 이미 다른 활성 사용자에 연결된 OAuth 계정은 거부한다.
5. 연결이 끝나면 session marker를 삭제하고 마이페이지로 돌아간다.

이 방식은 현재 계정 로그인과 추가하려는 OAuth 계정의 provider 인증을 모두 요구한다.

## 내 계정 API와 화면

`GET /api/users/me`의 본인 전용 응답에 다음을 추가한다.

- `customerCode`
- `email` (null 가능)
- `createdAt`
- `authProviders` (provider enum 목록)

프론트 마이페이지는 위 항목과 연결된 로그인 수단 목록을 표시한다. 이메일 값이 없으면 이메일 행 자체를 표시하지 않는다. 각 미연결 provider에 대해 명시적 연결 버튼을 제공한다.

## 탈퇴와 결제 이력

탈퇴는 일반 프로필 개인정보와 개인 활동 데이터를 계속 익명화·정리한다. OAuth 연결도 제거해 새 가입자가 과거 계정에 접근할 수 없게 한다.

`subscriptions`는 삭제하지 않는다. 탈퇴 시 활성 구독만 비활성화하고, 이전 canonical user id 및 customer code에 연결된 결제/구독 기록을 보존한다. 새 재가입 계정은 새 user id와 customer code를 받으므로 이전 결제 기록과 섞이지 않는다.

## 검증

- 이메일 없는 카카오 OAuth 신규 가입
- provider 식별자가 같은 OAuth 재로그인
- 네이버·구글·카카오별 독립 신규 가입 (동일 이메일 자동 병합 없음)
- customer code 생성·중복 방지와 migration backfill
- 명시적 OAuth 연결과 다른 사용자에 연결된 provider 거부
- 탈퇴 후 익명화, 구독 보존/비활성화, 동일 social account 재가입
- 닉네임 변경과 중복 거부, 자동 생성 `독자_xxxx` 유지
