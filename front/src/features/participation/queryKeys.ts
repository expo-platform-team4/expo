/** participation 도메인 React Query 캐시 키. */
export const participationKeys = {
  all: ['participation'] as const,
  myList: () => [...participationKeys.all, 'my-list'] as const,
  detail: (applicationId: number) => [...participationKeys.all, 'detail', applicationId] as const,
  boothProducts: (recruitmentNoticeId: number) =>
    [...participationKeys.all, 'booth-products', recruitmentNoticeId] as const,
}
