import { api } from '@/lib/api'

/** 백엔드 공통 응답 봉투. */
type ApiEnvelope<T> = { success: boolean; data: T; message?: string }

/**
 * 지금 노출 중인 메인 배너 한 건. 백엔드 `ActiveBannerResponse` 와 짝이다.
 *
 * **이미지 URL 이 아니라 파일 ID 를 준다.** 화면은 `/api/files/{id}/content` 로 받아 그린다 —
 * 프로필 이미지와 같은 방식이다.
 */
export type ActiveBanner = {
  id: number
  /** 홍보 대상 박람회. 배너를 누르면 이 박람회 상세로 간다 */
  expoId: number
  imageFileId: number
  headline: string | null
  /** 노출 순서. 백엔드가 이미 이 순서로 정렬해 준다 */
  sortOrder: number
}

/**
 * 지금 노출 가능한 메인 배너. `GET /api/banners/active`.
 *
 * **로그인이 필요 없다.** 공개 화면에서 부르는 것이라 `SecurityConfig` 에 이 경로만
 * `permitAll` 로 열려 있다.
 *
 * 슬롯 정원(현재 5)만큼 잘라서, 노출 순서대로 온다. 정렬·개수 제한은 서버가 한다.
 */
export const fetchActiveBanners = async (): Promise<ActiveBanner[]> => {
  const { data } = await api.get<ApiEnvelope<ActiveBanner[]>>('/banners/active')
  return data.data
}

/** 배너 이미지 경로. Next 이미지 최적화를 타지 않는 백엔드 프록시 경로다. */
export const bannerImageUrl = (imageFileId: number): string => `/api/files/${imageFileId}/content`
