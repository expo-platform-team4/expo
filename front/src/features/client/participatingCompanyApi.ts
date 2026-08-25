import { api } from '@/lib/api'

type ApiEnvelope<T> = { success: boolean; data: T; message: string | null }

/** 백엔드 `ParticipationApplicationStatus` 와 값이 같다. */
export type ParticipationApplicationStatus =
  'DRAFT' | 'PAYMENT_PENDING' | 'SUBMITTED' | 'PAYMENT_FAILED' | 'CANCELED'

export const PARTICIPATION_APPLICATION_STATUS_LABEL: Record<
  ParticipationApplicationStatus,
  string
> = {
  DRAFT: '작성 중',
  PAYMENT_PENDING: '결제 대기',
  SUBMITTED: '참여 확정',
  PAYMENT_FAILED: '결제 실패',
  CANCELED: '취소됨',
}

/**
 * 주최사가 보는 참여 기업 한 건. 백엔드 `ClientParticipatingCompanyResponse` 와 짝이다.
 *
 * 비어 있을 수 있는 필드는 선택 필드(`?`)다 — 백엔드가 non_null 직렬화라 값이 없으면 키가
 * 아예 빠진다(Spec.md 1-1절).
 */
export type ParticipatingCompany = {
  applicationId: number
  companyName: string
  participationPurpose?: string
  exhibitDescription?: string
  selectedBoothProductId?: number
  status: ParticipationApplicationStatus
  submittedAt?: string
  createdAt: string
}

/** `GET /api/client/expos/{expoId}/participating-companies` — 내 박람회의 참여 기업 목록. */
export const fetchParticipatingCompanies = async (
  expoId: number
): Promise<ParticipatingCompany[]> => {
  const { data } = await api.get<ApiEnvelope<ParticipatingCompany[]>>(
    `/client/expos/${expoId}/participating-companies`
  )
  return data.data
}
