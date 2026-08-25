import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import {
  checkParticipationApplication,
  completeParticipationApplicationCorrection,
  listAdminParticipationApplications,
  listParticipationApplicationHistory,
  requestParticipationApplicationCorrection,
  updateParticipationApplicationMemo,
} from './participationApi'
import { adminKeys } from './queryKeys'

/** `/admin/participation-applications` — 참여 신청서 목록(전체 상태). */
export const useAdminParticipationApplications = () =>
  useQuery({
    queryKey: adminKeys.participationApplications(),
    queryFn: () => listAdminParticipationApplications(),
  })

/** 신청서 한 건의 운영 이력. 카드를 펼쳤을 때만 불러온다(`enabled`). */
export const useParticipationApplicationHistory = (applicationId: number, enabled: boolean) =>
  useQuery({
    queryKey: adminKeys.participationApplicationHistory(applicationId),
    queryFn: () => listParticipationApplicationHistory(applicationId),
    enabled,
  })

const useInvalidateApplication = (applicationId: number) => {
  const queryClient = useQueryClient()
  return () => {
    queryClient.invalidateQueries({ queryKey: adminKeys.participationApplications() })
    queryClient.invalidateQueries({
      queryKey: adminKeys.participationApplicationHistory(applicationId),
    })
  }
}

/** 운영 확인 처리. */
export const useCheckParticipationApplication = (applicationId: number) => {
  const invalidate = useInvalidateApplication(applicationId)
  return useMutation({
    mutationFn: (message?: string) => checkParticipationApplication(applicationId, message),
    onSuccess: invalidate,
  })
}

/** 보완 요청. */
export const useRequestParticipationApplicationCorrection = (applicationId: number) => {
  const invalidate = useInvalidateApplication(applicationId)
  return useMutation({
    mutationFn: (message: string) =>
      requestParticipationApplicationCorrection(applicationId, message),
    onSuccess: invalidate,
  })
}

/** 보완 완료 처리. */
export const useCompleteParticipationApplicationCorrection = (applicationId: number) => {
  const invalidate = useInvalidateApplication(applicationId)
  return useMutation({
    mutationFn: (message?: string) =>
      completeParticipationApplicationCorrection(applicationId, message),
    onSuccess: invalidate,
  })
}

/** 관리자 메모 갱신. */
export const useUpdateParticipationApplicationMemo = (applicationId: number) => {
  const invalidate = useInvalidateApplication(applicationId)
  return useMutation({
    mutationFn: (memo: string) => updateParticipationApplicationMemo(applicationId, memo),
    onSuccess: invalidate,
  })
}
