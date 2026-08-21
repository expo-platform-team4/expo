/**
 * 날짜 포맷 유틸. Spec.md 9절 — 백엔드는 `Instant`(UTC, ISO-8601)로 준다.
 * 서버 타임존을 가정하지 않고, 보는 사람의 브라우저 로컬 타임존으로 표시한다.
 *
 * `checkin` 모듈의 `TicketViewPage.tsx` 가 이미 같은 패턴(`toLocaleString('ko-KR', ...)`)을
 * 쓰고 있다 — 여기로 옮겨 모듈 간에 재사용한다.
 */

/** `2026. 8. 20. 오후 3:00` 형태. 날짜와 시각을 함께 보여줄 때. */
export const formatDateTime = (isoInstant: string): string =>
  new Date(isoInstant).toLocaleString('ko-KR', {
    year: 'numeric',
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })

/** `2026. 8. 20.` 형태. 날짜만 필요할 때. */
export const formatDate = (isoInstant: string): string =>
  new Date(isoInstant).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'numeric',
    day: 'numeric',
  })
