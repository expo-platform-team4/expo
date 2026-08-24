import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import {
  calculateSettlement,
  confirmSettlement,
  recordSettlementRemittance,
  searchAdminSettlements,
  type RecordRemittancePayload,
  type SearchSettlementsParams,
} from './settlementApi'
import { adminKeys } from './queryKeys'

/** `/admin/settlements` — 정산 대상 목록·검색. */
export const useAdminSettlements = (params: SearchSettlementsParams = {}) =>
  useQuery({
    queryKey: adminKeys.settlements(params),
    queryFn: () => searchAdminSettlements(params),
  })

/** 정산 재계산·확정·송금 기록 공통 무효화. Spec.md 6절 — 서버 상태를 바꾸는 액션은 목록을 무효화한다. */
const useSettlementActionMutation = <TVariables>(
  mutationFn: (variables: TVariables) => Promise<unknown>
) => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminKeys.all })
    },
  })
}

export const useCalculateSettlement = () => useSettlementActionMutation(calculateSettlement)
export const useConfirmSettlement = () => useSettlementActionMutation(confirmSettlement)
export const useRecordRemittance = () =>
  useSettlementActionMutation(
    ({ settlementId, payload }: { settlementId: number; payload: RecordRemittancePayload }) =>
      recordSettlementRemittance(settlementId, payload)
  )
