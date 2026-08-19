/**
 * Spec.md 6절 쿼리 키 패턴.
 *
 * 목록은 `features/client/queryKeys.ts` 의 `clientKeys.settlements(page)` 를 그대로 재사용한다
 * (list 훅도 `useClientSettlements` 를 재사용 — Function.md 3절, 이 모듈은 기존 목록을 다시
 * 만들지 않는다). 여기는 이 모듈이 새로 추가하는 상세 조회 키만 갖는다.
 */
export const settlementKeys = {
  all: ['settlement'] as const,
  detail: (id: number) => [...settlementKeys.all, 'detail', id] as const,
}
