# expo

박람회 예약·티켓 판매 플랫폼

박람회 개최·운영, 티켓 중개 판매, 정산을 다룬다.

> 코드 스타일·커밋·PR 규칙은 [CONTRIBUTING.md](CONTRIBUTING.md) 를 먼저 읽는다.
> 저장소를 처음 클론했다면 "최초 1회 — 전원 필수" 절의 세 가지를 반드시 수행한다.

---

## 기술 스택

### 언어 · 빌드

| 구분 | 기술 | 버전 |
|-|-|-|
| 백엔드 언어 / 런타임 | Java (Gradle toolchain) | 21 (LTS) |
| 백엔드 빌드 도구 | Gradle Wrapper | 9.5.1 |
| 프론트 언어 | TypeScript | 5.9.3 |
| 프론트 런타임 | Node.js | **24 LTS** (아래 주의 참고) |
| 프론트 빌드 도구 | Next.js / Turbopack | 16.2.12 |
| 프론트 패키지 매니저 | Bun | 1.3.11 |

> **Node 버전 주의 — 24 를 쓴다**
> Node **26 에서는 Next 16 의 `rewrites()` 가 전부 500 으로 깨진다.** 내부·외부 rewrite 구분 없이,
> dev·production 구분 없이 `ERR_INVALID_ARG_TYPE: The "path" argument must be of type string. Received null`
> 이 뜬다. 같은 Next 16.2.12 를 Node 24 에서 돌리면 정상 동작한다(검증 완료).
> `/api` 프록시가 rewrites 로 동작하므로 Node 26 을 쓰면 백엔드 호출이 전부 실패한다.
> `front/.nvmrc` 에 `24` 를 박아 두었다.

### Backend

| 구분 | 기술 | 버전 |
|-|-|-|
| 애플리케이션 프레임워크 | Spring Boot | 4.1.0 |
| 코어 프레임워크 | Spring Framework | 7.0.8 |
| 웹 계층 | Spring MVC (`spring-boot-starter-webmvc`) | Boot 관리 |
| 인증 / 인가 | Spring Security | 7.1.0 |
| 소셜 로그인 | OAuth2 Client (Google, Kakao) | Boot 관리 |
| 토큰 | JJWT (Access / Refresh) | 0.13.0 |
| ORM | Hibernate ORM (JPA) | 7.4.1.Final |
| SQL 매퍼 | MyBatis Spring Boot Starter | 4.0.1 (MyBatis 3.5.19) |
| DB 마이그레이션 | Flyway | 12.4.0 |
| Database | PostgreSQL (JDBC 드라이버 42.7.11) | 18 |
| 테스트 DB | H2 (PostgreSQL 호환 모드) | 2.4.240 |
| 테스트 프레임워크 | JUnit Jupiter / Mockito | 6.0.3 / 5.23.0 |
| 직렬화 | Jackson (`tools.jackson`) | 3.1.4 |
| 로깅 | Logback | 1.5.34 |
| 실시간 통신 | Spring WebSocket / SSE | Boot 관리 |
| 모니터링 | Spring Boot Actuator | Boot 관리 |
| API 문서화 | springdoc-openapi (Swagger UI) | 3.1.0 |
| Object Storage | Spring Cloud AWS S3 | 4.1.0 (AWS SDK 2.47.4) |
| 결제 | Toss Payments API | v1 |
| 환경변수 관리 | `me.paulschwarz:springboot4-dotenv` | 5.1.0 |
| 코드 포맷 | Spotless (google-java-format `aosp`, 4칸/100컬럼) | 8.9.0 (GJF 1.28.0) |
| 정적 분석 | Checkstyle | 13.9.0 |
| 개발 편의 | Lombok / Spring DevTools | 1.18.46 / Boot 관리 |

