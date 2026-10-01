# 운세 AI 운영 관리자 설계

## 목적

운영자가 기간별 매출, 질문권, AI 원가, 결제 정합성 및 운세 퍼널 이탈을 한 화면에서 판단한다. 일반 사용자에게 운영 데이터나 개인정보를 노출하지 않는다.

## 범위와 경계

- 기존 `ADMIN`/`SUPER_ADMIN`과 `/api/admin/**` 보안 규칙을 재사용한다.
- 기존 `metric_events`, `ai_credit_ledger`, `ai_usage_records`, `ai_credit_orders`, `admin_audit_logs`를 우선 사용한다.
- 운세 원문, 생년월일, 질문 본문은 이벤트 metadata 또는 관리자 응답에 넣지 않는다.
- 실제 환불 실행은 PortOne 환불 API 검증이 완료되기 전까지 요청 기록과 상태 조회 구조만 만들며, 금전 환불을 추정해서 실행하지 않는다.

## 데이터 모델

`ai_credit_refunds`는 주문별 환불 요청·상태·금액·사유·처리자·PortOne 환불 식별자를 보관한다. `payment_reconciliation_runs`는 주문과 PortOne 상태의 불일치를 검사한 실행 결과를 보관한다. 기존 원장과 주문은 변경하지 않는다.

통계 성능을 위해 `ai_credit_orders(status, created_at)`, `ai_credit_ledger(type, created_at)`, `ai_usage_records(feature, completed_at)`, `metric_events(event_type, created_at)`, `metric_events(user_id, event_type, created_at)` 인덱스를 추가한다.

## API와 권한

- `GET /api/admin/fortune-operations/summary`: ADMIN 이상 기간별 KPI·시계열·퍼널.
- `GET /api/admin/fortune-operations/orders`: ADMIN 이상 주문 목록.
- `GET /api/admin/fortune-operations/users/{id}`: ADMIN 이상 비식별 운영 상세.
- `POST /api/admin/fortune-operations/orders/{id}/reconcile`: ADMIN 이상 PortOne 상태 재조회·정합성 기록.
- `POST /api/admin/fortune-operations/orders/{id}/fulfillment`: SUPER_ADMIN만, 이미 검증된 PAID 주문의 미지급 질문권 재처리.
- `POST /api/admin/fortune-operations/refunds`: SUPER_ADMIN만, 사유가 있는 환불 요청을 기록한다. 실제 PG 환불 실행은 PG 연동이 확정된 뒤 별도 구현한다.

모든 변경 작업은 `admin_audit_logs`에 관리자, 대상, 액션, 요약을 남긴다. 질문권 조정 API도 기존 감사 로그 호출을 추가한다.

## KPI 산식

- 신규 가입자: 기간 내 `users.created_at` 수.
- 명반 생성 사용자: `CHART_GENERATED` 고유 로그인 사용자와 비로그인 세션의 합.
- AI 질문 사용자: 기간 내 `ai_usage_records.user_id` 고유 수.
- 결제 사용자/건수/매출: `PAID` 주문 기준 고유 사용자/행 수/amount 합계.
- 환불액: `COMPLETED` 환불의 amount 합계.
- 순매출: 매출 - 환불액.
- 지급·사용 질문권: 각각 양수·음수 ledger amount의 절댓값 합계.
- AI 총 원가: usage record estimated_cost 합계.
- 질문당 평균 원가: 성공 usage의 총 원가 / 성공 호출 수.
- AI 원가율: AI 총 원가 / 순매출. 순매출이 0 이하이면 데이터 없음.

## 이벤트

기존 `metric_events`에 다음 이름을 기록한다: `FORTUNE_PAGE_VIEW`, `FORTUNE_PROFILE_STARTED`, `FORTUNE_PROFILE_COMPLETED`, `CHART_GENERATED`, `AI_CHAT_STARTED`, `FREE_CREDIT_USED`, `PAYWALL_VIEWED`, `PRODUCT_SELECTED`, `CHECKOUT_STARTED`, `PAYMENT_COMPLETED`, `PAID_AI_USED`, `RETURN_VISIT`.

metadata는 productCode, model, creditType처럼 비식별 운영 값만 허용한다. 금지 키 목록은 질문, 이름, 이메일, 생년월일, 위치와 결제수단 관련 키까지 확장한다.

## 화면

`/admin?tab=fortune-operations`에 기간 선택, KPI 표, 매출·AI 원가 일별 시계열, 퍼널 표, 주문 표, 사용자 검색 및 질문권 원장을 표시한다. 표본이 20 미만인 퍼널 비율에는 원시 건수와 표본 적음 표시를 함께 둔다.

## 검증

일반 사용자 403, ADMIN/SUPER_ADMIN 허용, 기간 경계 KPI, 중복 결제 제외, 실패 AI 비용 포함, ledger 합계, 이벤트 metadata 정제, 감사 로그 기록을 테스트한다.
