import { useMutation, useQuery } from '@tanstack/react-query'

import { useAuthStore } from '@/lib/auth'

import {
  createBoothContent,
  getMyBoothAllocation,
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
 * 공개된 콘텐츠 미리보기. 404 는 `api.ts` 가 이미 `null` 로 바꿔 주므로 여기서는 평범한
 * 성공 케이스로 다룬다(에러가 아니다 — Spec.md 5절의 "404 는 재시도해도 결과가 같다"는
 * 원칙과 달리, 이 경우 404 자체가 유효한 화면 상태다).
 */
export const usePublishedBoothContent = (allocationId: number) =>
  useQuery({
    queryKey: boothKeys.publishedContent(allocationId),
    queryFn: () => getPublishedBoothContent(allocationId),
  })

export const useCreateBoothContent = () =>
  useMutation({
    mutationFn: ({
      boothAllocationId,
      payload,
    }: {
      boothAllocationId: number
      payload: BoothContentFormPayload
    }) => createBoothContent(boothAllocationId, payload),
  })

export const useUpdateBoothContent = () =>
  useMutation({
    mutationFn: ({ contentId, payload }: { contentId: number; payload: BoothContentFormPayload }) =>
      updateBoothContent(contentId, payload),
  })

export const useSubmitBoothContentForReview = () =>
  useMutation({
    mutationFn: (contentId: number) => submitBoothContentForReview(contentId),
  })
