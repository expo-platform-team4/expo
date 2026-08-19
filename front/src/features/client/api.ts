import { api } from '@/lib/api'

type ApiEnvelope<T> = { success: boolean; data: T; message: string | null }

export type ClientDashboardProfile = {
  clientUserId: number
  nickname: string
  companyName: string
  profileImageFileId: number | null
  profileImageStorageKey: string | null
  profileImageUpdatedAt: string | null
}

/** `GET /api/client/me/dashboard` — 클라이언트 마이페이지 상단 프로필(E-API-001). */
export const getClientDashboardProfile = async (): Promise<ClientDashboardProfile> => {
  const { data } = await api.get<ApiEnvelope<ClientDashboardProfile>>('/client/me/dashboard')
  return data.data
}