> **Jackson 3 주의**
> Spring Boot 4 는 Jackson 3(`tools.jackson`)을 쓴다. Jackson 2 시절의
> `spring.jackson.serialization.write-dates-as-timestamps` 같은 설정은 상수가
> `SerializationFeature` → `DateTimeFeature` 로 옮겨가 더 이상 바인딩되지 않는다.
> 검색해서 나오는 Boot 3 예제를 그대로 붙여넣으면 기동이 깨진다.

> **Boot 4 자동설정 모듈화 주의**
> Boot 4 는 기술별 자동설정이 별도 모듈로 쪼개졌다. 예를 들어 `org.flywaydb:flyway-core` 만 넣으면
> 의존성은 해석되지만 자동설정이 없어 **마이그레이션이 아무 에러 없이 조용히 실행되지 않는다.**
> 반드시 `spring-boot-starter-flyway` 같은 starter 를 쓴다.
> 또한 `spring-boot-starter-web` 은 `spring-boot-starter-webmvc` 로 이름이 바뀌었다.

### Frontend

| 구분 | 기술 | 버전 |
|-|-|-|
| 프레임워크 | Next.js (App Router) | 16.2.12 |
| UI 라이브러리 | React | 19.2.4 |
| 서버 상태 / 캐싱 | TanStack Query | 5.101.4 |
| HTTP 클라이언트 | Axios | 1.19.0 |
| 클라이언트 상태 | Zustand | 5.0.14 |
| 폼 | React Hook Form | 7.84.0 |
| 스키마 검증 | Zod (+ `@hookform/resolvers`) | 4.4.3 (5.7.1) |
| CSS | Tailwind CSS (`@tailwindcss/postcss`) | 4.3.3 |
| 결제 SDK | `@tosspayments/tosspayments-sdk` | 2.7.1 |
| 린트 | ESLint (+ `eslint-config-next`) | 9.39.5 (16.2.12) |
| 포맷 | Prettier (+ `prettier-plugin-tailwindcss`) | 3.9.6 (0.8.1) |

라우팅은 **App Router** 가 담당한다. `react-router` 는 쓰지 않는다.

> **Tailwind v4 주의**
> v4 는 `tailwind.config.js` 를 쓰지 않는다. 설정은 `src/app/globals.css` 의 `@theme` 블록에서 한다.
> content 경로 지정도 필요 없다(자동 탐지). v3 튜토리얼을 그대로 따라가면 안 된다.

### 인프라 · 협업

| 구분 | 기술 | 버전 | 상태 |
|-|-|-|-|
| 컨테이너 | Docker / Compose | 29.5.2 | 사용 중 (`docker-compose.yml`) |
| 로컬 Object Storage | Adobe S3Mock | 5.1.0 | 사용 중 |
| Reverse Proxy | Caddy | 2.x | 예정 |
| 배포 인프라 | OCI (ARM, Oracle Linux) | — | 예정 |
| CI/CD 파이프라인 | GitHub Actions | — | 사용 중 (`.github/workflows/ci.yml`) |
| 컨테이너 레지스트리 | GHCR | — | **미구성** |
| 코드 리뷰 자동화 | CodeRabbit | — | 사용 중 (`.coderabbit.yaml`) |

배포 시 Next 는 `output: 'standalone'` 빌드를 Docker 컨테이너로 띄우고,
Caddy 가 `/` → Next, `/api` → Spring 으로 분기한다.

---

## 프로젝트 구조

