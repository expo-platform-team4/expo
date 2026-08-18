-- 현재 이 플랫폼은 킨텍스 한 곳만 다룬다: 장소 1개, 그 안에 홀(전시장) 2개, 각 홀 안에 구역(1~5홀) 5개.
-- 서비스 계층에서도 같은 검사를 먼저 하지만, 동시 요청 사이의 초과 삽입까지 막으려면 DB 트리거가 최종
-- 방어선이 되어야 한다. 개수만 세는 단순 검사라 EXCLUDE/CHECK 로는 표현이 안 돼 트리거로 구현한다.
--
-- COUNT(*) 만으로는 동시 삽입을 못 막는다. 두 트랜잭션이 동시에 같은 부모(장소 전체·같은 홀·같은
-- 구역의 부모 홀)에 자식을 추가하면, 서로의 커밋 전 행을 못 보고 둘 다 COUNT 가 제한 미만이라고 읽어
-- 제한을 넘겨서 커밋될 수 있다(TOCTOU 경쟁 상태). 그래서 COUNT 를 세기 전에 같은 범위(scope)를 대상으로
-- 트랜잭션 advisory lock 을 걸어 같은 부모에 대한 삽입을 직렬화한다. 범위가 다르면(예: 다른 홀에 구역
-- 추가) 잠금 키가 달라 서로 안 막는다.
CREATE OR REPLACE FUNCTION check_virtual_venue_limit()
    RETURNS TRIGGER AS
$$
BEGIN
    PERFORM pg_advisory_xact_lock(hashtext('virtual_venue_limit'));
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
    PERFORM pg_advisory_xact_lock(hashtext('venue_hall_limit:' || NEW.venue_id));
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
    PERFORM pg_advisory_xact_lock(hashtext('venue_zone_limit:' || NEW.hall_id));
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
