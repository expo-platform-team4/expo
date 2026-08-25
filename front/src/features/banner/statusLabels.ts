import type { BadgeVariant } from '@/components/ui'

import type { BannerReviewStatus } from './api'

/**
 * `BannerReviewStatus` → 한글 라벨·뱃지 색.
 *
 * `DRAFT` 와 `CANCELED` 는 지금 만들어지는 경로가 없다 — 신청은 등록과 동시에
 * `UNDER_REVIEW` 가 되고, 취소 API 가 아직 없다. 그래도 enum 에 있으므로 라벨을 둔다.
 * 빠뜨리면 나중에 그 경로가 생겼을 때 화면에 `undefined` 가 뜬다.
 */
export const BANNER_REVIEW_STATUS: Record<
  BannerReviewStatus,
  { label: string; variant: BadgeVariant }
> = {
  DRAFT: { label: '작성 중', variant: 'neutral' },
  UNDER_REVIEW: { label: '심사 대기', variant: 'info' },
  APPROVED: { label: '승인됨', variant: 'success' },
  REJECTED: { label: '반려됨', variant: 'error' },
  CANCELED: { label: '취소됨', variant: 'neutral' },
}
