# 운세 AI 운영 관리자 구현 계획

### Task 1: 통계 조회 기반

**Files:** migration, `FortuneOperationsAdminService`, `FortuneOperationsAdminController`, service tests

1. 기간 파라미터와 KPI 응답을 대상으로 실패 테스트를 만든다.
2. 통계·환불·정합성 테이블과 집계 인덱스를 migration으로 추가한다.
3. 기존 원장·주문·AI usage를 기간으로 집계하는 ADMIN API를 구현한다.
4. 일반 사용자 차단과 ADMIN 허용을 검증한다.

### Task 2: 결제 운영 액션과 감사

**Files:** order service/controller, audit service tests

1. 재처리/정합성/환불 요청의 권한·사유 검증 실패 테스트를 만든다.
2. PortOne 상태 재조회와 미지급 fulfillment 재처리, 환불 요청 저장을 구현한다.
3. 변경 작업과 질문권 조정을 감사 로그에 기록한다.

### Task 3: 퍼널 이벤트

**Files:** metric service/controller tests, fortune web pages

1. 허용 이벤트와 민감 metadata 차단 테스트를 만든다.
2. 서버 이벤트 허용 목록과 metadata 정제를 구현한다.
3. 운세 화면·AI 질문·결제 화면에서 최소 이벤트를 전송한다.

### Task 4: 관리자 화면

**Files:** `app/admin/page.tsx`, admin operation components

1. 기간/요약 API 응답 렌더링 테스트를 만든다.
2. 운세 운영 탭과 KPI, 시계열, 퍼널, 주문·사용자 표를 구현한다.
3. 위험 액션 확인 모달과 결과 메시지를 구현한다.

### Task 5: 검증과 배포

1. API 테스트와 프론트 타입/빌드를 실행한다.
2. migration SQL과 인덱스를 검토한다.
3. 두 staging 브랜치에 커밋·푸시한다.
