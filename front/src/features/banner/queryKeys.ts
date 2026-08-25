/** banner 도메인 React Query 캐시 키. */
export const bannerKeys = {
  all: ['banner'] as const,
  active: () => [...bannerKeys.all, 'active'] as const,
}
