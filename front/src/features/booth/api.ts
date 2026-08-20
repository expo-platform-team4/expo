import { isAxiosError } from 'axios'

import { api } from '@/lib/api'

type ApiEnvelope<T> = { success: boolean; data: T; message: string | null }

/** `com.expo.booth.entity.BoothAllocationStatus`. */
export type BoothAllocationStatus = 'ASSIGNED' | 'CANCELED' | 'REASSIGNED'

/** `com.expo.booth.entity.BoothContentStatus`. */
export type BoothContentStatus =
  'DRAFT' | 'UNDER_REVIEW' | 'PUBLISHED' | 'CORRECTION_REQUESTED' | 'HIDDEN'

/**
 * 확정 배정된 내 부스 한 행. `com.expo.booth.dto.ClientDashboardBoothResponse` 와 대응한다.
 *
 * `GET /api/client/me/booths` 가 준다 (`ClientBoothDashboardController`). `v_client_dashboard_booths`
 * 뷰를 `boothAllocationId != null` 로 필터링해 **확정 배정 건만** 돌려준다 — 신청만 하고 아직
 * 배정되지 않은 건은 여기 나오지 않는다(그건 `/client/participations` 화면, `features/participation/*`
 * 의 몫이다. 이 화면과 그 화면의 목적이 겹치지 않게 하는 경계선이다).
 */
export type MyConfirmedBooth = {
  clientUserId: number
  recruitmentNoticeId: number
  applicationId: number
  applicationStatus: string
  boothOrderId: number | null
  boothOrderStatus: string | null
  paymentStatus: string | null
  paidAt: string | null
  boothAllocationId: number
  allocationStatus: BoothAllocationStatus
  boothNumber: string | null
}

/** `GET /api/client/me/booths` — 내 확정 배정 부스 목록. CLIENT 전용. */
export const listMyConfirmedBooths = async (): Promise<MyConfirmedBooth[]> => {
  const { data } = await api.get<ApiEnvelope<MyConfirmedBooth[]>>('/client/me/booths')
  return data.data
}

/** 부스 확정 배정 상세. `com.expo.booth.dto.BoothAllocationResponse` 와 대응한다. */
export type BoothAllocation = {
  id: number
  applicationId: number
  boothOrderId: number
  boothProductId: number
  clientUserId: number
  allocatedAt: string
  status: BoothAllocationStatus
  canceledAt: string | null
  cancelReason: string | null
  createdAt: string
  updatedAt: string
}

/** `GET /api/client/booth-allocations/{allocationId}` — 내 배정 상세(배정·취소 일시, 취소 사유). */
export const getMyBoothAllocation = async (allocationId: number): Promise<BoothAllocation> => {
  const { data } = await api.get<ApiEnvelope<BoothAllocation>>(
    `/client/booth-allocations/${allocationId}`
  )
  return data.data
}

/**
 * 부스 콘텐츠(기업·부스 소개). `com.expo.booth.dto.BoothContentResponse` 와 대응한다.
 * 첨부 파일·외부 링크(`files`/`links`)는 이 화면 범위 밖이라 뺐다.
 */
export type BoothContent = {
  id: number
  boothAllocationId: number
  clientUserId: number
  companyDisplayName: string
  title: string
  companyDescription: string | null
  boothDescription: string | null
  productDescription: string | null
  status: BoothContentStatus
  publishedAt: string | null
  correctionRequestedAt: string | null
  correctionMessage: string | null
  createdAt: string
  updatedAt: string
}

export type BoothContentFormPayload = {
  companyDisplayName: string
  title: string
  companyDescription?: string
  boothDescription?: string
  productDescription?: string
}

/** `POST /api/client/booth-contents` — 부스 콘텐츠 작성. 확정 배정(ASSIGNED) 1건당 하나만 가질 수 있다. */
export const createBoothContent = async (
  boothAllocationId: number,
  payload: BoothContentFormPayload
): Promise<BoothContent> => {
  const { data } = await api.post<ApiEnvelope<BoothContent>>('/client/booth-contents', {
    boothAllocationId,
    ...payload,
  })
  return data.data
}

