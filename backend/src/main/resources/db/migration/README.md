# Flyway 마이그레이션

`V{버전}__{설명}.sql` 규칙으로 파일을 추가한다. 예: `V1__init.sql`, `V2__add_ticket_table.sql`

## 규칙

1. **이미 머지된 마이그레이션 파일은 수정하지 않는다.** 다른 사람 DB에는 이미 적용된 상태라 체크섬이 깨진다. 항상 새 버전 파일을 추가한다.
2. **버전 번호는 PR 단위로 겹치지 않게 잡는다.** 두 사람이 동시에 `V3__` 를 만들면 충돌한다. 머지 전에 번호를 확인한다.
3. **가급적 표준 SQL 로 쓴다.** 이 SQL 은 local(PostgreSQL)과 test(H2, `MODE=PostgreSQL`) 양쪽에서 실행된다. H2 는 JSONB, 배열 타입, `ON CONFLICT` 세부 문법을 완전히 지원하지 않는다.
4. PostgreSQL 전용 기능이 꼭 필요해지면 `db/migration`(공통) + `db/migration-pg`(PostgreSQL 전용)로 나누고 프로필별로 `spring.flyway.locations` 를 분기한다. 그 분기가 감당이 안 되면 테스트를 Testcontainers PostgreSQL 로 전환한다.

## 엔티티와의 관계

모든 프로필에서 `spring.jpa.hibernate.ddl-auto: validate` 다. JPA 가 테이블을 만들지 않는다.
엔티티를 추가·수정했으면 **반드시** 대응하는 마이그레이션 파일을 같이 커밋한다. 안 하면 기동 시점에 검증 실패한다.

## 첫 마이그레이션 추가 후

`application.yml` 의 `spring.flyway.fail-on-missing-locations` 를 `true` 로 올린다.
(지금은 마이그레이션이 하나도 없어 기동을 막지 않으려고 `false` 로 두었다.)
