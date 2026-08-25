import { useQuery } from '@tanstack/react-query'

import { useAuthStore } from '@/lib/auth'

import {
  getMyRecruitmentNoticeRequest,
  getPublicRecruitmentNotice,
  listAdminRecruitmentNotices,
  listMyRecruitmentNoticeRequests,
  listPublicRecruitmentNotices,
  listVenueHalls,
  listVenueZones,
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

/** `/admin/recruitment-notices` 목록. 상태 무관 전체. ADMIN 전용. */
export const useAdminRecruitmentNotices = () =>
  useQuery({
    queryKey: recruitmentKeys.adminNotices(),
    queryFn: listAdminRecruitmentNotices,
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

/** 가상 장소 목록. 공고 생성 요청 폼의 선택지. */
export const useVirtualVenues = () =>
  useQuery({
    queryKey: recruitmentKeys.virtualVenues(),
    queryFn: listVirtualVenues,
  })

/** 선택한 가상 장소의 홀 목록. 장소를 고르기 전에는 부르지 않는다(`enabled`). */
export const useVenueHalls = (virtualVenueId: number | null) =>
  useQuery({
    queryKey: recruitmentKeys.venueHalls(virtualVenueId ?? 0),
    queryFn: () => listVenueHalls(virtualVenueId as number),
    enabled: virtualVenueId !== null,
  })

/** 선택한 홀의 구역 목록. 홀을 고르기 전에는 부르지 않는다(`enabled`). */
export const useVenueZones = (hallId: number | null) =>
  useQuery({
    queryKey: recruitmentKeys.venueZones(hallId ?? 0),
    queryFn: () => listVenueZones(hallId as number),
    enabled: hallId !== null,
  })
