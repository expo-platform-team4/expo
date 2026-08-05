# 로깅 규칙

이 프로젝트는 **컨테이너로 배포**되고(OCI, Docker), 스키마에 **감사 테이블이 따로 있다**.
이 두 가지가 아래 규칙 대부분의 근거다.

핵심 세 줄만 먼저 옮기면 이렇다.

1. 로그에 **개인정보·토큰·비밀번호를 남기지 않는다.** 수집기로 흘러간 로그는 회수할 수 없다.
2. 되돌아봐야 하는 이력은 로그가 아니라 **감사 테이블**에 남긴다. 로그는 순환 삭제된다.
3. 로거는 `@Slf4j`, 메시지는 `{}` 플레이스홀더.

---

## 1. 어디로 나가는가 — stdout 하나뿐

**파일로 쓰지 않는다.** `logging.file.name`, `FileAppender`, 롤링 정책을 설정하지 않는다.

컨테이너 배포라 로그 수집은 플랫폼이 한다. 컨테이너 안에 파일을 쌓으면 재시작할 때 사라지고,
볼륨을 붙이면 이번엔 디스크가 찬다. 표준 출력으로만 내보내고 수집·보관은 인프라에 맡긴다.

## 2. 어떤 형식인가 — 프로필로 갈린다

Spring Boot 4 는 **구조화 로깅이 내장**돼 있다. `logstash-logback-encoder` 같은 의존성이나
`logback-spring.xml` 을 만들지 않는다. 설정 파일이 하나 줄면 어긋날 자리도 하나 준다.

| 프로필 | 형식 | 설정 |
| --- | --- | --- |
| local / test | 사람이 읽는 한 줄 텍스트 | `logging.pattern.console` |
| prod | ECS(Elastic Common Schema) JSON 한 줄 | `logging.structured.format.console: ecs` |

prod 에서 JSON 을 쓰는 이유는 수집기가 필드로 파싱해야 검색이 되기 때문이다.
`traceId` 로 요청 하나를 추적하려면 그게 문자열 속이 아니라 필드여야 한다.

## 3. 레벨 기준

**이 표가 이 문서의 핵심이다.** 레벨 선택이 흔들리면 운영에서 로그를 못 믿게 된다.

| 레벨 | 기준 | 예 |
| --- | --- | --- |
| `ERROR` | 사람이 **즉시** 봐야 한다. 알림 대상 | 결제 승인 실패, 정산 배치 중단, 외부 API 연속 실패 |
| `WARN` | 비정상이지만 **자동으로 복구됐다** | 재시도 후 성공, 만료 토큰으로 들어온 요청 |
| `INFO` | 비즈니스 경계 이벤트. **요청마다 찍지 않는다** | 주문 생성, 결제 확정, 정산 마감 |
| `DEBUG` | 로컬 전용. prod 는 `INFO` 고정이라 나가지 않는다 | 외부 API 요청 본문, 분기 추적 |

판단이 안 서면 이렇게 묻는다.

- **새벽 3시에 이것 때문에 깨워도 되나?** → 그렇다면 `ERROR`
- **사용자 입력이 틀린 것뿐인가?** → `WARN` 이하. 400 응답은 정상 동작이지 오류가 아니다
- **정상 흐름에서도 찍히나?** → `INFO` 는 하루 수천 건이 한계다. 요청당 로그는 `DEBUG`

## 4. 메시지 작성

### `{}` 플레이스홀더를 쓴다

```java
// 나쁨 — 레벨이 꺼져 있어도 문자열 연결 연산이 돈다
log.debug("주문 " + orderId + " 조회 " + member.getEmail());

// 좋음
log.debug("주문 {} 조회", orderId);
```

Checkstyle 이 문자열 연결 로깅을 잡는다(아래 8절).

### 엔티티를 통째로 넣지 않는다

```java
// 나쁨 — toString() 이 연관을 타고 들어가 LazyInitializationException 이 나거나
//        이메일·전화번호가 통째로 로그에 실린다
log.info("회원 저장 {}", user);

// 좋음 — 식별자만
log.info("회원 저장 memberId={}", user.getId());
```

### 예외는 두 번째 인자로 넘긴다

```java
// 나쁨 — 스택트레이스가 사라진다
log.error("결제 실패: " + e.getMessage());

// 좋음
log.error("결제 실패 orderId={}", orderId, e);
```

### 반복문 안에서 찍지 않는다

N 건 처리는 시작/끝 요약 한 줄이다. 1000건 루프에 `log.info` 하나면 로그 1000줄이 된다.

```java
log.info("정산 대상 {}건 처리 시작", targets.size());
// ... 루프 ...
log.info("정산 완료 성공={} 실패={}", success, failed);
```

## 5. 예외 로깅은 한 곳에서만

**`*ExceptionHandler` 에서만 찍는다.** 서비스에서 `log.error` 하고 다시 던지면
같은 예외가 두 번 남아 장애 분석 때 중복 집계된다.

핸들러에서도 레벨을 나눈다.

| 예외 | 레벨 | 이유 |
| --- | --- | --- |
| `BusinessException`, 검증 실패 | `WARN`(또는 로깅 안 함) | 사용자 입력 오류다. 정상 동작이다 |
| `DataIntegrityViolationException` | `WARN` | 동시 가입 등 예상 가능한 경합 |
| 그 외 처리되지 않은 예외 | `ERROR` + 스택트레이스 | 예상 못 한 것이다 |

