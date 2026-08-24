/**
 * ticket 도메인 React Query 캐시 키.
 *
 * 주문 생성·비회원 조회는 전부 뮤테이션이라 캐시 키가 필요 없다 — 구매 가능 티켓 상품
 * 목록(GET) 하나만 키를 쓴다. expoId 별로 캐시가 섞이면 안 되므로 키에 넣는다
 * (`features/expo/queryKeys.ts` 의 `expoKeys` 와 같은 모양).
 */
export const ticketKeys = {
  all: ['ticket'] as const,
  purchasableTicketProducts: (expoId: number) =>
    [...ticketKeys.all, 'purchasableTicketProducts', expoId] as const,
  myOrders: () => [...ticketKeys.all, 'myOrders'] as const,
  myTickets: () => [...ticketKeys.all, 'myTickets'] as const,
  clientTicketProducts: (expoId: number) =>
    [...ticketKeys.all, 'clientTicketProducts', expoId] as const,
}