```
expo/
├── docker-compose.yml                      로컬 인프라 (PostgreSQL + S3Mock)
├── CONTRIBUTING.md                         코드 스타일 · 커밋 · PR 규칙
├── .editorconfig                           에디터 공통 규칙 (Spotless/Prettier 와 일치시킬 것)
├── .git-blame-ignore-revs                  일괄 포맷 커밋을 blame 에서 제외
├── .github/workflows/ci.yml                PR 스타일·빌드 검사
├── backend/                                Spring Boot
│   ├── config/checkstyle/                  Checkstyle 룰셋
│   ├── env.sample                          환경변수 템플릿 (.env 는 커밋 금지)
│   └── src/main/
│       ├── java/com/expo/                  ← 도메인 패키지 (아래 규칙 참고)
│       └── resources/
│           ├── application.yml             공통
│           ├── application-local.yml       로컬 (PostgreSQL)
│           ├── application-test.yml        테스트 (H2)
│           ├── application-prod.yml        운영
│           ├── application-oauth2.yml      소셜 로그인 (키 발급 후 활성화)
│           ├── db/migration/               Flyway 마이그레이션
│           └── mapper/<domain>/            MyBatis XML
└── front/                                  Next.js
    ├── .nvmrc                              Node 24 고정
    ├── next.config.ts                      standalone + /api rewrites
    ├── env.sample
    └── src/
        ├── app/
        │   ├── layout.tsx
        │   ├── providers.tsx               QueryClientProvider
        │   └── globals.css                 Tailwind v4 @theme
        └── lib/api.ts                      axios 인스턴스
```

---

## 백엔드 패키지 규칙

루트 패키지는 `com.expo` 다. 진입점 `ExpoApplication` 이 이 패키지에 있으므로
컴포넌트 스캔과 MyBatis 매퍼 스캔이 하위 도메인 전체를 자동으로 훑는다. 도메인을 추가해도 스캔 설정은 손대지 않는다.

### 집합체 도메인 (17개)

```
auth  member  expo  banner  ticket  order  payment  refund  checkin
notification  settlement  client  admin  recruitment  participation  venue  booth
```

각 도메인은 **예외 없이** 아래 8개 하위 패키지를 갖는다.

| 하위 패키지 | 무엇을 두는가 |
|-|-|
| `controller` | HTTP 엔드포인트. 요청 검증과 DTO 변환까지만 하고 비즈니스 로직은 `service` 에 위임한다 |
| `converter` | Entity ↔ DTO 변환. 변환 코드가 `service` 로 새어 나가지 않게 여기에 모은다 |
| `dto` | 요청·응답 DTO. `record` 를 기본으로 쓴다. 엔티티를 그대로 노출하지 않는다 |
| `entity` | JPA 엔티티와 도메인 규칙. 스키마는 Flyway 가 만들고 여기서는 매핑만 한다 |
| `event` | 도메인 이벤트와 리스너. 도메인 간 직접 호출을 줄이는 데 쓴다 |
| `exception` | 그 도메인 전용 예외. 공통 예외는 `common.exception` 을 쓴다 |
| `repository` | `JpaRepository` 와 MyBatis `@Mapper` 인터페이스 |
| `service` | 트랜잭션 경계와 비즈니스 로직 |

### common (집합체 아님)

여러 도메인이 공유하는 코드라 표준 8개 구조를 따르지 않는다.

| 하위 패키지 | 무엇을 두는가 |
|-|-|
| `annotation` | 커스텀 어노테이션 (`@LoginMember` 등) |
| `config` | `SecurityConfig`, `RestClient`(토스 호출), S3, Swagger, CORS |
| `exception` | 여러 도메인이 공유할 예외·에러코드·핸들러 자리. **현재는 비어 있다** — 아래 참고 |
| `response` | `ApiResponse`, `PageResponse` 등 공통 응답 포맷 |
| `util` | 특정 도메인에 속하지 않는 순수 유틸리티 |

> `BusinessException`, `ErrorCode`, `AuthExceptionHandler` 는 지금 `auth/exception` 에 있다.
> 아직 auth 하나만 쓰고 있어서다. **두 번째 도메인이 같은 예외를 쓰게 되면 그때
> `common/exception` 으로 옮기고 핸들러를 `GlobalExceptionHandler` 로 승격한다.**

### jwt (집합체 아님)

