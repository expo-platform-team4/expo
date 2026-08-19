package com.expo.notification.service;

import com.expo.notification.dto.NotificationHistoryPage;
import com.expo.notification.dto.NotificationHistoryResponse;
import com.expo.notification.dto.NotificationHistoryRow;
import com.expo.notification.entity.NotificationStatus;
import com.expo.notification.repository.NotificationHistoryMapper;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 알림 이력 조회 (D-API-009).
 *
 * <p>운영자가 "문자가 안 갔다" 는 문의를 받았을 때 여는 화면이다. 그래서 <b>실패를 찾기 쉬운 것</b>이 목적이고,
 * 찾은 뒤 곧바로 재발송(D-API-010)으로 이어진다. 응답에 {@code retryable} 을 담는 이유가 그것이다 —
 * 화면이 상태 문자열을 보고 직접 판단하면 규칙이 두 곳으로 갈린다.
 */
@Service
public class NotificationHistoryService {

    /** 한 번에 가져갈 수 있는 최대 건수. 관리자 화면이라도 무제한은 아니다. */
    private static final int MAX_PAGE_SIZE = 100;

    private final NotificationHistoryMapper historyMapper;
    private final PhoneNumberMasker phoneNumberMasker;
    private final NotificationTextRebuilders textRebuilders;

    public NotificationHistoryService(
            NotificationHistoryMapper historyMapper,
            PhoneNumberMasker phoneNumberMasker,
            NotificationTextRebuilders textRebuilders) {
        this.historyMapper = historyMapper;
        this.phoneNumberMasker = phoneNumberMasker;
        this.textRebuilders = textRebuilders;
    }

    /**
     * 조건에 맞는 알림을 최신순으로 준다.
     *
     * <p>{@code offset} 을 {@code long} 으로 계산한다. {@code int} 로 곱하면 {@code page} 가 큰 값일 때
     * 오버플로해 음수 offset 이 되고, PostgreSQL 이 {@code OFFSET must not be negative} 로 거절해 500 이
     * 난다. 체크인 이력 조회에서 실제로 밟은 적이 있다.
     */
    @Transactional(readOnly = true)
    public NotificationHistoryPage search(
            String status,
            String templateCode,
            String referenceType,
            Long referenceId,
            Instant startAt,
            Instant endAt,
            int page,
            int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        long offset = (long) safePage * safeSize;

        List<NotificationHistoryRow> rows =
                historyMapper.search(
                        status,
                        templateCode,
                        referenceType,
                        referenceId,
                        startAt,
                        endAt,
                        safeSize,
                        offset);

        return new NotificationHistoryPage(
                historyMapper.countSearch(
                        status, templateCode, referenceType, referenceId, startAt, endAt),
                safePage,
                safeSize,
                rows.stream().map(this::toResponse).toList());
    }

    private NotificationHistoryResponse toResponse(NotificationHistoryRow row) {
        return new NotificationHistoryResponse(
                row.notificationId(),
                row.recipientUserId(),
                phoneNumberMasker.mask(row.recipientPhoneNumber()),
                row.channel(),
                row.templateCode(),
                row.referenceType(),
                row.referenceId(),
                row.status(),
                row.retryCount(),
                row.lastError(),
                row.sentAt(),
                row.createdAt(),
                row.attemptCount(),
                row.lastAttemptNo(),
                row.lastAttemptStatus(),
                row.lastAttemptErrorCode(),
                row.lastAttemptAt(),
                retryable(row));
    }

    /**
     * 재발송할 수 있는가.
     *
     * <p><b>{@code FAILED} 만 재발송한다.</b> 나머지를 뺀 이유가 각각 다르다.
     *
     * <ul>
     *   <li>{@code SENT} — 이미 나갔다. 다시 보내면 같은 사람이 두 번 받는다
     *   <li>{@code CANCELED} — 보낼 번호가 없어서 시도조차 안 한 건이다. 다시 눌러도 결과가 같다
     *   <li>{@code PENDING} — 발송 중일 수 있다. 응답을 기다리는 중에 또 보내면 중복이다
     *   <li>{@code RETRYING} — 워커가 잡고 있다는 뜻이다 (아직 워커가 없어 실제로는 안 쓰인다)
     * </ul>
     *
     * <p>번호가 없으면 {@code FAILED} 라도 못 보낸다. 상태만 보고 판단하면 화면에 눌러도 안 되는 버튼이 생긴다.
     *
     * <p>본문을 다시 만들 수 있는 템플릿인지도 본다. 재구성기가 없는 템플릿은 재발송 API 가 409 로
     * 거절하므로, 목록에서 미리 걸러야 눌러도 안 되는 버튼이 안 생긴다.
     *
     * <h2>여기서 걸러지지 않는 실패가 하나 남는다</h2>
     *
     * 발권 알림은 <b>살아 있는 접근 토큰이 있어야</b> 재발송된다(만료를 물려받아야 하므로). 그 확인은
     * 알림마다 토큰을 한 번씩 조회해야 해서, 100건짜리 목록이 쿼리 100번이 된다. 목록에서는 하지
     * 않고 재발송 시점에 판단한다.
     *
     * <p>그래서 {@code retryable} 은 <b>"재발송을 시도할 수 있다"</b>는 뜻이지 "성공한다" 는 뜻이 아니다.
     */
    private boolean retryable(NotificationHistoryRow row) {
        return NotificationStatus.FAILED.name().equals(row.status())
                && row.recipientPhoneNumber() != null
                && !row.recipientPhoneNumber().isBlank()
                && textRebuilders.supports(row.templateCode());
    }
}
