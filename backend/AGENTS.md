# backend/AGENTS.md

백엔드 작업 규칙. 공통 규칙과 금지 사항은 [루트 AGENTS.md](../AGENTS.md) 를 먼저 읽는다.

## Spring Boot 4 — 검색 결과가 통하지 않는 지점

학습 데이터와 인터넷 예제 대부분이 Boot 3 다. 아래 넷은 그대로 붙여넣으면 깨진다.

| 하려는 것 | Boot 3 (틀림) | 여기서 (맞음) |
| --- | --- | --- |
| 웹 스타터 | `spring-boot-starter-web` | **`spring-boot-starter-webmvc`** |
| Flyway | `org.flywaydb:flyway-core` | **`spring-boot-starter-flyway`** |
| JSON | Jackson 2 (`com.fasterxml`) | **Jackson 3 (`tools.jackson`)** |
| JPA·검증 어노테이션 | `javax.*` | **`jakarta.*`** |

**Flyway 가 특히 위험하다.** Boot 4 는 자동설정이 기술별 모듈로 쪼개졌다. `flyway-core` 만 넣으면
의존성은 해석되지만 자동설정이 없어 **마이그레이션이 아무 에러 없이 조용히 실행되지 않는다.**
성공한 것처럼 보이는데 테이블이 없다.

Jackson 3 는 `spring.jackson.serialization.write-dates-as-timestamps` 같은 설정이 더 이상
바인딩되지 않는다. 상수가 `SerializationFeature` 에서 `DateTimeFeature` 로 옮겨갔다.
애초에 Jackson 3 는 날짜를 기본으로 ISO-8601 로 직렬화하므로 설정 자체가 필요 없다.

`javax.persistence` 와 `javax.validation` 은 Checkstyle 이 막는다.

## 클래스를 어디에 둘 것인가

**규칙을 여기 옮겨 적지 않는다. 대상 패키지의 `package-info.java` 를 읽는다.**
모든 패키지에 하나씩 들어 있고 그 자리에서 무엇을 두는 곳인지 알 수 있다.

구조만 요약하면, 루트 패키지는 `com.expo` 이고 그 아래가 도메인이다.
각 도메인은 정해진 하위 패키지 구조를 따른다 — 자세한 내용은 [README](../README.md) 의
"백엔드 패키지 규칙" 절에 있다.

**집합체가 아닌 패키지가 둘 있다.**

- `com.expo.common` — 여러 도메인이 공유하는 코드. MDC 로깅 필터는 `common.logging` 에 있다
- `com.expo.jwt` — JWT 발급·검증과 인증 필터

`auth` 는 회원가입·로그인이라는 **업무**를, `jwt` 는 토큰이라는 **기술**을 다룬다.
토큰 자체를 손볼 일이면 `jwt`, 가입·인증 흐름이면 `auth` 다.

새 클래스를 만들 때 `package-info.java` 는 그대로 둔다(지우지 않는다).

## 데이터베이스

모든 프로필이 `spring.jpa.hibernate.ddl-auto: validate` 다. **JPA 가 테이블을 만들지 않는다.**
엔티티를 추가·수정하면 반드시 대응하는 마이그레이션을 같이 커밋한다.

마이그레이션 작성 규칙(버전 번호 충돌, 수정 금지, 뷰 관리)은
[`src/main/resources/db/migration/README.md`](src/main/resources/db/migration/README.md) 에 있다.
**마이그레이션을 건드리기 전에 반드시 읽는다.**

## 로깅

- 로거는 `@Slf4j` 로만 만든다. `LoggerFactory.getLogger` 직접 호출은 Checkstyle 이 막는다
- 메시지는 `{}` 플레이스홀더를 쓴다. 문자열 연결도 Checkstyle 이 막는다
- 예외는 두 번째 인자로 넘긴다 — `log.error("결제 실패 orderId={}", orderId, e)`
- 엔티티를 통째로 로그에 넣지 않는다. 식별자만 찍는다

레벨 기준, 마스킹 대상, MDC 규칙, 로그와 감사의 구분은 [`docs/logging.md`](../docs/logging.md) 에 있다.

## 다 됐다고 말하기 전에

```bash
cd backend && ./gradlew build -x test
```

- **포맷은 신경 쓰지 않는다.** 로컬에서 `./gradlew` 를 돌리면 `compileJava` 가 Spotless 를 자동 적용한다
- **Checkstyle 은 `maxWarnings = 0` 이다.** 경고 하나만 나와도 빌드가 실패한다
- **테스트는 CI 에서 돈다** (`backend-test` job). 로컬에서도 `./gradlew test` 로 전부 돌아간다
- `ExpoApplicationTests` 는 **Testcontainers 로 실제 PostgreSQL 을 띄운다.** 마이그레이션을 처음부터
  적용하고 `ddl-auto: validate` 로 엔티티와 대조하므로, **스키마와 엔티티가 어긋나면 여기서 깨진다.**
  그래서 Docker 데몬이 필요하다 — 순수 단위 테스트는 Docker 없이도 돈다
