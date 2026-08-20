import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { useAuthStore } from '@/lib/auth'

import {
  createBoothContent,
  getMyBoothAllocation,
  getMyBoothContentByAllocation,
  getPublishedBoothContent,
  listMyConfirmedBooths,
  submitBoothContentForReview,
  updateBoothContent,
  type BoothContentFormPayload,
} from './api'
import { boothKeys } from './queryKeys'

/** `/client/booths` 화면 — 내 확정 배정 부스 목록. */
export const useMyConfirmedBooths = () => {
  const accessToken = useAuthStore((state) => state.accessToken)
  return useQuery({
    queryKey: boothKeys.myList(),
    queryFn: listMyConfirmedBooths,
    enabled: Boolean(accessToken),
  })
}

/** 배정 상세(배정·취소 일시, 취소 사유). 카드를 펼쳤을 때만 부른다(`enabled`). */
export const useBoothAllocation = (allocationId: number | null) => {
  const accessToken = useAuthStore((state) => state.accessToken)
  return useQuery({
    queryKey: boothKeys.allocationDetail(allocationId ?? 0),
    queryFn: () => getMyBoothAllocation(allocationId as number),
    enabled: Boolean(accessToken) && allocationId !== null,
  })
}

/**
 * 공개된 콘텐츠 미리보기(방문객용, 로그인 불필요). 404 는 `api.ts` 가 이미 `null` 로 바꿔
 * 주므로 여기서는 평범한 성공 케이스로 다룬다(에러가 아니다 — Spec.md 5절의 "404 는
 * 재시도해도 결과가 같다"는 원칙과 달리, 이 경우 404 자체가 유효한 화면 상태다).
 *
 * 부스 소유자 본인이 자기 콘텐츠를 관리할 때는 이걸 쓰지 않는다 — `useMyBoothContent`.
 */
export const usePublishedBoothContent = (allocationId: number) =>
  useQuery({
    queryKey: boothKeys.publishedContent(allocationId),
    queryFn: () => getPublishedBoothContent(allocationId),
  })

/**
 * 배정 ID 로 **내** 콘텐츠를 상태 무관하게 조회한다(이슈 #111). `BoothContentSection` 이
 * 마운트될 때마다 이걸로 기존 콘텐츠 유무를 먼저 확인한다 — 세션에서 만든 것만 기억하던
 * 이전 방식(새로고침하면 잃어버림)을 대체한다.
 */
export const useMyBoothContent = (allocationId: number) =>
  useQuery({
    queryKey: boothKeys.myContent(allocationId),
    queryFn: () => getMyBoothContentByAllocation(allocationId),
  })

export const useCreateBoothContent = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      boothAllocationId,
      payload,
    }: {
      boothAllocationId: number
      payload: BoothContentFormPayload
    }) => createBoothContent(boothAllocationId, payload),
    onSuccess: (_content, { boothAllocationId }) => {
      queryClient.invalidateQueries({ queryKey: boothKeys.myContent(boothAllocationId) })
    },
  })
}

export const useUpdateBoothContent = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      contentId,
      payload,
    }: {
      contentId: number
      allocationId: number
      payload: BoothContentFormPayload
    }) => updateBoothContent(contentId, payload),
    onSuccess: (_content, { allocationId }) => {
      queryClient.invalidateQueries({ queryKey: boothKeys.myContent(allocationId) })
    },
  })
}

export const useSubmitBoothContentForReview = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ contentId }: { contentId: number; allocationId: number }) =>
      submitBoothContentForReview(contentId),
    onSuccess: (_content, { allocationId }) => {
      queryClient.invalidateQueries({ queryKey: boothKeys.myContent(allocationId) })
    },
  })
}
