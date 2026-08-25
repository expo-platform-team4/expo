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

/**
 * UTC ISO 문자열을 `<input type="datetime-local">` 에 그대로 넣을 수 있는 `YYYY-MM-DDTHH:mm`
 * 형태(브라우저 로컬 타임존 기준)로 바꾼다.
 *
 * `isoInstant.slice(0, 16)` 로 UTC 문자열을 그냥 잘라 넣으면 안 된다 — datetime-local 값은
 * 타임존 표기가 없는 대신 브라우저가 "로컬 타임존 기준"으로 해석하므로, UTC 자정을 그대로
 * 넣으면 KST(UTC+9) 환경에서 실제 시각보다 9시간 이르게 보이고, 그대로 제출하면 서버에는
 * 9시간 어긋난 시각이 저장된다.
 */
export const toDatetimeLocalValue = (isoInstant: string): string => {
  const date = new Date(isoInstant)
  const localMs = date.getTime() - date.getTimezoneOffset() * 60_000
  return new Date(localMs).toISOString().slice(0, 16)
}

/**
 * UTC ISO 문자열을 `<input type="date">` 에 넣을 수 있는 `YYYY-MM-DD` 형태(브라우저 로컬
 * 타임존 기준)로 바꾼다. {@link toDatetimeLocalValue} 와 같은 이유로 `slice(0, 10)` 로 UTC
 * 문자열을 그냥 잘라 쓰면 안 된다.
 */
export const toDateInputValue = (isoInstant: string): string =>
  toDatetimeLocalValue(isoInstant).slice(0, 10)
