/** booth 도메인 React Query 캐시 키. Spec.md 6절 — `{module}Keys.{자원}(...params)` 팩토리 형태. */
export const boothKeys = {
  all: ['booth'] as const,
  myList: () => [...boothKeys.all, 'my-list'] as const,
  allocationDetail: (allocationId: number) =>
    [...boothKeys.all, 'allocation', allocationId] as const,
  publishedContent: (allocationId: number) =>
    [...boothKeys.all, 'published-content', allocationId] as const,
}
