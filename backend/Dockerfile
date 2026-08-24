# 백엔드 이미지. ARM64(Ampere A1) 위에서 돈다.
#
# 베이스 이미지에 --platform 을 적지 않는다. temurin 은 멀티아치라 빌드하는 러너의
# 아키텍처를 따라가고, CI 가 ubuntu-24.04-arm 러너에서 빌드하므로 그대로 arm64 가 나온다.
# 여기에 linux/amd64 를 박으면 OCI 에서 exec format error 로 뜨지 않는다.

# ---------- 빌드 ----------
FROM eclipse-temurin:21-jdk AS build

WORKDIR /src

# 의존성 선언만 먼저 복사한다. 소스만 바뀐 재빌드에서 이 레이어가 캐시되어
# Gradle 의존성 내려받기를 건너뛴다.
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

COPY config ./config
COPY src ./src

# bootJar 만 돈다. build 를 쓰면 테스트와 Checkstyle 까지 도는데,
# 그건 CI 가 별도 job 으로 이미 한다. 여기서 또 하면 배포만 느려진다.
#
# --no-daemon: 컨테이너는 한 번 빌드하고 끝이라 데몬을 띄울 이유가 없다.
RUN ./gradlew bootJar --no-daemon

# ---------- 런타임 ----------
FROM eclipse-temurin:21-jre

# curl 은 헬스체크용이다. 배포 스크립트가 "떴는지" 를 물어볼 수단이 필요한데,
# JRE 이미지에는 HTTP 를 쏠 도구가 없다. 이게 없으면 배포가 "몇 초 기다린 뒤
# 됐겠지" 로 가고, 마이그레이션이 실패해도 성공으로 넘어간다.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# root 로 돌리지 않는다. 컨테이너가 뚫렸을 때 할 수 있는 일을 줄인다.
RUN useradd --system --create-home --shell /usr/sbin/nologin expo
USER expo

WORKDIR /app
COPY --from=build --chown=expo:expo /src/build/libs/*.jar app.jar

EXPOSE 8080

# 힙 상한을 컨테이너 메모리 비율로 잡는다.
#
# JVM 은 컨테이너 제한을 인식하지만 기본값(25%)은 낮고, 반대로 제한이 없으면
# 호스트 전체를 기준으로 잡아 같은 인스턴스의 다른 앱을 압박한다.
# compose 에서 mem_limit 을 걸고 여기서 그 비율을 정한다 — 둘이 짝이다.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=50 -XX:+ExitOnOutOfMemoryError"

# OOM 이면 프로세스를 죽인다. 힙이 꽉 찬 채로 살아 있으면
# health 는 통과하는데 요청은 처리 못 하는 상태가 되어 더 찾기 어렵다.

# start-period 를 넉넉히 준다. Flyway 마이그레이션 34개를 적용하고 뜨기까지
# 로컬 실측이 30초였고, ARM 인스턴스는 더 걸릴 수 있다. 이 시간 안의 실패는
# 재시도로 세지 않는다 — 짧게 잡으면 정상 기동 중에 unhealthy 로 찍힌다.
HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=3 \
    CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
