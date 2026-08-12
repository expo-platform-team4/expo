-- 현재 이 플랫폼은 킨텍스 한 곳만 다룬다: 장소 1개, 그 안에 홀(전시장) 2개, 각 홀 안에 구역(1~5홀) 5개.
-- 서비스 계층에서도 같은 검사를 먼저 하지만, 동시 요청 사이의 초과 삽입까지 막으려면 DB 트리거가 최종
-- 방어선이 되어야 한다. 개수만 세는 단순 검사라 EXCLUDE/CHECK 로는 표현이 안 돼 트리거로 구현한다.

CREATE OR REPLACE FUNCTION check_virtual_venue_limit()
    RETURNS TRIGGER AS
$$
BEGIN
    IF (SELECT COUNT(*) FROM virtual_venues) >= 1 THEN
        RAISE EXCEPTION 'virtual_venue_limit_exceeded' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_virtual_venue_limit
    BEFORE INSERT ON virtual_venues
    FOR EACH ROW
EXECUTE FUNCTION check_virtual_venue_limit();

CREATE OR REPLACE FUNCTION check_venue_hall_limit()
    RETURNS TRIGGER AS
$$
BEGIN
    IF (SELECT COUNT(*) FROM venue_halls WHERE venue_id = NEW.venue_id) >= 2 THEN
        RAISE EXCEPTION 'venue_hall_limit_exceeded' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_venue_hall_limit
    BEFORE INSERT ON venue_halls
    FOR EACH ROW
EXECUTE FUNCTION check_venue_hall_limit();

CREATE OR REPLACE FUNCTION check_venue_zone_limit()
    RETURNS TRIGGER AS
$$
BEGIN
    IF (SELECT COUNT(*) FROM venue_zones WHERE hall_id = NEW.hall_id) >= 5 THEN
        RAISE EXCEPTION 'venue_zone_limit_exceeded' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_venue_zone_limit
    BEFORE INSERT ON venue_zones
    FOR EACH ROW
EXECUTE FUNCTION check_venue_zone_limit();
