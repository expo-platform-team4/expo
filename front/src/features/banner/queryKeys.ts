/** banner 도메인 React Query 캐시 키. Spec.md 6절 — `{module}Keys.{자원}(...params)` 팩토리. */
export const bannerKeys = {
  all: ['banner'] as const,
  active: () => [...bannerKeys.all, 'active'] as const,
  myRequests: (page: number) => [...bannerKeys.all, 'my-requests', page] as const,
  /** 페이지를 뺀 접두사. 취소·신청 뒤 전체 페이지를 한꺼번에 무효화할 때 쓴다. */
  myRequestsAll: () => [...bannerKeys.all, 'my-requests'] as const,
  adminRequests: (status: string | undefined, page: number) =>
    [...bannerKeys.all, 'admin-requests', status ?? 'ALL', page] as const,
  conflicts: (requestId: number) => [...bannerKeys.all, 'conflicts', requestId] as const,
}
