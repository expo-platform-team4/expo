package com.expo.common.scheduling;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * {@code app.scheduling.enabled} 가 꺼져 있으면 스케줄러를 아예 만들지 않는다.
 *
 * <h2>왜 필요한가</h2>
 *
 * 테스트에서 스케줄러가 돌면 <b>테스트가 보고 있는 데이터를 배경에서 바꾼다.</b> 예를 들어
 * 배너 통합 테스트는 "정리 전에는 안 보이고 정리 후에 보인다" 를 검증하는데, 그 사이에
 * 스케줄러가 끼어들면 무엇 때문에 바뀌었는지 알 수 없게 된다. 실패도 성공도 못 믿는다.
 *
 * <p>기본값은 켜짐이다. 끄는 것은 {@code application-test.yml} 뿐이다.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public @interface SchedulingEnabled {}
