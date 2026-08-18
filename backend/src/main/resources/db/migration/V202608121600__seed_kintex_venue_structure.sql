-- 이 플랫폼이 실제로 다루는 유일한 장소인 킨텍스의 실제 구조를 시드로 넣는다.
-- 장소·홀·구역 개수는 이미 트리거로 (1개, 홀당 2개, 구역 5개)로 제한돼 있으므로,
-- 다른 값이 먼저 들어가 있는 환경(예: 로컬 테스트 데이터)에서는 조용히 건너뛴다.
--
-- 구역(홀) 코드는 킨텍스 실제 명칭대로 제1전시장·제2전시장을 넘나들며 1~10 으로 연속되게 넣는다
-- (제1전시장 1~5, 제2전시장 6~10). 각 홀 안에서만 코드 중복을 막는 스키마라 두 전시장에서
-- 번호를 다시 1부터 써도 제약상 문제는 없지만, 실제 명칭과 헷갈리지 않도록 연속 번호를 쓴다.
--
-- 면적은 공식 홈페이지 기준 값이지만 가로×세로 분할 값은 공개돼 있지 않아 width·depth 는 비워둔다.
-- max_booth_count 는 업계 관례인 부스당 9㎡(3m x 3m) 기준으로 면적에서 역산한 추정치다.
-- 공식 부스 수 자료가 확보되면 반드시 실제 값으로 교체해야 한다 (현재 이 값을 고치는 API는 없다).
DO $$
DECLARE
    v_venue_id BIGINT;
    v_hall1_id BIGINT;
    v_hall2_id BIGINT;
BEGIN
    IF EXISTS (SELECT 1 FROM virtual_venues) THEN
        RETURN;
    END IF;

    INSERT INTO virtual_venues (name, address, region_code, operational_status)
    VALUES ('킨텍스', '경기도 고양시 일산서구 킨텍스로 217-60', 'GYEONGGI', 'ACTIVE')
    RETURNING id INTO v_venue_id;

    INSERT INTO venue_halls (venue_id, hall_code, name, operational_status)
    VALUES (v_venue_id, 'EXPO1', '제1전시장', 'ACTIVE')
    RETURNING id INTO v_hall1_id;

    INSERT INTO venue_halls (venue_id, hall_code, name, operational_status)
    VALUES (v_venue_id, 'EXPO2', '제2전시장', 'ACTIVE')
    RETURNING id INTO v_hall2_id;

    INSERT INTO venue_zones (hall_id, zone_code, name, max_booth_count, operational_status) VALUES
        (v_hall1_id, '1',  '1홀', 1179, 'ACTIVE'),
        (v_hall1_id, '2',  '2홀', 1197, 'ACTIVE'),
        (v_hall1_id, '3',  '3홀', 1197, 'ACTIVE'),
        (v_hall1_id, '4',  '4홀', 1197, 'ACTIVE'),
        (v_hall1_id, '5',  '5홀', 1179, 'ACTIVE'),
        (v_hall2_id, '6',  '6홀',  620, 'ACTIVE'),
        (v_hall2_id, '7',  '7홀', 1254, 'ACTIVE'),
        (v_hall2_id, '8',  '8홀', 1254, 'ACTIVE'),
        (v_hall2_id, '9',  '9홀', 1471, 'ACTIVE'),
        (v_hall2_id, '10', '10홀', 1452, 'ACTIVE');
END $$;
