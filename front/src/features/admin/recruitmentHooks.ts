import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import {
  cancelAdminNotice,
  closeAdminNotice,
  createAdminNotice,
  createAdminNoticeRequest,
  decideVenue,
  listAdminNoticeRequests,
  listAdminNotices,
  publishAdminNotice,
  type CreateAdminNoticePayload,
  type CreateAdminNoticeRequestPayload,
  type DecideVenuePayload,
} from './recruitmentApi'
import { adminKeys } from './queryKeys'

/** `/admin/recruitment-notice-requests` — 모집공고 생성 요청 목록. */
export const useAdminNoticeRequests = () =>
  useQuery({
    queryKey: adminKeys.noticeRequests(),
    queryFn: listAdminNoticeRequests,
  })

/** 모집공고 생성 요청 작성. 승인된 박람회를 근거로 관리자가 대신 작성한다. */
export const useCreateAdminNoticeRequest = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: CreateAdminNoticeRequestPayload) => createAdminNoticeRequest(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminKeys.noticeRequests() })
    },
  })
}

/** 장소 충돌 판정. 성공하면 요청 목록 캐시를 무효화한다. */
export const useDecideVenue = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ requestId, payload }: { requestId: number; payload: DecideVenuePayload }) =>
      decideVenue(requestId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminKeys.noticeRequests() })
    },
  })
}

/** `/admin/recruitment-notices` — 기업 모집 공고 목록. */
export const useAdminNotices = () =>
  useQuery({
    queryKey: adminKeys.notices(),
    queryFn: listAdminNotices,
  })

/** 기업 모집 공고 초안 생성. 승인된 모집공고 생성 요청을 근거로 만든다. */
export const useCreateAdminNotice = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: CreateAdminNoticePayload) => createAdminNotice(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminKeys.notices() })
    },
  })
}

/** 공고 게시·마감·취소 공통 무효화. 셋 다 목록 캐시 하나만 갱신하면 된다. */
const useNoticeActionMutation = <TVariables>(
  mutationFn: (variables: TVariables) => Promise<unknown>
) => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminKeys.notices() })
    },
  })
}

export const usePublishAdminNotice = () => useNoticeActionMutation(publishAdminNotice)
export const useCloseAdminNotice = () => useNoticeActionMutation(closeAdminNotice)
export const useCancelAdminNotice = () =>
  useNoticeActionMutation(({ noticeId, reason }: { noticeId: number; reason?: string }) =>
    cancelAdminNotice(noticeId, reason)
  )
