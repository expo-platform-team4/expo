import { useQuery } from '@tanstack/react-query'

import { useClientSettlements } from '@/features/client/hooks'
import { useAuthStore } from '@/lib/auth'

import { getClientSettlementDetail } from './api'
import { settlementKeys } from './queryKeys'

/** 정산 목록. 목록 화면·상세 화면 둘 다 이 훅을 그대로 재사용한다(중복 정의하지 않는다). */
export { useClientSettlements }

/**
 * 정산 상세·티켓/부스 구분 리포트.
 *
 * 재시도하지 않는다 — 내 것이 아니면 404 인데(Spec.md 5절), 다시 불러도 결과가 같은
 * 종류의 실패라 재시도는 로딩만 길게 남긴다(`checkin` 모듈의 `useTicketView` 와 같은 이유).
 */
export const useClientSettlementDetail = (settlementId: number | null) => {
  const accessToken = useAuthStore((state) => state.accessToken)
  return useQuery({
    queryKey: settlementKeys.detail(settlementId ?? 0),
    queryFn: () => getClientSettlementDetail(settlementId as number),
    enabled: Boolean(accessToken) && settlementId !== null && Number.isFinite(settlementId),
    retry: false,
  })
}
