# ERD 머메이드 다이어그램

발표용 ERD 캡처 자료. 현재 마이그레이션(`V1__init_schema.sql` + 이후 모든 `V*.sql`) 기준으로
직접 추출했다 — `docs/init_table_schema.md`는 2026-07-31 스냅샷이라 참고만 하고 정본으로 쓰지 않았다.
물리 테이블 71개, 뷰 18개.

## 사용법

각 `.mmd` 파일을 [Mermaid Live Editor](https://mermaid.live)에 붙여도 되고, 이미 렌더링된
`.svg`(무한 확대, 발표 자료 삽입용)·`.png`(scale 3, 바로 첨부용)를 그대로 쓰면 된다.
다시 렌더링하려면:

```bash
npx @mermaid-js/mermaid-cli -i 파일명.mmd -o 파일명.png -b white -s 3
```

## 1. 전체 조망

| 파일 | 내용 |
| --- | --- |
| `00_overview` | 물리 테이블 71개를 11개 도메인 클러스터로 묶은 flowchart. 컬럼 없이 테이블명과 FK 방향만 표시 — "한눈에 보는 전체 구조" 용. |

## 2. 클러스터별 상세 (erDiagram, 전체 컬럼 표기)

각 파일은 해당 클러스터의 테이블은 전체 컬럼·PK/FK/UK를 표기하고, 다른 클러스터를 참조하는
테이블은 `(외부)` 표시가 붙은 PK 전용 스텁으로만 걸쳐 그린다 — 그 테이블의 실제 상세는 자신의
클러스터 파일을 보면 된다.

| 파일 | 클러스터 | 테이블 수 |
| --- | --- | --- |
| `01_common_auth` | 공통 · 인증 및 사용자 관리 | 8 |
| `02_venue` | 가상 장소 (킨텍스 1곳/홀 2개/구역 5개 고정) | 5 |
| `03_expo_management` | 박람회 관리 | 14 |
| `04_ticket_inventory` | 티켓 및 재고 | 3 |
| `05_order_payment_refund` | 주문 · 결제 · 환불 | 6 |
| `06_checkin` | 발권 및 QR 체크인 | 3 |
| `07_notification` | 알림 | 2 |
| `08_banner` | 광고 배너 | 4 |
| `09_recruitment_participation` | 기업 모집공고 및 참여 신청 | 9 |
| `10_booth` | 부스 판매 · 결제 · 배정 · 관리 | 11 |
| `11_settlement` | 정산 및 회계 | 6 |

## 3. 뷰(View) 매핑

SQL 뷰(`R__*.sql`) 18개는 PK/FK가 없는 조회 전용이라 ER 표기 대신 "뷰 → 참조 테이블" flowchart로
따로 뺐다.

| 파일 | 내용 |
| --- | --- |
| `views_member_mypage` | 검색·노출(v_public_expo_cards) + 일반 회원 마이페이지 3장 |
| `views_client_dashboard` | 클라이언트(주최사) 대시보드 6장 |
| `views_admin_dashboard` | 관리자 대시보드 및 운영 8장 (감사 로그 포함) |

## 클러스터링 기준과 알아둘 것

- 클러스터 경계는 `V1__init_schema.sql`의 섹션 주석(`com.expo.*` 백엔드 패키지와 1:1 대응)을
  그대로 따랐다. 문서(`docs/init_table_schema.md`)의 6-1~6-15 번호와는 일부 다르게 묶었다
  (예: `가상 장소`는 문서상 6-14지만 booth_templates/booths/booth_products는 실제로는
  `com.expo.booth` 패키지라 10번 부스 클러스터에 있다).
- V1 이후 추가된 테이블 4개를 반영했다: `email_verifications`, `recruitment_notice_request_zones`,
  `venue_reservation_histories`, `expo_opening_request_categories`.
- 컬럼 변경 드리프트도 반영했다: `file_metadata.access_level` 추가, `phone_verifications`에
  `signup_token_*`/`version` 추가, `kakao_message_histories` → `message_histories` 개명 +
  `channel` 컬럼 추가, `ticket_orders.order_number` 40→50자, `recruitment_notice_requests`의
  `venue_zone_id` 제거(→ zones 테이블로 다대다 전환) 및 `expo_id` 추가,
  `recruitment_notices.venue_reservation_id` 제거(1:1→1:N 전환), `check_in_histories`의
  `issued_ticket_id` NULL 허용 등.
- 복합 FK(장소 계층 검증, 부스 구매 체인 정합성 등)와 EXCLUDE/트리거 제약은 다이어그램에서
  뺐다 — 무결성 로직이지 구조적 관계가 아니라 넣으면 오히려 읽기 어려워진다. 필요하면
  `V1__init_schema.sql`의 "16. FK 제약"·"17. 특수 제약" 절을 본다.
