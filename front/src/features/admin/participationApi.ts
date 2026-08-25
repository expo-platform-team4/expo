import { api } from '@/lib/api'

import type { ParticipationApplicationStatus } from '@/features/participation/api'

export type { ParticipationApplicationStatus } from '@/features/participation/api'

type ApiEnvelope<T> = { success: boolean; data: T; message: string | null }

/** `com.expo.participation.entity.ApplicationOperationActionType`. */
export type ApplicationOperationActionType =
  'CHECKED' | 'CORRECTION_REQUESTED' | 'CORRECTION_COMPLETED' | 'MEMO_UPDATED'

/** 참여 신청 운영 이력. `com.expo.participation.dto.ApplicationOperationHistoryResponse` 와 대응한다. */
export type ApplicationOperationHistory = {
  id: number
  applicationId: number
  actionType: ApplicationOperationActionType
  message: string | null
  processedByAdminId: number | null
  createdAt: string
}

/**
 * 관리자용 참여 신청서. `com.expo.participation.dto.AdminParticipationApplicationResponse` 와
 * 대응한다.
 *
 * 보완 요청·완료는 이 응답의 필드가 아니라 이력(`ApplicationOperationHistory`)으로만 남는다 —
 * 부스 콘텐츠처럼 `status` 에 별도 상태가 있는 게 아니다. "지금 보완 대기 중인가"는 이력 중
 * 가장 최근 보완 관련 이벤트를 봐야 안다(백엔드도 `completeCorrection` 에서 그렇게 판단한다).
 */
export type AdminParticipationApplication = {
  id: number
  recruitmentNoticeId: number
  clientUserId: number
  companyNameSnapshot: string
  participationPurpose: string | null
  exhibitDescription: string | null
  selectedBoothProductId: number | null
  boothOrderId: number | null
  status: ParticipationApplicationStatus
  submittedAt: string | null
  adminCheckedAt: string | null
  adminCheckedBy: number | null
  adminMemo: string | null
  createdAt: string
  updatedAt: string
}

/** `GET /api/admin/participation-applications` — 참여 신청서 목록(전체 상태). ADMIN 전용. */
export const listAdminParticipationApplications = async (
  recruitmentNoticeId?: number
): Promise<AdminParticipationApplication[]> => {
  const { data } = await api.get<ApiEnvelope<AdminParticipationApplication[]>>(
    '/admin/participation-applications',
    { params: recruitmentNoticeId ? { recruitmentNoticeId } : undefined }
  )
  return data.data
}

/** `POST /api/admin/participation-applications/{applicationId}/check` — 운영 확인. */
export const checkParticipationApplication = async (
  applicationId: number,
  message?: string
): Promise<AdminParticipationApplication> => {
  const { data } = await api.post<ApiEnvelope<AdminParticipationApplication>>(
    `/admin/participation-applications/${applicationId}/check`,
    message ? { message } : undefined
  )
  return data.data
}

/** `POST /api/admin/participation-applications/{applicationId}/correction-request` — 보완 요청. */
export const requestParticipationApplicationCorrection = async (
  applicationId: number,
  message: string
): Promise<AdminParticipationApplication> => {
  const { data } = await api.post<ApiEnvelope<AdminParticipationApplication>>(
    `/admin/participation-applications/${applicationId}/correction-request`,
    { message }
  )
  return data.data
}

/**
 * `POST /api/admin/participation-applications/{applicationId}/correction-complete` — 보완 완료
 * 처리. 가장 최근 보완 관련 이력이 "보완 요청"이어야 한다 — 아니면 백엔드가 거절한다.
 */
export const completeParticipationApplicationCorrection = async (
  applicationId: number,
  message?: string
): Promise<AdminParticipationApplication> => {
  const { data } = await api.post<ApiEnvelope<AdminParticipationApplication>>(
    `/admin/participation-applications/${applicationId}/correction-complete`,
    message ? { message } : undefined
  )
  return data.data
}

/** `PATCH /api/admin/participation-applications/{applicationId}/memo` — 관리자 메모 갱신. */
export const updateParticipationApplicationMemo = async (
  applicationId: number,
  memo: string
): Promise<AdminParticipationApplication> => {
  const { data } = await api.patch<ApiEnvelope<AdminParticipationApplication>>(
    `/admin/participation-applications/${applicationId}/memo`,
    { memo }
  )
  return data.data
}

/** `GET /api/admin/participation-applications/{applicationId}/histories` — 운영 이력 조회. */
export const listParticipationApplicationHistory = async (
  applicationId: number
): Promise<ApplicationOperationHistory[]> => {
  const { data } = await api.get<ApiEnvelope<ApplicationOperationHistory[]>>(
    `/admin/participation-applications/${applicationId}/histories`
  )
  return data.data
}