JWT 발급·검증과 인증 필터. 여러 도메인이 쓰는 인증 인프라라 8개 구조를 따르지 않는다.
`JwtTokenProvider`, `JwtAuthenticationFilter`, `JwtProperties`, `AuthPrincipal` 이 여기 있다.

`auth` 는 회원가입·로그인이라는 **업무**를 다루고, `jwt` 는 토큰이라는 **기술**을 다룬다.
토큰 자체를 손볼 일이면 `jwt`, 가입·인증 흐름을 손볼 일이면 `auth` 다.

모든 패키지에 `package-info.java` 가 들어 있고 그 자리에서 위 규칙을 다시 확인할 수 있다.
클래스를 추가하면 `package-info.java` 는 그대로 두면 된다(지우지 않는다).

---

## 시작하기

### 1. 사전 준비

- JDK 21 (Gradle toolchain 이 자동으로 받아오지만, 미리 있으면 첫 빌드가 빠르다)
- **Node 24 LTS** — Node 26 은 안 된다. 위 "Node 버전 주의" 참고
- Docker
- Bun 1.3+

### 2. 로컬 인프라 기동

저장소 루트에서 실행한다. PostgreSQL 과 S3Mock 이 함께 뜬다.

```bash
docker compose up -d
```

준비될 때까지 기다렸다가 상태를 확인한다. 둘 다 `healthy` 가 떠야 한다.

```bash
docker compose ps
```

| 서비스 | 주소 | 비고 |
|-|-|-|
| PostgreSQL | `localhost:5432` | 계정 `expo / expo_local_pw`, DB `expo` |
| S3Mock | `http://localhost:9090` | 버킷 `expo-local` 자동 생성 |

비밀번호를 바꾸려면 루트에 `.env` 를 만들어 `POSTGRES_PASSWORD` 를 지정하고 `backend/.env` 도 같은 값으로 맞춘다.

> 포트가 이미 쓰이고 있으면 `docker-compose.yml` 의 `ports` 왼쪽 숫자와
> `backend/.env` 의 `POSTGRES_PORT` / `AWS_S3_ENDPOINT` 를 같이 바꾼다.

### 3. 환경변수 설정

```bash
cp backend/env.sample backend/.env
```

`backend/.env` 를 열어 최소 두 개는 반드시 채운다.

- `POSTGRES_PASSWORD` — 위 compose 기본값을 쓴다면 `expo_local_pw`
- `JWT_SECRET` — `openssl rand -base64 48` 로 생성

프론트도 동일하게 한다.

```bash
cp front/env.sample front/.env.local
```

`.env*` 는 `.gitignore` 에 등록되어 있다. **절대 커밋하지 않는다.**

### 4. 실행

```bash
cd backend && ./gradlew bootRun --args='--spring.profiles.active=local'
```

```bash
cd front && bun install && bun run dev
```

- Backend: http://localhost:8080
- Frontend: http://localhost:3000
- Swagger UI: http://localhost:8080/swagger-ui.html
- S3Mock: http://localhost:9090

브라우저의 `/api` 요청은 `next.config.ts` 의 rewrites 가 8080 으로 넘긴다.
브라우저 기준 same-origin 이라 CORS 설정이 필요 없다.

### 자주 쓰는 명령

```bash
cd backend && ./gradlew spotlessApply
```

```bash
cd backend && ./gradlew build
```

```bash
cd front && bun run format && bun run check
```

`bun run check` 는 `format:check` + `lint` + `typecheck` 를 한 번에 돌린다. CI 와 같은 조합이다.

---

## Object Storage 동작 방식

박람회·부스 이미지는 S3 에 올린다. 로컬에서는 **Adobe S3Mock** 이 S3 인 척한다.

### 에뮬레이터는 라이브러리가 아니다

백엔드에 들어 있는 건 `io.awspring.cloud:spring-cloud-aws-starter-s3` 하나뿐이고,
이건 **AWS SDK 와 Spring 을 이어주는 코드**일 뿐이다. S3Mock 과는 아무 관계가 없다.
S3Mock 은 `docker-compose.yml` 에 별도로 떠 있는 **서버**다.

