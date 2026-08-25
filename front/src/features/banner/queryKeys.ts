/** banner 도메인 React Query 캐시 키. Spec.md 6절 — `{module}Keys.{자원}(...params)` 팩토리. */
export const bannerKeys = {
  all: ['banner'] as const,
  active: () => [...bannerKeys.all, 'active'] as const,
  myRequests: () => [...bannerKeys.all, 'my-requests'] as const,
  adminRequests: (status?: string) =>
    [...bannerKeys.all, 'admin-requests', status ?? 'ALL'] as const,
  conflicts: (requestId: number) => [...bannerKeys.all, 'conflicts', requestId] as const,
}
