# AGENTS.md

AI 코딩 도구가 이 저장소에서 작업하기 전에 읽는 규칙이다.
Claude Code · Cursor · Copilot · Codex 어느 것이든 이 파일이 정본이다.

## 이 파일의 역할

**여기는 지식 저장소가 아니라 라우팅 테이블이다.** 이 저장소의 문서는 5,000줄이 넘는다.
전부 읽히려 하지 말고, 지금 하는 작업에 필요한 문서로 보내는 것이 이 파일의 일이다.

담는 것은 셋뿐이다.

1. **어디를 읽어야 하는가** (라우팅)
2. **무엇을 하면 안 되는가** (되돌리기 어렵거나 조용히 깨지는 것)
3. **이 프로젝트에서만 참인 사실** (모델이 반드시 틀리는 것)

담지 않는 것: 디렉터리 구조, **스택 목록·버전 표**, 들여쓰기·포맷 규칙, 일반적인 좋은 코드 조언.
구조와 버전의 정본은 코드(`build.gradle`, `package.json`)다. 여기 복제하면 반드시 어긋난다.
포맷은 Spotless·Prettier·CI 가 기계적으로 강제한다.
**부탁으로 지킬 수 있는 것은 여기 쓰지 않는다. 게이트로 만든다.**

**단, 버전 때문에 코드가 달라지는 제약은 3절에 쓴다.** "Boot 4 라 `-webmvc` 다",
"Node 26 에서는 rewrites 가 깨진다" 처럼 틀리면 동작하지 않는 사실이 그렇다.
금지하는 것은 `build.gradle` 을 베껴 적은 표이지, 버전을 언급하는 것 자체가 아니다.

---

## 1. 시작하기 전에 — 무엇을 읽을 것인가

| 지금 하려는 일 | 먼저 읽는다 |
| --- | --- |
| DB 스키마 변경, 마이그레이션 추가 | [`backend/src/main/resources/db/migration/README.md`](backend/src/main/resources/db/migration/README.md) |
| 테이블·컬럼의 의미와 근거 | [`docs/init_table_schema.md`](docs/init_table_schema.md), [`docs/erd/`](docs/erd/) — **해당 절만** |
| 로그를 남길 때 | [`docs/logging.md`](docs/logging.md) |
| 백엔드 클래스를 어디 둘지 | 대상 패키지의 `package-info.java` |
| 파일 업로드 · S3 | [`docs/s3-presigned-url.md`](docs/s3-presigned-url.md) |
| 커밋 메시지, PR, 스타일 | [`CONTRIBUTING.md`](CONTRIBUTING.md) |
| 백엔드 상세 규칙 | [`backend/AGENTS.md`](backend/AGENTS.md) |
| 프론트 상세 규칙 | [`front/AGENTS.md`](front/AGENTS.md) |

`docs/init_table_schema.md` 는 2,000줄이 넘는다. **통째로 읽지 말고 해당 테이블 절만 찾아 읽는다.**

---

## 2. 절대 하지 않는 것

되돌리기 어렵거나, 에러 없이 조용히 깨지는 것들이다.

### 데이터베이스

- **머지된 `V*.sql` 을 수정하지 않는다.**
  다른 사람 DB 에는 이미 적용된 상태라 체크섬이 깨지고, 그 사람의 애플리케이션이 기동조차 되지 않는다.
  바꿔야 하면 항상 새 버전 파일을 추가한다.
  (예외: `R__*.sql` 반복 마이그레이션은 직접 고쳐도 된다. 체크섬이 바뀌면 Flyway 가 재적용한다)
- **엔티티만 추가하고 마이그레이션을 빠뜨리지 않는다.**
  모든 프로필이 `ddl-auto: validate` 다. JPA 가 테이블을 만들어 주지 않으므로 기동 시점에 실패한다.
- **새 마이그레이션 파일명은 `V{연월일시분}__{설명}.sql` 이다.** 예: `V202608071501__add_sms_channel.sql`
  `V1`·`V2` 손번호는 쓰지 않는다 (2026-08-07 변경). `./gradlew check` 가 막는다.

### 비밀값

- **`.env` 를 커밋하지 않는다.** `env.sample` 만 커밋한다.
- **`application*.yml` 에 시크릿을 리터럴로 적지 않는다.** `${VAR}` 참조만 쓴다.