### 업로드한 파일은 어디에 있나

```
[ 호스트: ./gradlew bootRun ]
  │  POST /expos/1/images  (multipart)
  ▼
Controller → S3Template.upload("expo-local", "expo/1/hero.png", stream)
  │
  │  AWS SDK 가 S3 PutObject "HTTP 요청" 을 만든다
  │  대상: spring.cloud.aws.s3.endpoint = http://localhost:9090
  ▼
──── HTTP ────▶ [ Docker: S3Mock 컨테이너 ]
                      └ 컨테이너 안 /s3data 에 저장 (볼륨 expo-s3mock-data)
```

**호스트 파일시스템에는 저장되지 않는다.** 프로젝트 폴더를 뒤져도 없다.
파일은 HTTP 로 컨테이너에 넘어가 볼륨에 쌓인다. 확인하려면 S3 에 물어봐야 한다.

```bash
AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test AWS_DEFAULT_REGION=ap-northeast-2 \
aws --endpoint-url http://localhost:9090 s3 ls s3://expo-local --recursive
```

애플리케이션 코드는 로컬이든 운영이든 똑같다. `endpoint` 값만 다르다.
운영에서는 이 설정을 지우면 진짜 AWS S3 로 붙는다. 그게 에뮬레이터를 쓰는 이유다.

### 팀 개발 시 알아둘 것

**데이터는 개발자별로 격리된다.** 각자 자기 컨테이너를 돌리므로 A 가 올린 이미지는 B 에게 없다.
이건 정상이고 의도한 것이다.

**버킷은 자동으로 만들어진다.** `docker compose up -d` 하면 `expo-local` 이 이미 있다.
따로 만들 필요 없고, 없으면 첫 업로드에서 `NoSuchBucket` 이 난다.

**데이터는 재기동해도 남는다.** `restart` 든 `down` 이든 유지되고, `down -v` 로만 지워진다.

**DB 와 S3 의 수명을 맞춰라.** 둘은 별도 볼륨이다. 한쪽만 지우면 DB 에는 이미지 키가 남았는데
실제 객체는 없는 상태가 되어 화면에서 이미지가 깨진다. 원인을 찾기 까다로우니
초기화는 `docker compose down -v` 로 **양쪽을 같이** 한다.

### 알려진 제약: presigned URL 이 검증되지 않는다

S3Mock 은 presigned URL 을 받아주기만 하고 **서명·만료시간·HTTP 메서드를 검증하지 않는다.**
즉 만료 계산이 틀렸거나 서명 로직에 버그가 있어도 **로컬에서는 멀쩡히 동작하고 운영에서 터진다.**
presigned URL 관련 코드는 로컬 테스트를 믿지 말고 코드 리뷰로 잡아야 한다.

---

## 프론트 환경변수 — 두 개로 나뉜 이유

**서버 컴포넌트는 `/api/...` 같은 상대 경로로 fetch 할 수 없다.** 브라우저처럼 "현재 오리진" 개념이
없어서 절대 URL 이 필요하다. 그래서 브라우저용과 서버용을 따로 둔다.

| 변수 | 노출 | 값 | 쓰는 곳 |
|-|-|-|-|
| `NEXT_PUBLIC_API_BASE_URL` | 브라우저 | `/api` | 클라이언트 컴포넌트. rewrites 가 백엔드로 넘긴다 |
| `BACKEND_ORIGIN` | 서버 전용 | `http://localhost:8080` | 서버 컴포넌트 fetch, `next.config.ts` 의 rewrites 대상 |
| `NEXT_PUBLIC_TOSS_CLIENT_KEY` | 브라우저 | `test_ck_...` | 결제창 |

`src/lib/api.ts` 가 실행 위치를 보고 baseURL 을 알아서 고른다.