/** `PUT /api/client/booth-contents/{contentId}` — 콘텐츠 본문 수정. 초안·보완 요청 상태에서만 가능하다. */
export const updateBoothContent = async (
  contentId: number,
  payload: BoothContentFormPayload
): Promise<BoothContent> => {
  const { data } = await api.put<ApiEnvelope<BoothContent>>(
    `/client/booth-contents/${contentId}`,
    payload
  )
  return data.data
}

/** `POST /api/client/booth-contents/{contentId}/submit-for-review` — 검수 요청. 관리자 승인 후 공개된다. */
export const submitBoothContentForReview = async (contentId: number): Promise<BoothContent> => {
  const { data } = await api.post<ApiEnvelope<BoothContent>>(
    `/client/booth-contents/${contentId}/submit-for-review`
  )
  return data.data
}

/**
 * `GET /api/client/booth-contents/by-allocation/{allocationId}` — 배정 ID 로 **내** 콘텐츠를
 * 상태 무관(DRAFT·UNDER_REVIEW·PUBLISHED·CORRECTION_REQUESTED·HIDDEN)하게 조회한다. 로그인 필요.
 *
 * 이슈 #111 로 추가된 엔드포인트 — 전에는 `contentId` 를 이미 알아야 조회할 수 있어, 콘텐츠를
 * 만든 뒤 새로고침하면 다시 찾을 방법이 없었다(재작성 시도는 409 `DUPLICATE_BOOTH_CONTENT`).
 * 없으면 404 → `null`. 공개(`getPublishedBoothContent`)와는 반대로 소유자 본인만 보는
 * "내 콘텐츠 원본"이라 상태를 가리지 않는다 — 방문객에게 보여줄 공개 미리보기는 여전히
 * `getPublishedBoothContent` 를 쓴다.
 */
export const getMyBoothContentByAllocation = async (
  allocationId: number
): Promise<BoothContent | null> => {
  try {
    const { data } = await api.get<ApiEnvelope<BoothContent>>(
      `/client/booth-contents/by-allocation/${allocationId}`
    )
    return data.data
  } catch (error) {
    if (isAxiosError(error) && error.response?.status === 404) {
      return null
    }
    throw error
  }
}

/** 공개(PUBLISHED)된 부스 콘텐츠. `com.expo.booth.dto.PublicBoothContentResponse` 와 대응한다. */
export type PublishedBoothContent = {
  id: number
  boothAllocationId: number
  companyDisplayName: string
  title: string
  companyDescription: string | null
  boothDescription: string | null
  productDescription: string | null
  publishedAt: string
}

/**
 * `GET /api/public/booth-contents/by-allocation/{allocationId}` — 배정 ID 로 공개된 콘텐츠를 조회한다.
 * 로그인이 필요 없다. **PUBLISHED 상태만** 돌려준다 — 방문객에게 보여줄 공개 미리보기용이다.
 * 소유자 본인이 상태 무관하게 자기 콘텐츠를 다시 찾을 때는 `getMyBoothContentByAllocation` 을 쓴다.
 *
 * 없으면(아직 공개 전) 404 → `null` 로 다룬다. 화면이 이걸 "에러"가 아니라 "콘텐츠 없음"
 * 상태로 렌더링해야 하기 때문이다.
 */
export const getPublishedBoothContent = async (
  allocationId: number
): Promise<PublishedBoothContent | null> => {
  try {
    const { data } = await api.get<ApiEnvelope<PublishedBoothContent>>(
      `/public/booth-contents/by-allocation/${allocationId}`
    )
    return data.data
  } catch (error) {
    if (isAxiosError(error) && error.response?.status === 404) {
      return null
    }
    throw error
  }
}
