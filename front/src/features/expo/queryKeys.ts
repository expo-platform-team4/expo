import type { ExpoCardQuery } from './api'

/**
 * expo 도메인 React Query 캐시 키.
 *
 * 목록 키에 필터를 통째로 넣는다 — 지역·검색어·정렬이 바뀌면 다른 결과이므로
 * 같은 캐시에 담기면 안 된다.
 */
export const expoKeys = {
  all: ['expo'] as const,
  cards: (query: ExpoCardQuery) => [...expoKeys.all, 'cards', query] as const,
  detail: (expoId: number) => [...expoKeys.all, 'detail', expoId] as const,
  purchasableTicketProducts: (expoId: number) =>
    [...expoKeys.all, 'purchasableTicketProducts', expoId] as const,
}
