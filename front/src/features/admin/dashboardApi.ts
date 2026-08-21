import { api } from '@/lib/api'

type ApiEnvelope<T> = { success: boolean; data: T; message: string | null }

/** `AdminDashboardCountsResponse` 와 짝이다. `v_admin_dashboard_counts` 뷰 기반, 항상 단일 행. */
export type AdminDashboardCounts = {
  pendingExpoReviewCount: number
  pendingBannerReviewCount: number
  pendingNoticeRequestCount: number
  venueConflictCount: number
  uncheckedApplicationCount: number
  pendingChangeRequestCount: number
  pendingCancellationRequestCount: number
  settlementWaitingCount: number
}

/** `GET /api/admin/dashboard/summary` — 처리 대기 건수 집계. */
export const getAdminDashboardSummary = async (): Promise<AdminDashboardCounts> => {
  const { data } = await api.get<ApiEnvelope<AdminDashboardCounts>>('/admin/dashboard/summary')
  return data.data
}

/** `AdminPendingReviewResponse` 와 짝이다. 박람회 개최·배너·모집공고 생성 요청 심사 대기 통합 목록. */
export type AdminPendingReview = {
  reviewTargetType: string
  targetId: number
  title: string
  requesterClientId: number
  status: string
  submittedAt: string | null
  createdAt: string
}

/** `GET /api/admin/dashboard/pending-tasks` — 심사 대기 목록 통합 조회. */
export const getAdminPendingTasks = async (): Promise<AdminPendingReview[]> => {
  const { data } = await api.get<ApiEnvelope<AdminPendingReview[]>>(
    '/admin/dashboard/pending-tasks'
  )
  return data.data
}

/** `reviewTargetType` 값 → 한글 라벨. 백엔드 열거값을 그대로 옮긴다. */
export const REVIEW_TARGET_TYPE_LABEL: Record<string, string> = {
  EXPO_OPENING: '박람회 개최 신청',
  BANNER: '배너 신청',
  RECRUITMENT_NOTICE_REQUEST: '모집공고 생성 요청',
}
