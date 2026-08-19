/**
 * expo 도메인 React Query 캐시 키.
 *
 * 박람회 자체를 나열·조회하는 공개 API 가 없어(GitHub 이슈 #107) 지금은 상세 화면의
 * "구매 가능한 티켓 상품" 하나뿐이다. expoId 별로 캐시가 섞이면 안 되므로 키에 넣는다.
 */
export const expoKeys = {
  all: ['expo'] as const,
  purchasableTicketProducts: (expoId: number) =>
    [...expoKeys.all, 'purchasableTicketProducts', expoId] as const,
}