### 로그

- **개인정보·토큰·비밀번호를 로그에 남기지 않는다.**
  로그는 수집기로 흘러가면 회수할 수 없다. `DEBUG` 라도 안 된다.
- **되돌아봐야 하는 이력을 로그에만 남기지 않는다.**
  로그는 보관 기간이 지나면 사라진다. 감사 테이블(`*_histories`, `before_data`/`after_data`)이
  이미 설계돼 있으니 거기 남긴다. 결제 승인·정산 확정처럼 돈이나 권한이 걸린 변경이 여기 해당한다.

### API

- **Entity 를 컨트롤러 응답으로 반환하지 않는다.** 해당 도메인 `dto` 의 record 를 쓴다.
  엔티티를 그대로 내보내면 연관 관계를 타고 개인정보가 새거나 직렬화가 깨진다.

### Git

- **`dev` 에 직접 푸시하지 않는다.** 항상 브랜치를 만들고 PR 로 올린다.
- **커밋 메시지는 제목 한 줄만 쓴다.** 본문과 `Co-Authored-By` 같은 트레일러를 넣지 않는다.

---

## 3. 이 프로젝트에서만 참인 사실

검색해서 나오는 일반적인 예제가 여기서는 통하지 않는 지점들이다.
**버전 자체가 아니라, 그 버전 때문에 코드가 달라지는 지점만 쓴다.**
버전 목록이 필요하면 `build.gradle` 과 `package.json` 을 본다.

- **Spring Boot 4 다.** Boot 3 관습이 그대로 통하지 않는다. 의존성 이름과 자동설정이 다르다.
  백엔드를 건드린다면 [`backend/AGENTS.md`](backend/AGENTS.md) 를 먼저 읽는다.
- **Node 는 24 로 고정이다.** Node 26 에서는 Next 16 의 `rewrites()` 가 전부 500 으로 깨진다.
  `/api` 프록시가 rewrites 라 백엔드 호출이 전부 실패한다.
- **스키마는 PostgreSQL 전용이다.** JSONB · `EXCLUDE USING gist` · 부분 인덱스를 쓴다.
  H2 로는 돌지 않아 **테스트도 Testcontainers 로 실제 PostgreSQL 을 띄운다.** 그래서
  `./gradlew test` 에는 **Docker 데몬이 필요하다**(스프링 컨텍스트 테스트만 해당).

---

## 4. 다 됐다고 말하기 전에

CI 가 검사하는 것과 같은 조합이다. 통과하지 못하면 PR 머지가 막힌다.

```bash
cd backend && ./gradlew build -x test
```

```bash
cd front && bun run format && bun run check && bun run build
```

**자동으로 정리되는 건 백엔드 Java 뿐이다.** 로컬에서 `./gradlew` 를 돌리면 `compileJava` 가
Spotless 를 적용한다.

**프론트는 자동이 아니다.** `front/` 안의 `.ts` · `.tsx` · `.css` 와 **`.md` 까지** Prettier 대상이라
`bun run format` 을 직접 돌려야 한다. `bun run check` 는 검사만 하고 고치지 않는다.
`front/AGENTS.md` 도 여기 포함된다 — 실제로 이 파일 때문에 CI 가 깨진 적이 있다.

`front/` 밖의 마크다운(루트 `AGENTS.md`, `README.md`, `docs/*.md`)은 **어떤 포매터도 검사하지 않는다.**
손으로 맞춘다.

---

## 5. 이 파일을 고칠 때

- **AI 가 같은 실수를 두 번 했을 때** 한 줄 추가한다. 한 번은 우연이다. 예방적으로 쓰지 않는다.
- **Checkstyle·ESLint·CI 로 옮길 수 있게 되면 여기서 삭제한다.** 문서는 자라기만 하면 죽는다.
- 다른 변경과 섞지 않고 별도 커밋으로 올린다.

새 줄을 넣기 전에 세 가지를 확인한다.

1. **반증 가능한가** — "좋은 설계를 하라"(X) / "마이그레이션 파일을 수정하지 마라"(O)
2. **이 프로젝트에서만 참인가** — 일반 상식은 모델이 이미 안다
3. **실제로 틀린 적이 있는가**
