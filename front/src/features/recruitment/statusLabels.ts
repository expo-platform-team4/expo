import type { BadgeVariant } from '@/components/ui'

import type {
  RecruitmentNoticeRequestStatus,
  RecruitmentNoticeStatus,
  VenueConflictStatus,
  VenueDecision,
} from './api'

/** `RecruitmentNoticeStatus` → 한글 라벨·뱃지 색. */
export const NOTICE_STATUS: Record<
  RecruitmentNoticeStatus,
  { label: string; variant: BadgeVariant }
> = {
  DRAFT: { label: '작성 중', variant: 'neutral' },
  SCHEDULED: { label: '게시 예정', variant: 'info' },
  OPEN: { label: '모집 중', variant: 'success' },
  CLOSED: { label: '모집 마감', variant: 'neutral' },
  CANCELED: { label: '취소됨', variant: 'error' },
  ARCHIVED: { label: '보관됨', variant: 'neutral' },
}

/** `RecruitmentNoticeRequestStatus` → 한글 라벨·뱃지 색. */
export const NOTICE_REQUEST_STATUS: Record<
  RecruitmentNoticeRequestStatus,
  { label: string; variant: BadgeVariant }
> = {
  DRAFT: { label: '임시저장', variant: 'neutral' },
  SUBMITTED: { label: '제출됨', variant: 'info' },
  UNDER_REVIEW: { label: '검토 중', variant: 'info' },
  APPROVED: { label: '승인됨', variant: 'success' },
  REJECTED: { label: '반려됨', variant: 'error' },
  CANCELED: { label: '취소됨', variant: 'error' },
}

/** `VenueConflictStatus` → 한글 라벨·뱃지 색. */
export const VENUE_CONFLICT_STATUS: Record<
  VenueConflictStatus,
  { label: string; variant: BadgeVariant }
> = {
  CLEAR: { label: '충돌 없음', variant: 'success' },
  CONFLICT_PENDING: { label: '충돌 검토 중', variant: 'info' },
  RESOLVED: { label: '충돌 해결됨', variant: 'success' },
}

/** `VenueDecision` → 한글 라벨·뱃지 색. */
export const VENUE_DECISION: Record<VenueDecision, { label: string; variant: BadgeVariant }> = {
  PENDING: { label: '결정 대기', variant: 'neutral' },
  ALLOWED: { label: '장소 허용', variant: 'success' },
  CANCELED: { label: '장소 취소', variant: 'error' },
}
