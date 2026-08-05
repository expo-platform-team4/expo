# 개발 규칙

팀원마다 에디터(Cursor / IntelliJ / VS Code)와 AI 도구가 달라서, 저장할 때마다 서로 다른 포맷터가
발동하면 한 줄만 고쳐도 파일 전체가 diff 로 잡힌다. 그걸 막기 위한 공통 규칙이다.

핵심 원칙은 하나다.

> **포맷의 정답은 에디터가 아니라 빌드 도구가 갖는다.**

누가 어떤 에디터를 쓰든 `./gradlew spotlessApply` / `bun run format` 을 통과한 결과물은
바이트 단위로 동일하다. `.editorconfig` 는 그 전에 diff 를 줄여 주는 완충재일 뿐이다.

| 계층 | 백엔드 (Java) | 프론트 (TS/React) | 하는 일 |
| --- | --- | --- | --- |
| 에디터 | `.editorconfig` | `.editorconfig` | 저장 시 지킬 최소 규칙 (들여쓰기, LF, 파일 끝 개행) |
| 포맷터 (자동 수정 O) | **Spotless** + google-java-format | **Prettier** | 들여쓰기, 줄바꿈, 공백, import 정렬 |
| 린터 (자동 수정 X) | **Checkstyle** | **ESLint** | 네이밍, `System.out` 금지, 복잡도, 버그 패턴 |

포맷은 포맷터가, 규칙은 린터가 맡는다. 이 분리가 지켜져야 둘이 싸우지 않는다.

**Java 는 4칸 들여쓰기 + 100컬럼**이다 (`googleJavaFormat().aosp()`).
`.editorconfig` 와 Spotless 양쪽에 박혀 있어 어느 에디터에서든 동일하게 나온다.

---

## A. 최초 1회 — 전원 필수

### 1. 에디터 확장 설치

VS Code / Cursor 로 **저장소 루트**를 열면 우측 하단에 권장 확장 알림이 뜬다. 전부 설치한다.

- **EditorConfig for VS Code** — 이게 없으면 `.editorconfig` 가 통째로 무시된다. 가장 중요하다.
- Prettier, ESLint, Extension Pack for Java

IntelliJ 는 `.editorconfig` 를 기본 인식하므로 추가 설치가 필요 없다.

### 2. blame 보호 설정

저장소당 한 번만 실행하면 된다.

```bash
git config blame.ignoreRevsFile .git-blame-ignore-revs
```

전 코드베이스를 일괄 재포맷한 커밋이 하나 있다. 이 설정을 안 하면 로컬 `git blame` 이
전부 그 커밋으로 나와서 누가 무엇을 썼는지 추적할 수 없다. (GitHub 웹 blame 은 자동 적용된다)

### 3. 작업 중인 브랜치가 있다면

`dev` 를 머지한 뒤 포맷을 한 번 돌리고 푸시한다. 안 그러면 PR 에 안 건드린 파일이 잔뜩 잡힌다.

```bash
git merge origin/dev && cd backend && ./gradlew spotlessApply
```

---

## B. 무엇이 자동이고 무엇이 수동인가

|  | 백엔드 (Java) | 프론트 (TS/React) |
| --- | --- | --- |
| 저장할 때 | **아무 일도 안 일어남** (IDE Java 포맷 끔) | Prettier 가 자동 정리 |
| `./gradlew bootRun` / `build` / `test` | **Spotless 가 자동으로 고침** | — |
| 수동 실행 | `./gradlew spotlessApply` | `bun run format` |
| 검사만 | `./gradlew spotlessCheck checkstyleMain` | `bun run check` |

Java 는 IDE 저장 포맷을 **껐다**. Red Hat Java 확장의 자체 포맷터는 google-java-format 과
결과가 달라 계속 충돌하기 때문이다. 대신 `./gradlew` 명령을 한 번이라도 돌리면 그때 정리된다.
IDE 화면에서 잠깐 들여쓰기가 어긋나 보여도 정상이다.

---

## C. 개발 흐름 — 지금 vs 앞으로

**지금**

```
코드 작성 → commit → push → PR 생성 → 리뷰 → 머지
```

**앞으로**

```
코드 작성
   ↓
./gradlew bootRun 등으로 한 번 실행        ← 여기서 Java 포맷이 자동 정리됨
   ↓
git status 로 포맷 변경분 확인              ← 새로 생긴 단계
   ↓
git add . && git commit                     ← 포맷 변경분을 같이 커밋
   ↓
git push (branch publish)
   ↓
PR 생성
   ↓
CI 자동 실행: backend / frontend
   ↓
둘 다 초록이어야 머지 버튼 활성화            ← 새로 생긴 게이트
   ↓
리뷰 → 머지
```

실질적으로 바뀌는 건 두 가지다.

1. **커밋 전에 `git status` 를 한 번 볼 것.** 포맷터가 고친 파일이 섞여 있을 수 있다.
2. **PR 에서 CI 가 빨간불이면 머지가 막힌다.**

---

## D. 커밋 메시지

