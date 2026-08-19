import { useQuery } from '@tanstack/react-query'

import { fetchPurchasableTicketProducts } from './api'
import { expoKeys } from './queryKeys'

/**
 * 박람회 상세의 구매 가능 티켓 상품 목록.
 *
 * `expoId` 가 `null` 이면(라우트 파라미터가 숫자가 아닌 경우) 요청을 보내지 않는다.
 * 존재하지 않는 expoId 는 400 으로 실패하는데, 다시 불러도 같은 결과라 재시도하지
 * 않는다(Spec.md 5절 — checkin 모듈의 `useTicketView` 와 같은 패턴).
 */
export const usePurchasableTicketProducts = (expoId: number | null) =>
  useQuery({
    queryKey: expoKeys.purchasableTicketProducts(expoId ?? 0),
    queryFn: () => fetchPurchasableTicketProducts(expoId as number),
    enabled: expoId !== null,
    retry: false,
  })
