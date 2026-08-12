-- 부스 주문 취소 후 같은 신청서로 재주문할 수 있도록, application_id 의 단일 컬럼 UNIQUE 를
-- "취소·만료되지 않은 주문 1건만" 허용하는 부분 UNIQUE 인덱스로 바꾼다.
--
-- 배경: V1 의 application_id UNIQUE 제약은 취소된 주문도 자리를 계속 차지한다. 결제 전 주문을
--       취소하고(BoothOrderService.cancel) 다시 주문을 생성하면(BoothOrderService.create) 취소된
--       옛 주문과 같은 application_id 로 새 행을 넣으려다 무결성 제약 위반으로 실패했다.
ALTER TABLE booth_orders DROP CONSTRAINT booth_orders_application_id_key;

CREATE UNIQUE INDEX uq_booth_orders_active_application
    ON booth_orders (application_id)
    WHERE status NOT IN ('CANCELED', 'EXPIRED');