`NEXT_PUBLIC_` 이 붙은 값은 빌드 산출물에 그대로 박혀 브라우저에서 전부 보인다.
**시크릿 키(`test_sk_`)는 절대 프론트 `.env` 에 넣지 않는다.**

Docker 로 띄울 때는 `localhost` 가 Next 컨테이너 자기 자신이므로
`BACKEND_ORIGIN` 을 서비스 이름으로 바꾼다 (예: `http://backend:8080`).

---

## API 키 발급 현황

### Toss Payments — 키 발급 없이 개발 가능

토스페이먼츠는 [개발자센터 문서](https://docs.tosspayments.com/reference/using-api/api-keys)에 공개 테스트 키를 제공한다.
가입이나 사업자번호 없이 결제 연동을 테스트할 수 있다. 다만 결제 내역 조회는 되지 않는다.

문서의 `test_ck_...` / `test_sk_...` 를 복사해 `.env` 에 넣는다.
`test_sk_`(시크릿 키)는 **서버 전용**이다. 프론트에는 절대 넣지 않는다.

### OAuth2 (Google / Kakao) — 미발급

**키가 없어도 앱은 정상 기동한다.** 소셜 로그인 엔드포인트만 없는 상태가 된다.

소셜 로그인 설정은 `application-oauth2.yml` 로 분리되어 있고, `oauth2` 프로필이 켜졌을 때만 로드된다.

| 상황 | 실행 프로필 | 결과 |
|-|-|-|
| 키 발급 전 (현재) | `local` | 정상 기동. `/oauth2/authorization/*` 없음 |
| 키 발급 후 | `local,oauth2` | Google / Kakao 로그인 동작 |

왜 이렇게 나눴나 — Spring Security 는 `spring.security.oauth2.client.registration.*` 키가 존재하기만 하면 자동설정을 켜고, 이때 `client-id` 가 빈 문자열이면 다음과 같이 기동 자체가 실패한다.

```
java.lang.IllegalStateException: Client id of registration 'google' must not be empty.
    at ...OAuth2ClientProperties.validateRegistration(OAuth2ClientProperties.java:71)
```

즉 "키를 못 받았으니 빈 값으로 두자"가 통하지 않는다. 그래서 등록 정보 전체를 별도 프로필로 뺐다.
같은 이유로 `client-id` 만 채우고 `client-secret` 을 비워도 실패한다. 항상 쌍으로 관리한다.

발급처와 리디렉션 URI 는 `backend/env.sample` 주석에 정리해 두었다.

---

## 데이터베이스 규칙

모든 프로필에서 `spring.jpa.hibernate.ddl-auto: validate` 다. **JPA 가 테이블을 만들지 않는다.**
스키마는 Flyway 마이그레이션이 전담하고, JPA 는 엔티티와 실제 스키마가 맞는지 검증만 한다.

엔티티를 추가·수정하면 **반드시** 대응하는 마이그레이션 파일을 같이 커밋한다. 안 하면 기동 시점에 검증 실패한다.
작성 규칙은 [`backend/src/main/resources/db/migration/README.md`](backend/src/main/resources/db/migration/README.md) 참고.

### 알려진 제약: Flyway + H2

local(PostgreSQL)과 test(H2)가 **같은 마이그레이션 SQL** 을 실행한다.
H2 는 `MODE=PostgreSQL` 로 상당 부분을 흡수하지만 JSONB, 배열 타입, `ON CONFLICT` 세부 문법까지는 따라오지 못한다.

마이그레이션 SQL 은 가급적 표준 문법으로 쓴다. PostgreSQL 전용 기능이 꼭 필요해지면
`db/migration`(공통) + `db/migration-pg`(전용)로 `spring.flyway.locations` 를 프로필별로 나누고,
그마저 감당이 안 되면 테스트를 Testcontainers PostgreSQL 로 전환한다.
