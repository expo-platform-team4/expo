import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { useAuthStore } from '@/lib/auth'
import { clientKeys } from '@/features/client/queryKeys'
import {
  approveExpoOpeningRequest,
  listAdminExpoOpeningRequests,
  rejectExpoOpeningRequest,
  type ExpoOpeningRequestStatus,
} from '@/features/client/expoOpeningApi'

/**
 * 관리자 개최 신청 목록.
 *
 * API 호출부는 `features/client/expoOpeningApi.ts` 에 주최사용과 함께 둔다 — 같은 자원의
 * 두 관점이라 타입을 한 벌만 유지하는 편이 어긋날 여지가 적다.
 */
export const useAdminExpoOpeningRequests = (status?: ExpoOpeningRequestStatus) => {
  const accessToken = useAuthStore((state) => state.accessToken)
  return useQuery({
    queryKey: clientKeys.adminExpoOpeningRequests(status),
    queryFn: () => listAdminExpoOpeningRequests(status),
    enabled: Boolean(accessToken),
  })
}

/** 승인. 백엔드가 같은 트랜잭션에서 expos 행을 만든다. */
export const useApproveExpoOpening = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: approveExpoOpeningRequest,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'expo-opening-requests'] })
    },
  })
}

export const useRejectExpoOpening = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ requestId, reason }: { requestId: number; reason: string }) =>
      rejectExpoOpeningRequest(requestId, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'expo-opening-requests'] })
    },
  })
}
