-- 박람회 개최 신청 시점에 고른 카테고리를, 승인되어 실제 expos 행이 생기기 전까지 담아 두는 테이블이다.
-- expo_categories(expo_id, category_id)는 이미 있지만 expo_id 가 NOT NULL FK 라 신청 단계에서는 쓸 수 없다.
-- 승인되는 순간(ExpoOpeningRequestService.approve()) 여기 담긴 category_id 들을 그대로 expo_categories 로 복사한다.
CREATE TABLE expo_opening_request_categories (
    opening_request_id BIGINT      NOT NULL REFERENCES expo_opening_requests (id),
    category_id         BIGINT      NOT NULL REFERENCES categories (id),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (opening_request_id, category_id)
);

CREATE INDEX idx_expo_opening_request_categories_category
    ON expo_opening_request_categories (category_id);
