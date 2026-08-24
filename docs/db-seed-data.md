# 로컬 DB 시드 데이터

## 요약

`backend/src/main/resources/db/seed/` 밑의 `.sql` 파일들은 **Flyway 가 실행하지 않는다.**
`db/migration` 과 달리 사람이 `docker exec -i expo-postgres psql` 로 손수 실행해야 하고,
운영 DB 에는 절대 들어가면 안 된다. 화면을 눌러보려면 결제·발권처럼 아직 API 가 없는
도메인의 데이터를 SQL 로 직접 채워야 하는데, 그 결과물이 여기 모여 있다.

시드 파일은 성격이 둘로 갈린다.

| 종류 | 예 | 몇 번 돌려도 되나 | 무엇을 채우나 |
| --- | --- | --- | --- |
| 시나리오 빌더 | `local_ticket_flow.sql`, `local_full_flow_expo7.sql` | O — 돌릴 때마다 새 주문이 하나씩 더 생기게 짜여 있다 | 특정 플로우 하나 (발권만, 부스+정산+체크인 전체 등) |
| 스냅샷 | `local_dev_snapshot_20260824.sql` | O(안전하지만 의미 없음) — `ON CONFLICT DO NOTHING` 이라 두 번째부턴 전부 스킵된다 | 특정 시점 로컬 DB 전체 (여러 시나리오가 뒤섞인 상태) |

## 언제 무엇을 쓰나

- **처음 로컬을 세팅하고 화면에 데이터가 좀 있었으면 좋겠다** → 스냅샷 하나만 실행한다.
  가장 빠르다. 대신 스냅샷을 만든 사람 로컬의 특정 시점을 그대로 복사한 것이라, 이후에
  스키마가 바뀌면 낡아진다.
- **특정 기능(발권, 체크인, 정산 등)을 처음부터 코드로 재현하고 싶다** → 시나리오 빌더를 쓴다.
  `NOT EXISTS` / `ON CONFLICT` 로 재실행에 안전하게 짜여 있고, 무엇을 왜 넣는지 파일
  상단에 설명이 있다.
- **결제 API 등이 이후에 생겨서 이 시드 없이도 화면을 만들 수 있게 됐다** → 그 시드 파일은
  지운다. SQL 로 우회하던 이유가 없어졌으면 더 이상 필요 없다.

## 실행 방법

전제 조건: `docker compose up -d` 로 DB 가 떠 있고, 백엔드를 한 번 기동해 Flyway 마이그레이션까지
적용된 상태 (`ddl-auto: validate` 라 마이그레이션이 먼저 돌아야 테이블이 생긴다).

```bash
docker exec -i expo-postgres psql -U expo -d expo \
  < backend/src/main/resources/db/seed/local_dev_snapshot_20260824.sql
```

`local_full_flow_expo7.sql` 처럼 QR 해시를 실제 백엔드와 맞춰야 하는 파일은 `-v qr_secret=...`
같은 psql 변수를 추가로 받는다. 파일 상단 주석에 정확한 실행 커맨드가 적혀 있으니 그대로 쓴다.

## 스냅샷의 한계

스냅샷은 "그 시점 내 로컬 DB 를 그대로 복사"한 것이라 아래는 보장하지 않는다.

1. **업로드 파일 실물** — `file_metadata` / `expo_images` / `expo_files` 가 가리키는 S3 객체는
   스냅샷을 만든 사람의 로컬 S3Mock 볼륨에만 있다. 행은 생기지만 이미지·PDF 는 깨져 보인다.
2. **QR 해시** — `issued_tickets.qr_token_hash`, `ticket_access_tokens.token_hash` 는 스냅샷을
   만든 사람의 `backend/.env` 의 `QR_TOKEN_SECRET` 으로 계산된 값이다. 본인 값이 다르면 QR
   스캔·`/tickets?token=` 화면이 이 데이터로는 검증되지 않는다. QR 을 실제로 눌러보려면
   `local_full_flow_expo7.sql` 처럼 실행 시점에 `:qr_secret` 변수로 다시 계산하는 방식이 필요하다.
3. **비밀번호** — `users.password_hash` 는 실제 bcrypt 해시고, 원문 비밀번호는 계정을 만든
   사람만 안다. 로그인해서 화면을 보려면 회원가입 API 로 계정을 새로 만드는 편이 낫다.

## 스냅샷을 새로 만들 때

다음에 또 로컬 DB 를 통째로 시드로 공유해야 하면 이 순서를 따른다.

1. `pg_dump --data-only --inserts --on-conflict-do-nothing` 으로 뽑는다.
2. 아래는 반드시 제외한다 (`--exclude-table` 옵션).
   - `flyway_schema_history` — Flyway 내부 테이블.
   - `virtual_venues`, `venue_halls`, `venue_zones` — 이미
     `V202608121600__seed_kintex_venue_structure.sql` 마이그레이션이 모든 환경에 넣는다.
     여기서 또 넣으면 중복이다.
   - `refresh_tokens`, `phone_verifications`, `password_reset_tokens`, `social_accounts` —
     세션·인증 부산물이다. `refresh_tokens` 는 `JWT_SECRET` 이 사람마다 달라도 되는 값이라
     (`env.sample` 참고) 남의 걸 넣어봐야 검증되지 않고, `phone_verifications` 는 만료되는
     1회용 인증코드라 재현할 의미가 없다.
3. `pg_dump` 가 순환 FK 를 경고하면(`users`↔`file_metadata`, `participation_applications`↔
   `booth_orders` 등) 한쪽 컬럼을 `NULL` 로 비워 먼저 넣고, 상대 테이블이 생긴 뒤 `UPDATE` 로
   채운다. 두 테이블이 서로를 참조하면 순서만으로는 절대 풀리지 않는다.
4. **실행해서 검증한다.** 스키마만 있는 빈 DB(마이그레이션만 적용, 데이터는 없는 상태)에
   실제로 돌려보고, 두 번째 실행이 전부 `INSERT 0 0` 으로 스킵되는지도 확인한다. `pg_dump`
   출력을 그대로 커밋하지 않는다 — 위 순환 FK 문제 때문에 반드시 한 번은 실패한다.
5. 파일 상단에 실행 방법과 "알려진 한계"(업로드 파일 실물 없음, QR 해시 불일치 가능성,
   비밀번호 원문 모름)를 적는다. 이 문서의 예시를 그대로 따라도 된다.
