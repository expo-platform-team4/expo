import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { useAuthStore } from '@/lib/auth'

import {
  approveBannerRequest,
  cancelBannerRequest,
  createBannerRequest,
  fetchActiveBanners,
  listAdminBannerRequests,
  listBannerConflicts,
  listMyBannerRequests,
  rejectBannerRequest,
  type BannerReviewStatus,
} from './api'
import { bannerKeys } from './queryKeys'

/**
 * 메인 배너 조회.
 *
 * 배너는 자주 바뀌지 않는데(승인·기간 단위로 움직인다) 메인 홈에서 매번 부른다.
 * `staleTime` 을 길게 잡아 페이지를 오갈 때마다 다시 부르지 않게 한다.
 *
 * 실패해도 재시도하지 않는다 — 배너는 <b>보조 요소</b>라, 안 보이더라도 홈은 그대로
 * 쓸 수 있어야 한다. 로딩을 길게 끌지 않는 편이 낫다.
 */
export const useActiveBanners = () =>
  useQuery({
    queryKey: bannerKeys.active(),
    queryFn: fetchActiveBanners,
    staleTime: 5 * 60 * 1000,
    retry: false,
  })

/** 내 배너 신청 목록. CLIENT 전용이라 토큰이 있을 때만 부른다. */
export const useMyBannerRequests = (page: number) => {
  const accessToken = useAuthStore((state) => state.accessToken)
  return useQuery({
    queryKey: bannerKeys.myRequests(page),
    queryFn: () => listMyBannerRequests(page),
    enabled: Boolean(accessToken),
    // 페이지를 넘길 때 목록이 빈 화면으로 깜빡이지 않게 이전 페이지를 잠깐 유지한다.
    placeholderData: (previous) => previous,
  })
}

/** 배너 신청. 성공하면 내 목록 캐시를 무효화한다. */
export const useCreateBannerRequest = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createBannerRequest,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: bannerKeys.myRequestsAll() })
    },
  })
}

/** 관리자 심사 목록. `status` 를 비우면 전체다. */
export const useAdminBannerRequests = (status: BannerReviewStatus | undefined, page: number) =>
  useQuery({
    queryKey: bannerKeys.adminRequests(status, page),
    queryFn: () => listAdminBannerRequests(status, page),
    placeholderData: (previous) => previous,
  })

/**
 * 이 신청과 기간이 겹치는 배너들. **펼쳤을 때만 부른다**(`enabled`).
 *
 * 목록은 이미 `hasPeriodConflict` 로 겹치는지 알려준다. 겹치는 건만 열어 보면 되므로
 * 행마다 미리 불러 둘 이유가 없다.
 */
export const useBannerConflicts = (requestId: number | null) =>
  useQuery({
    queryKey: bannerKeys.conflicts(requestId ?? 0),
    queryFn: () => listBannerConflicts(requestId as number),
    enabled: requestId !== null,
  })

/**
 * 승인·반려.
 *
 * **노출 배너 캐시(`active`)까지 무효화한다.** 승인은 그 자리에서 `ACTIVE` 배너를 만들 수
 * 있어서, 심사 화면에서 승인한 관리자가 메인 홈으로 갔을 때 방금 승인한 배너가 안 보이면
 * 승인이 안 된 것처럼 보인다.
 */
const useReviewMutation = <TVariables, TData>(
  mutationFn: (variables: TVariables) => Promise<TData>
) => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: bannerKeys.all })
    },
  })
}

export const useApproveBannerRequest = () => useReviewMutation(approveBannerRequest)
export const useRejectBannerRequest = () => useReviewMutation(rejectBannerRequest)

/** 배너 신청 취소. 성공하면 내 목록 캐시를 무효화한다. */
export const useCancelBannerRequest = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: cancelBannerRequest,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: bannerKeys.myRequestsAll() })
    },
  })
}
