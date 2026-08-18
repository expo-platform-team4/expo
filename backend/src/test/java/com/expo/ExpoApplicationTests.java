package com.expo;

import com.expo.support.PostgresContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * 애플리케이션이 뜨는지 본다. <b>이 프로젝트에서 가장 많은 것을 검사하는 테스트다.</b>
 *
 * <p>이름은 "컨텍스트가 로드되나" 지만, 실제로 통과하려면 아래가 전부 맞아야 한다.
 *
 * <ul>
 *   <li>Flyway 마이그레이션 전부가 빈 PostgreSQL 위에서 순서대로 적용된다
 *   <li>{@code ddl-auto: validate} 가 <b>엔티티와 실제 스키마를 전수 대조</b>해 통과한다
 *   <li>모든 빈이 생성되고 의존성이 해결된다 — 순환 참조, 중복 빈, 누락 설정이 여기서 드러난다
 *   <li>MyBatis 매퍼 XML 이 전부 파싱되고 인터페이스와 짝이 맞는다
 * </ul>
 *
 * <p>두 번째가 이 테스트를 붙인 이유다. 예전에는 H2 위에서 JPA 가 엔티티로 스키마를 만들었기 때문에
 * <b>엔티티와 마이그레이션이 어긋나도 항상 통과했다.</b> 검사 대상과 검사 기준이 같은 곳에서 나왔다.
 * 이제는 마이그레이션이 만든 스키마를 엔티티가 검사한다.
 */
@SpringBootTest
@Import(PostgresContainerConfig.class)
class ExpoApplicationTests {

    @Test
    void contextLoads() {}
}