현재 [AuthExceptionHandler](../backend/src/main/java/com/expo/auth/exception/AuthExceptionHandler.java)
에는 로깅이 없다. 특히 `handleDataIntegrityViolation` 은 어떤 제약이 걸렸는지 판단한 뒤
원본 예외를 그대로 버린다 — 예상 밖 제약 위반이 들어오면 추적할 단서가 없다.
공통 핸들러로 승격할 때 같이 정리한다.

## 6. MDC — 요청 하나를 추적하는 법

로그가 여러 요청 사이에 섞이면 아무것도 못 읽는다. 요청마다 `traceId` 를 붙인다.

| 키 | 값 | 넣는 곳 |
| --- | --- | --- |
| `traceId` | UUID 앞 8자리 | `MdcLoggingFilter` (요청 진입 시) |
| `memberId` | 인증된 회원 PK | `JwtAuthenticationFilter` (인증 성공 시) |

**키 이름을 여기 적힌 대로만 쓴다.** `trace_id`, `traceID` 가 섞이면 수집기에서 필드가 갈라진다.

### `MDC.clear()` 를 빠뜨리면 안 된다

MDC 는 `ThreadLocal` 이다. 톰캣은 스레드를 풀에서 재사용하므로, 정리하지 않으면
**다음 요청의 로그에 이전 사용자의 `memberId` 가 그대로 붙는다.** 개인정보가 엉뚱한 요청에
섞여 들어가는 실제 사고로 이어진다.

```java
try {
    MDC.put("traceId", traceId);
    filterChain.doFilter(request, response);
} finally {
    MDC.clear();   // 예외가 나도 반드시 지운다
}
```

## 7. 남기면 안 되는 값

아래는 **어떤 레벨에서도** 로그에 넣지 않는다. `DEBUG` 라도 안 된다 —
로컬에서 찍은 로그가 그대로 이슈에 붙어 올라간다.

| 대상 | 비고 |
| --- | --- |
| 비밀번호 | 원문·해시 모두 |
| JWT, `Authorization` 헤더 | 토큰 자체가 인증 수단이다 |
| 토스 `secret-key`, AWS 자격증명 | |
| 사업자등록번호 | 개인사업자는 개인정보다 |
| 카드번호·결제 수단 정보 | |
| 이메일·전화번호 | 꼭 필요하면 `ab***@example.com` 형태로 마스킹 |

**요청/응답 본문을 통째로 찍지 않는다.** 가입 요청 하나에 위 항목 여러 개가 동시에 들어 있다.

## 8. 기계가 강제하는 것

아래는 [checkstyle.xml](../backend/config/checkstyle/checkstyle.xml) 이 잡는다. 부탁이 아니라 게이트다.

| 규칙 | 이유 |
| --- | --- |
| `System.out` / `System.err` | 운영에서 수집되지 않는다 |
| `printStackTrace()` | 스택트레이스가 로그 수집 밖으로 샌다 |
| `LoggerFactory.getLogger` 직접 호출 | `@Slf4j` 로 통일한다 |
| `log.info("..." + x)` 문자열 연결 | 플레이스홀더를 쓴다 |

**한계**: Checkstyle 의 `Regexp` 는 줄 단위라 여러 줄에 걸친 호출은 잡지 못한다.

```java
// 이건 통과해 버린다
log.info(
    "주문 " + orderId + " 생성");
```

완전한 강제가 아니라 대부분을 거르는 그물이다. 리뷰에서 한 번 더 본다.

## 9. 로그와 감사(audit)는 다르다

**이걸 혼동하면 나중에 되돌릴 수 없다.**

| | 로그 | 감사 |
| --- | --- | --- |
| 어디에 | stdout → 수집기 | PostgreSQL 테이블 |
| 언제까지 | 보관 기간이 지나면 사라진다 | 영구 |
| 무엇을 위해 | 장애 분석, 디버깅 | 법적 근거, 분쟁 대응, 변경 추적 |
| 신뢰성 | 유실될 수 있다 | 트랜잭션으로 보장 |

이 스키마에는 감사용 테이블과 컬럼이 **이미 설계돼 있다.**

- `expo_change_requests` / `expo_change_histories`
- 여러 테이블의 `before_data` / `after_data` (JSONB)

**"나중에 누가 무엇을 바꿨는지 확인해야 한다"면 그건 로그가 아니라 감사 테이블이다.**
결제 승인, 정산 확정, 박람회 승인·반려처럼 돈이나 권한이 걸린 변경은 전부 여기 해당한다.
로그에만 남기고 넘어가면 6개월 뒤 분쟁이 났을 때 근거가 없다.

## 10. 설정 위치

| 파일 | 무엇 |
| --- | --- |
| [application.yml](../backend/src/main/resources/application.yml) | 공통 |
| [application-local.yml](../backend/src/main/resources/application-local.yml) | `com.expo: DEBUG`, 콘솔 패턴 |
| [application-test.yml](../backend/src/main/resources/application-test.yml) | SQL 로그 |
| [application-prod.yml](../backend/src/main/resources/application-prod.yml) | `INFO` 고정, ECS JSON |

특정 패키지만 임시로 자세히 보고 싶으면 **`application-local.yml` 에서만** 레벨을 낮춘다.
prod 설정은 건드리지 않는다.
