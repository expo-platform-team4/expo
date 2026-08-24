-- =============================================================================
-- V202608201545__seed_default_banner_slot.sql
--
-- banner_slots 를 채우는 관리 API가 없어(B-API-019~024 목록에 슬롯 CRUD 없음),
-- 배너 승인(B-API-022) 시 연결할 슬롯이 하나도 없는 상태였다.
-- MVP 범위의 메인 배너 슬롯 1개를 시드로 넣는다.
--
-- max_active_count = 5 는 B-API-024 "현재 노출 가능한 메인 배너 최대 5개" 요구사항의
-- 근거 값이다. width_px/height_px 는 명세에 없어 통상적인 메인 배너 규격으로 추정했다.
-- =============================================================================

INSERT INTO banner_slots (slot_code, name, max_active_count, width_px, height_px, active)
SELECT 'MAIN_TOP', '메인 상단 배너', 5, 1200, 400, TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM banner_slots WHERE slot_code = 'MAIN_TOP'
);
