import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { useAuthStore } from '@/lib/auth'

import {
  createRecruitmentNoticeRequest,
  getMyRecruitmentNoticeRequest,
  getPublicRecruitmentNotice,
  listMyRecruitmentNoticeRequests,
  listPublicRecruitmentNotices,
  listVirtualVenues,
} from './api'
import { recruitmentKeys } from './queryKeys'

/** `/recruitment-notices` 목록. 공개 — 인증 없이 호출한다. */
export const useRecruitmentNotices = () =>
  useQuery({
    queryKey: recruitmentKeys.notices(),
    queryFn: listPublicRecruitmentNotices,
  })

/** `/recruitment-notices/{noticeId}` 상세. 공개. */
export const useRecruitmentNotice = (noticeId: number | null) =>
  useQuery({
    queryKey: recruitmentKeys.noticeDetail(noticeId ?? 0),
    queryFn: () => getPublicRecruitmentNotice(noticeId as number),
    enabled: noticeId !== null && Number.isFinite(noticeId),
  })

/** 내 모집공고 생성 요청 목록. `/client/recruitment-notice-requests` 화면 아래에서만 쓴다(CLIENT 전용). */
export const useMyRecruitmentNoticeRequests = () => {
  const accessToken = useAuthStore((state) => state.accessToken)
  return useQuery({
    queryKey: recruitmentKeys.requests(),
    queryFn: listMyRecruitmentNoticeRequests,
    enabled: Boolean(accessToken),
  })
}

/** 내 모집공고 생성 요청 상세. */
export const useMyRecruitmentNoticeRequest = (requestId: number | null) => {
  const accessToken = useAuthStore((state) => state.accessToken)
  return useQuery({
    queryKey: recruitmentKeys.requestDetail(requestId ?? 0),
    queryFn: () => getMyRecruitmentNoticeRequest(requestId as number),
    enabled: Boolean(accessToken) && requestId !== null && Number.isFinite(requestId),
  })
}

/** 모집공고 생성 요청 작성. 성공하면 목록 캐시를 무효화한다(Spec.md 6절). */
export const useCreateRecruitmentNoticeRequest = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createRecruitmentNoticeRequest,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: recruitmentKeys.requests() })
    },
  })
}

/** 가상 장소 목록. 공고 생성 요청 폼의 선택지. */
export const useVirtualVenues = () =>
  useQuery({
    queryKey: recruitmentKeys.virtualVenues(),
    queryFn: listVirtualVenues,
  })
