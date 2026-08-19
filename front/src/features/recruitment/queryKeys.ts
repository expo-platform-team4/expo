/** recruitment 도메인 React Query 캐시 키. Spec.md 6절 — `{module}Keys.{자원}(...params)` 팩토리. */
export const recruitmentKeys = {
  all: ['recruitment'] as const,
  notices: () => [...recruitmentKeys.all, 'notices'] as const,
  noticeDetail: (noticeId: number) => [...recruitmentKeys.all, 'notice', noticeId] as const,
  requests: () => [...recruitmentKeys.all, 'requests'] as const,
  requestDetail: (requestId: number) => [...recruitmentKeys.all, 'request', requestId] as const,
  virtualVenues: () => [...recruitmentKeys.all, 'virtual-venues'] as const,
}
