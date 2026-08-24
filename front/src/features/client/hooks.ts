import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { useAuthStore } from '@/lib/auth'

import {
  getClientDashboardProfile,
  getClientMyConfirmedBooths,
  getClientMyExpos,
  getClientMyRecruitmentResults,
  getClientSettlements,
} from './api'
import {
  createExpoOpeningRequest,
  listMyExpoOpeningRequests,
  submitExpoOpeningRequest,
  type ExpoOpeningRequestPayload,
} from './expoOpeningApi'
import { clientKeys } from './queryKeys'

export const useClientDashboardProfile = () => {
  const accessToken = useAuthStore((state) => state.accessToken)
  return useQuery({
    queryKey: clientKeys.dashboardProfile,
    queryFn: getClientDashboardProfile,
    enabled: Boolean(accessToken),
  })
}

/** 내가 연 박람회 목록. 대시보드 요약 지표와 "내 박람회" 화면이 함께 쓴다. */
export const useClientMyExpos = () => {
  const accessToken = useAuthStore((state) => state.accessToken)
  return useQuery({
    queryKey: clientKeys.myExpos,
    queryFn: getClientMyExpos,
    enabled: Boolean(accessToken),
  })
}

/** 확정 배정 부스 목록. 대시보드 요약 지표가 쓴다. */
export const useClientMyConfirmedBooths = () => {
  const accessToken = useAuthStore((state) => state.accessToken)
  return useQuery({
    queryKey: clientKeys.myConfirmedBooths,
    queryFn: getClientMyConfirmedBooths,
    enabled: Boolean(accessToken),
  })
}

/** 모집공고 신청·확정 배정 현황. 대시보드 요약 지표가 쓴다. */
export const useClientMyRecruitmentResults = () => {
  const accessToken = useAuthStore((state) => state.accessToken)
  return useQuery({
    queryKey: clientKeys.myRecruitmentResults,
    queryFn: getClientMyRecruitmentResults,
    enabled: Boolean(accessToken),
  })
}

/** 내 정산 목록. "내 박람회" 화면이 박람회별 매출 요약을 붙일 때 쓴다. */
export const useClientSettlements = (page = 0, size = 100) => {
  const accessToken = useAuthStore((state) => state.accessToken)
  return useQuery({
    queryKey: clientKeys.settlements(page),
    queryFn: () => getClientSettlements(page, size),
    enabled: Boolean(accessToken),
  })
}

/** 내 박람회 개최 신청 목록. `/client/expos/new` 화면이 쓴다. */
export const useMyExpoOpeningRequests = () => {
  const accessToken = useAuthStore((state) => state.accessToken)
  return useQuery({
    queryKey: clientKeys.myExpoOpeningRequests(),
    queryFn: listMyExpoOpeningRequests,
    enabled: Boolean(accessToken),
  })
}

/** 개최 신청 작성. 성공하면 내 신청 목록과 내 박람회 목록을 모두 무효화한다. */
export const useCreateExpoOpeningRequest = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      content,
      submitNow,
    }: {
      content: ExpoOpeningRequestPayload
      submitNow: boolean
    }) => createExpoOpeningRequest(content, submitNow),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: clientKeys.myExpoOpeningRequests() })
    },
  })
}

/** 임시저장한 신청을 심사 요청으로 올린다. */
export const useSubmitExpoOpeningRequest = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: submitExpoOpeningRequest,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: clientKeys.myExpoOpeningRequests() })
    },
  })
}
