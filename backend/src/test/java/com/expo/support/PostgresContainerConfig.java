package com.expo.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * 스프링 컨텍스트를 띄우는 테스트가 붙을 <b>실제 PostgreSQL</b>.
 *
 * <h2>왜 H2 를 버렸나</h2>
 *
 * H2 는 {@code V1__init_schema.sql} 의 PostgreSQL 전용 구문({@code CREATE EXTENSION btree_gist},
 * {@code JSONB}, {@code EXCLUDE} 제약)을 실행하지 못한다. 그래서 test 프로필은 <b>Flyway 를 끄고</b>
 * JPA 가 엔티티로 스키마를 만들게 해 두었다.
 *
 * <p>그러면 테스트는 통과하지만 <b>엔티티와 마이그레이션이 어긋나도 아무도 모른다.</b> 검사 대상과
 * 검사 기준이 같은 곳(엔티티)에서 나오기 때문이다. 실제로 이 프로젝트의 로컬 DB 가 두 주 동안
 * 마이그레이션과 어긋난 채였는데 329개 테스트 중 하나도 잡지 못했다.
 *
 * <p>Testcontainers 는 매 실행마다 빈 PostgreSQL 을 띄우고 Flyway 가 마이그레이션을 처음부터
 * 적용한다. 거기에 {@code ddl-auto: validate} 를 걸면 <b>엔티티와 실제 스키마를 전수 대조</b>한다.
 * 어긋나면 기동에서 터진다.
 *
 * <h2>컨테이너는 한 번만 뜬다</h2>
 *
 * {@code @Bean} 으로 두면 스프링이 수명주기를 관리하고, <b>스프링 테스트 컨텍스트 캐시</b>가 같은
 * 설정을 쓰는 테스트끼리 컨텍스트를 재사용한다. 클래스마다 새로 띄우지 않는다.
 *
 * <p>{@link ServiceConnection} 이 컨테이너의 접속 정보를 {@code spring.datasource.*} 에 자동으로
 * 꽂는다. 그래서 {@code application-test.yml} 에는 datasource 를 적지 않는다 — 적으면 포트가 매번
 * 달라지는 컨테이너와 어긋난다.
 *
 * <h2>이미지 버전을 로컬과 맞춘다</h2>
 *
 * {@code docker-compose.yml} 과 같은 {@code postgres:18-alpine} 이다. 테스트가 도는 PostgreSQL 과
 * 개발자가 쓰는 PostgreSQL 이 다르면 여기서 통과한 것이 저기서 깨진다.
 *
 * <h2>Docker 가 필요하다</h2>
 *
 * 이 설정을 쓰는 테스트는 Docker 데몬 없이는 돌지 않는다. 순수 단위 테스트(스프링 컨텍스트를 안
 * 띄우는 것들)는 영향을 받지 않으므로, Docker 가 없는 환경에서도 대부분의 테스트는 그대로 돈다.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresContainerConfig {

    /** 로컬 개발 환경과 같은 이미지. {@code docker-compose.yml} 을 바꾸면 여기도 바꾼다. */
    private static final String IMAGE = "postgres:18-alpine";

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(IMAGE);
    }
}
