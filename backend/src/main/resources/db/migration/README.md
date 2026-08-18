# Flyway 마이그레이션

`V{연월일시분}__{설명}.sql` 규칙으로 파일을 추가한다. 예: `V202608071501__add_sms_notification_channel.sql`

버전 자리는 파일을 만드는 시각을 `yyyyMMddHHmm` 으로 적는다. 숫자만 쓴다 — Flyway 는 버전 문자열의
밑줄을 점과 같은 구분자로 보기 때문에 `V_202608071501__` 처럼 앞에 밑줄을 붙이면 파싱이 실패한다.
설명 앞의 `__` 두 개만 구분자다.

> **변경 이력** — 2026-08-07 이전에는 `V{버전}__{설명}.sql` (`V1__init.sql`, `V2__add_ticket_table.sql`)
> 로 손번호를 매겼고, "버전 번호는 PR 단위로 겹치지 않게 잡는다. 머지 전에 번호를 확인한다" 는 규칙이
> 충돌을 사람에게 떠넘기고 있었다. 파일을 만든 시각을 버전으로 쓰면 조율 없이 겹치지 않는다.
> **앞으로는 `V{연월일시분}` 이 정본이다.** 이미 머지된 `V1__init_schema.sql` 은 이름을 바꾸지 않는다 —
> 파일명이 바뀌면 적용 이력의 version·description 과 어긋나 팀원 DB 에서 기동이 실패한다.

## 규칙

1. **이미 머지된 마이그레이션 파일은 수정하지 않는다.** 다른 사람 DB에는 이미 적용된 상태라 체크섬이 깨진다. 항상 새 버전 파일을 추가한다.
2. **PostgreSQL 네이티브 SQL 을 쓴다.** 표준 SQL 유지는 V1 시점에 포기했다. JSONB·`EXCLUDE`·부분 인덱스가 필요해졌고, 낮추면 명세가 요구하는 무결성을 코드로 떠넘기게 된다. 아래 "PostgreSQL 전용이다" 절 참조.
3. **`db/migration` 하나만 쓴다.** PostgreSQL 전용으로 `db/migration-pg` 를 나누지 않는다.

## PostgreSQL 전용이다 — 테스트도 PostgreSQL 에서 돈다

**`V1__init_schema.sql` 과 뷰 16개는 PostgreSQL 전용이다.** 스키마가 아래 셋을 요구하는데,
분기해서 낮추면 명세가 요구하는 무결성 보장을 코드로 떠넘기게 되어 네이티브로 가기로 했다.

| 기능 | 쓰는 곳 | 왜 필요한가 |
|-|-|-|
| `JSONB` | 12개 컬럼 (`before_data`, `payload`, `included_items` 등) | 변경 이력·PG 응답·부스 구성 스냅샷 |
| `EXCLUDE USING gist` | `venue_reservations` | 장소·홀·구역·기간 중복을 DB 가 최종 차단. 명세가 "동시 요청은 이 제약이 최종적으로 차단한다"고 명시 |
| 부분 UNIQUE 인덱스 | `booth_reservations` | 한 부스 상품에 활성 임시 확보 1건. 명세가 SQL 을 그대로 제시 |

한동안 test 프로필은 H2 였고, 위 구문 때문에 **Flyway 를 끈 채** JPA 가 엔티티로 스키마를 만들었다.
그 구성은 테스트를 통과시키지만 **엔티티와 마이그레이션이 어긋나도 아무도 모르는 상태**를 만든다 —
검사 대상과 검사 기준이 둘 다 엔티티에서 나오기 때문이다.

**지금은 Testcontainers 가 실제 PostgreSQL 을 띄운다.** 매 실행마다 빈 DB 에 마이그레이션을 처음부터
적용하고, `ddl-auto: validate` 가 엔티티와 대조한다. 즉 **마이그레이션을 고치고 엔티티를 안 고치면
(혹은 그 반대면) `ExpoApplicationTests.contextLoads` 가 깨진다.**

```
missing column [drift_probe_column] in table [notifications]
```

일부러 어긋나게 만들어 확인한 메시지다. 컨테이너 설정은
`backend/src/test/java/com/expo/support/PostgresContainerConfig.java` 에 있고,
이미지는 `docker-compose.yml` 과 같은 `postgres:18-alpine` 이다.

> **Docker 가 필요하다.** 스프링 컨텍스트를 띄우는 테스트만 컨테이너를 쓰므로, Docker 가 없어도
> 나머지 단위 테스트는 그대로 돈다.

## 뷰

뷰는 `R__NN_v_이름.sql` 반복 마이그레이션으로 관리한다. 버전 번호가 없고 파일 체크섬이 바뀌면
Flyway 가 자동으로 재적용한다. 즉 **뷰 파일은 직접 고쳐도 된다** — 1번 규칙의 예외다.
`NN` 은 알파벳 순 실행 순서를 고정하기 위한 접두 번호다.

## 엔티티와의 관계

모든 프로필에서 `spring.jpa.hibernate.ddl-auto: validate` 다. JPA 가 테이블을 만들지 않는다.
엔티티를 추가·수정했으면 **반드시** 대응하는 마이그레이션 파일을 같이 커밋한다. 안 하면 기동 시점에 검증 실패한다.
