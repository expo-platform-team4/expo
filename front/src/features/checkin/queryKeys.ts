/**
 * checkin 도메인 React Query 캐시 키.
 *
 * 토큰을 키에 넣는다. 링크마다 다른 주문이라 섞이면 안 된다.
 */
export const checkinKeys = {
  all: ['checkin'] as const,
  ticketView: (token: string) => [...checkinKeys.all, 'ticketView', token] as const,
}