**제목 한 줄만 쓴다.** 본문은 가급적 쓰지 않고, 꼭 필요한 경우에만 최소한으로 적는다.
AI 도구가 자동으로 붙이는 `Co-Authored-By` 같은 트레일러는 지우고 커밋한다.

```
chore: 코드 스타일 공통 규칙 도입
feat: 로그인 API 추가
fix: JWT 만료 검증 오류 수정
```

포맷만 바꾸는 커밋은 **로직 변경과 절대 섞지 않는다.** 섞이면 리뷰가 불가능해진다.

---

## E. 커밋 전 권장 명령

```bash
cd backend && ./gradlew spotlessApply checkstyleMain
```

```bash
cd front && bun run format && bun run check
```

---

## F. CI 가 실패했을 때

| 실패한 체크 | 원인 | 대응 |
| --- | --- | --- |
| `backend` → Style, spotless | 포맷이 어긋남 | `cd backend && ./gradlew spotlessApply` → 커밋 → 푸시 |
| `backend` → Style, checkstyle | 네이밍·금지 패턴 위반 (자동 수정 불가) | 로그의 룰 이름 확인 후 직접 수정 |
| `backend` → Build | 컴파일 실패 | 로컬에서 `./gradlew build -x test` 재현 |
| `frontend` → Format check | Prettier 미적용 | `cd front && bun run format` → 커밋 → 푸시 |
| `frontend` → Lint | ESLint 규칙 위반 | `bun run lint:fix` 로 일부 자동 수정, 나머지는 직접 |
| `frontend` → Typecheck | 타입 오류 | `bun run typecheck` 로그 확인 |

**백엔드 테스트는 아직 CI 에서 돌리지 않는다.** `ExpoApplicationTests.contextLoads` 가
`V1__init_schema.sql` 의 `CREATE EXTENSION btree_gist` 를 H2 가 실행하지 못해 실패하기 때문이다
(README 의 "알려진 제약: Flyway + H2" 참고). 테스트 환경을 고친 뒤
[`ci.yml`](.github/workflows/ci.yml) 하단에 주석으로 남겨 둔 `backend-test` job 을 되살린다.
그때까지는 로컬에서 `./gradlew test` 를 직접 돌려 확인한다.

---

## G. 자주 나오는 질문

**왜 내 PR 에 안 건드린 파일이 잔뜩 잡히나?**
일괄 포맷 커밋 이전에 딴 브랜치라서 그렇다. A-3 대로 `dev` 를 머지하고 `spotlessApply` 를 돌리면 사라진다.

**포맷을 예외로 두고 싶은 블록이 있다**
Java 는 `// spotless:off` ~ `// spotless:on`, Checkstyle 은 `// CHECKSTYLE:OFF` ~ `// CHECKSTYLE:ON` 으로 감싼다.

**Javadoc 이 자동 정리가 안 되는데?**
일부러 껐다. google-java-format 은 한글을 폭 1 로 계산해서 100컬럼에 맞춰 재줄바꿈하는데,
한글은 화면상 2칸이라 실제로는 200칸짜리 줄이 된다. Javadoc 줄바꿈은 각자 읽기 좋게 손으로 맞춘다.
Checkstyle `LineLength` 도 주석 줄은 검사에서 제외한다.

**왜 2칸이 아니라 4칸인가?**
팀 합의다. google-java-format 은 들여쓰기 옵션이 없고 2칸(기본) 또는 4칸(`aosp`) 둘 중 하나만 고를 수 있다.

**`./gradlew check` 가 포맷 위반을 안 잡는다**
의도된 동작이다. 로컬에서는 `compileJava` 가 먼저 `spotlessApply` 로 고쳐 버린다.
포맷 게이트는 CI 가 담당한다 (CI 에서는 `spotlessCheck` 로 바뀌어 고치지 않고 실패시킨다).

---

## H. 규칙을 바꾸고 싶다면

설정 파일 위치는 다음과 같다. 바꾸려면 PR 로 올려 팀 합의를 거친다.

| 파일 | 내용 |
| --- | --- |
| [`.editorconfig`](.editorconfig) | 에디터 공통 규칙 (들여쓰기, 개행, 인코딩) |
| [`backend/build.gradle`](backend/build.gradle) | `spotless` / `checkstyle` 블록, 자동 실행 훅 |
| [`backend/config/checkstyle/checkstyle.xml`](backend/config/checkstyle/checkstyle.xml) | Checkstyle 룰셋 |
| [`front/.prettierrc`](front/.prettierrc) | Prettier 설정 |
| [`front/eslint.config.mjs`](front/eslint.config.mjs) | ESLint 설정 |
| [`.github/workflows/ci.yml`](.github/workflows/ci.yml) | CI 파이프라인 |

`.editorconfig` 의 `[*.java] indent_size` 와 `build.gradle` 의 `googleJavaFormat().aosp()` 는
**반드시 같이 바꿔야 한다.** 어긋나면 IDE 와 Gradle 이 서로 다른 결과를 내면서 무한 diff 가 난다.
