import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { useAuthStore } from '@/lib/auth'

import {
  createParticipationApplication,
  getMyParticipationApplication,
  listAvailableBoothProducts,
  listMyParticipations,
  updateParticipationApplication,
  withdrawParticipationApplication,
  type UpdateParticipationApplicationPayload,
} from './api'
import { participationKeys } from './queryKeys'

/** 참여 신청서 작성. 성공하면 내 참여 목록 캐시를 무효화한다. */
export const useCreateParticipationApplication = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createParticipationApplication,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: participationKeys.myList() })
    },
  })
}

/** 내 참여 신청서 상세. */
export const useMyParticipationApplication = (applicationId: number | null) => {
  const accessToken = useAuthStore((state) => state.accessToken)
  return useQuery({
    queryKey: participationKeys.detail(applicationId ?? 0),
    queryFn: () => getMyParticipationApplication(applicationId as number),
    enabled: Boolean(accessToken) && applicationId !== null && Number.isFinite(applicationId),
  })
}

/** `/client/participations` 화면 — 내가 참여기업으로 신청한 목록. */
export const useMyParticipations = () => {
  const accessToken = useAuthStore((state) => state.accessToken)
  return useQuery({
    queryKey: participationKeys.myList(),
    queryFn: listMyParticipations,
    enabled: Boolean(accessToken),
  })
}

/** 공고에 등록된, 구매 가능한 부스 상품 목록. 참여 신청 폼의 선택지. */
export const useAvailableBoothProducts = (recruitmentNoticeId: number | null) =>
  useQuery({
    queryKey: participationKeys.boothProducts(recruitmentNoticeId ?? 0),
    queryFn: () => listAvailableBoothProducts(recruitmentNoticeId as number),
    enabled: recruitmentNoticeId !== null && Number.isFinite(recruitmentNoticeId),
  })

/** 참여 신청서 초안 수정. 성공하면 상세·목록 캐시를 무효화한다. */
export const useUpdateParticipationApplication = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      applicationId,
      payload,
    }: {
      applicationId: number
      payload: UpdateParticipationApplicationPayload
    }) => updateParticipationApplication(applicationId, payload),
    onSuccess: (application) => {
      queryClient.invalidateQueries({ queryKey: participationKeys.detail(application.id) })
      queryClient.invalidateQueries({ queryKey: participationKeys.myList() })
    },
  })
}

/** 참여 신청 철회. 성공하면 상세·목록 캐시를 무효화한다. */
export const useWithdrawParticipationApplication = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: withdrawParticipationApplication,
    onSuccess: (application) => {
      queryClient.invalidateQueries({ queryKey: participationKeys.detail(application.id) })
      queryClient.invalidateQueries({ queryKey: participationKeys.myList() })
    },
  })
}
