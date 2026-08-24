package com.expo.checkin.service;

import com.expo.common.exception.BusinessException;
import com.expo.common.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.sql.SQLException;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

/**
 * 행 잠금 대기에 상한을 건다 (이슈 #74).
 *
 * <h2>왜 필요한가</h2>
 *
 * 발권과 체크인은 {@code PESSIMISTIC_WRITE} / {@code FOR UPDATE} 로 행을 잠그는데, PostgreSQL 의 기본
 * {@code lock_timeout} 은 <b>0(무제한)</b> 이다. 앞선 트랜잭션이 오래 잡고 있으면 뒤따르는 요청이 무한정 기다리고, 그동안 HTTP
 * 커넥션과 DB 커넥션이 함께 묶인다. 현장 체크인은 <b>줄이 서 있는 상황</b>에서 도는데, 응답이 영영 안 오는 것보다 "잠시 후 다시" 가 낫다.
 *
 * <h2>왜 JPA 힌트로는 안 되나</h2>
 *
 * {@code jakarta.persistence.lock.timeout} 힌트는 PostgreSQL 에서 기대대로 동작하지 않는다. Hibernate 가 그것을
 * {@code NOWAIT} / {@code SKIP LOCKED} 로 번역하는 경우는 있어도, PostgreSQL 에는 "3초만 기다린다" 를 뜻하는 <b>구문이
 * 없다.</b> 그건 세션 파라미터다.
 *
 * <h2>왜 전역 설정이 아닌가</h2>
 *
 * 커넥션 풀의 {@code connection-init-sql} 로 걸면 한 곳에서 끝나지만 <b>모든 쿼리에 걸린다.</b> 오래 걸려도 되는 배치까지 끊길 수
 * 있다. {@code SET LOCAL} 은 현재 트랜잭션에만 적용되고 커밋·롤백과 함께 사라진다.
 *
 * <p>지금 잠금을 쓰는 곳이 이 도메인 둘뿐이라 여기에 둔다. 다른 도메인이 같은 것을 필요로 하면 그때 {@code common} 으로 옮긴다.
 */
@Slf4j
@Component
public class RowLockTimeout {

    /** PostgreSQL 이 {@code lock_timeout} 초과로 올리는 SQLSTATE. */
    private static final String LOCK_NOT_AVAILABLE = "55P03";

    private final EntityManager entityManager;
    private final int timeoutMillis;

    public RowLockTimeout(
            EntityManager entityManager,
            @Value("${app.check-in.lock-timeout-millis:3000}") int timeoutMillis) {
        this.entityManager = entityManager;
        this.timeoutMillis = timeoutMillis;
    }

    /**
     * 잠금 대기 상한을 걸고 실행한다. 상한을 넘기면 {@link ErrorCode#RESOURCE_BUSY} 로 바꿔 던진다.
     *
     * <p>호출하는 쪽은 <b>트랜잭션 안</b>이어야 한다. {@code SET LOCAL} 은 트랜잭션 밖에서는 아무 일도 하지 않는다.
     *
     * <p>MyBatis 로 잠그는 경로에도 그대로 통한다 — JPA 와 MyBatis 가 같은 트랜잭션의 같은 커넥션을 쓴다.
     */
    public <T> T runWithTimeout(Supplier<T> action) {
        applyToCurrentTransaction();
        try {
            return action.get();
        } catch (DataAccessException | PersistenceException e) {
            // 둘 다 잡는 이유는 잠금 경로가 둘이기 때문이다. Spring Data 리포지토리는 예외를
            // DataAccessException 으로 번역해 주지만, 번역을 타지 않는 경로도 있어 JPA 쪽
            // 원본 타입까지 함께 받는다. 잠금 시간 초과가 아니면 그대로 흘려보낸다.
            if (!isLockTimeout(e)) {
                throw e;
            }
            log.warn("행 잠금 대기 시간 초과 timeoutMillis={}", timeoutMillis, e);
            throw new BusinessException(ErrorCode.RESOURCE_BUSY);
        }
    }

    /**
     * 값은 설정에서 오지만 바인딩 파라미터로 넘길 수 없다 — {@code SET} 은 파라미터를 받지 않는다. 그래서 문자열로 이어 붙이는데, 붙기 전에
     * 정수임을 강제한다.
     */
    private void applyToCurrentTransaction() {
        if (timeoutMillis <= 0) {
            throw new IllegalStateException(
                    "lock-timeout-millis 는 양수여야 한다. 0 은 PostgreSQL 에서 무제한을 뜻한다");
        }
        entityManager
                .createNativeQuery("SET LOCAL lock_timeout = " + timeoutMillis)
                .executeUpdate();
    }

    /** 원인 사슬을 따라 내려가며 {@code 55P03} 을 찾는다. 드라이버·ORM 이 몇 겹으로 감싸기 때문이다. */
    private static boolean isLockTimeout(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof SQLException sqlException
                    && LOCK_NOT_AVAILABLE.equals(sqlException.getSQLState())) {
                return true;
            }
            if (current.getCause() == current) {
                return false;
            }
        }
        return false;
    }
}
