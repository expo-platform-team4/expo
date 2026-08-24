package com.expo.support;

import com.expo.checkin.service.RowLockTimeout;
import java.util.function.Supplier;

/**
 * 잠금 대기 상한을 걸지 않고 그대로 실행하는 테스트용 대역.
 *
 * <p>{@link RowLockTimeout} 은 {@code SET LOCAL lock_timeout} 을 실행하므로 진짜 트랜잭션과 진짜 PostgreSQL 이
 * 필요하다. 판정 규칙만 보는 단위 테스트에는 DB 가 없다.
 *
 * <p>상한 자체가 동작하는지는 {@code RowLockTimeoutIntegrationTest} 가 실제 경합을 만들어 확인한다.
 */
public class PassthroughRowLockTimeout extends RowLockTimeout {

    public PassthroughRowLockTimeout() {
        super(null, 1);
    }

    @Override
    public <T> T runWithTimeout(Supplier<T> action) {
        return action.get();
    }
}
