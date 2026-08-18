-- 관리자 심사·변경·결제·정산 운영 이력 통합 조회  →  com.expo.admin
-- 근거: 유스케이스 명세서 UC-30 "관리자가 참여 신청 운영 확인, 부스 결제·자동 배정, 티켓 결제·환불,
--       알림, 공개, 티켓·부스비 정산 등의 처리 이력을 조회"하고, 기본 흐름 2단계 "시스템이 처리자,
--       처리 시각, 변경 전·후 상태 및 오류 내용을 표시한다"를 근거로 공통 컬럼을 잡았다.
-- 주의: UNION ALL 구조와 log_type 리터럴은 추정이다. 9개 이력 테이블을 묶는다 — 결제 이벤트
--       이력(ticket_payment_histories, booth_payment_histories)은 관리자가 아니라 PG 응답으로
--       발생하므로 processor_admin_id 가 NULL 이다.
-- log_id 는 원본 이력 테이블의 PK 다. occurred_at 이 같은 행이 있을 수 있어 페이지 정렬 타이브레이커로 쓴다.
CREATE OR REPLACE VIEW v_admin_audit_logs AS
SELECT 'EXPO_REVIEW'::VARCHAR(30)      AS log_type,
       id                              AS log_id,
       expo_id                        AS target_id,
       decision::VARCHAR(40)          AS action,
       from_status,
       to_status,
       reason,
       reviewer_admin_id              AS processor_admin_id,
       reviewed_at                    AS occurred_at
FROM expo_review_histories
UNION ALL
SELECT 'EXPO_CHANGE'::VARCHAR(30),
       id,
       expo_id,
       'UPDATE'::VARCHAR(40),
       NULL::VARCHAR(20),
       NULL::VARCHAR(20),
       reason,
       changed_by_admin_id,
       created_at
FROM expo_change_histories
UNION ALL
SELECT 'BANNER_REVIEW'::VARCHAR(30),
       id,
       banner_application_id,
       decision::VARCHAR(40),
       from_status,
       to_status,
       reason,
       reviewer_admin_id,
       reviewed_at
FROM banner_review_histories
UNION ALL
SELECT 'RECRUITMENT_NOTICE_REQUEST'::VARCHAR(30),
       id,
       request_id,
       action_type::VARCHAR(40),
       from_status,
       to_status,
       reason,
       processed_by_admin_id,
       created_at
FROM recruitment_notice_request_histories
UNION ALL
SELECT 'RECRUITMENT_NOTICE'::VARCHAR(30),
       id,
       recruitment_notice_id,
       action_type::VARCHAR(40),
       NULL::VARCHAR(20),
       NULL::VARCHAR(20),
       reason,
       processed_by_admin_id,
       created_at
FROM recruitment_notice_histories
UNION ALL
SELECT 'PARTICIPATION_OPERATION'::VARCHAR(30),
       id,
       application_id,
       action_type::VARCHAR(40),
       NULL::VARCHAR(20),
       NULL::VARCHAR(20),
       message,
       processed_by_admin_id,
       created_at
FROM application_operation_histories
UNION ALL
SELECT 'TICKET_PAYMENT'::VARCHAR(30),
       id,
       ticket_payment_id,
       event_type::VARCHAR(40),
       from_status,
       to_status,
       NULL::TEXT,
       NULL::BIGINT,
       occurred_at
FROM ticket_payment_histories
UNION ALL
SELECT 'BOOTH_PAYMENT'::VARCHAR(30),
       id,
       booth_payment_id,
       event_type::VARCHAR(40),
       from_status,
       to_status,
       NULL::TEXT,
       NULL::BIGINT,
       occurred_at
FROM booth_payment_histories
UNION ALL
SELECT 'BOOTH_MANAGEMENT'::VARCHAR(30),
       id,
       booth_allocation_id,
       action_type::VARCHAR(40),
       NULL::VARCHAR(20),
       NULL::VARCHAR(20),
       reason,
       processed_by_admin_id,
       created_at
FROM booth_management_histories;
