/**
 * checkin 도메인 React Query 캐시 키.
 *
 * 토큰을 키에 넣는다. 링크마다 다른 주문이라 섞이면 안 된다.
 */
export const checkinKeys = {
  all: ['checkin'] as const,
  ticketView: (token: string) => [...checkinKeys.all, 'ticketView', token] as const,
  /** 박람회별 체크인 현황. `expoId` 로 나눈다 — 주최사가 여러 박람회를 열 수 있다. */
  summary: (expoId: number) => [...checkinKeys.all, 'summary', expoId] as const,
  /** 박람회별 체크인 이력. */
  history: (expoId: number, page: number, size: number) =>
    [...checkinKeys.all, 'history', expoId, page, size] as const,
}
