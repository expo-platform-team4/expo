import { api } from '@/lib/api'

type ApiEnvelope<T> = { success: boolean; data: T; message: string | null }

/** 구매 가능한 티켓 상품 한 종류. 백엔드 `PurchasableTicketProductResponse` 와 짝이다. */
export type PurchasableTicketProduct = {
  ticketProductId: number
  name: string
  description: string
  price: number
  availableQuantity: number
  maxQuantityPerOrder: number
  salesStartAt: string
  salesEndAt: string
}

/**
 * 박람회 상세의 구매 가능 티켓 상품 목록. `GET /api/expos/{expoId}/ticket-products/purchasable`.
 *
 * 박람회 자체의 제목·소개·일정은 주지 않는다(티켓 상품 정보만 준다) — 그쪽은
 * `fetchExpoDetail` 이 맡는다. 존재하지 않는 expoId 를 넘기면 400 으로
 * "박람회를 찾을 수 없습니다" 를 준다(`ErrorCode.EXPO_NOT_FOUND`).
 */
export const fetchPurchasableTicketProducts = async (
  expoId: number
): Promise<PurchasableTicketProduct[]> => {
  const { data } = await api.get<ApiEnvelope<PurchasableTicketProduct[]>>(
    `/expos/${expoId}/ticket-products/purchasable`
  )
  return data.data
}

/** 이미지·자료 종류. 백엔드 `ExpoImageType` / `ExpoFilePurpose` 와 같은 값이다. */
export type ExpoImageType = 'THUMBNAIL' | 'DETAIL' | 'GALLERY'
export type ExpoFilePurpose = 'INTRO_PDF' | 'CATALOG' | 'LEAFLET' | 'PROMO_VIDEO' | 'OTHER'

export const EXPO_IMAGE_TYPE_LABEL: Record<ExpoImageType, string> = {
  THUMBNAIL: '대표 이미지',
  DETAIL: '상세 이미지',
  GALLERY: '갤러리',
}

export const EXPO_FILE_PURPOSE_LABEL: Record<ExpoFilePurpose, string> = {
  INTRO_PDF: '소개 자료',
  CATALOG: '카탈로그',
  LEAFLET: '리플렛',
  PROMO_VIDEO: '홍보 영상',
  OTHER: '기타',
}

/** 박람회에 붙은 이미지 한 장. */
export type ExpoImage = {
  id: number
  fileId: number
  imageType: ExpoImageType
  /** 대체 텍스트. 안 넣고 올릴 수 있어 없을 수 있다. */
  altText?: string
  sortOrder: number
  url: string
}

/** 박람회에 붙은 자료 한 건. */
export type ExpoFile = {
  id: number
  fileId: number
  filePurpose: ExpoFilePurpose
  /** 제목을 비우면 백엔드가 원본 파일명을 채워 준다. */
  title?: string
  sortOrder: number
  url: string
}

/** 노출용 판매 상태. 뷰가 판매기간·재고로 계산한 값이다. */
export type DisplaySalesStatus = 'SCHEDULED' | 'ON_SALE' | 'SOLD_OUT' | 'SALE_ENDED' | 'CANCELED'

export const SALES_STATUS_LABEL: Record<DisplaySalesStatus, string> = {
  SCHEDULED: '판매 예정',
  ON_SALE: '판매중',
  SOLD_OUT: '매진',
  SALE_ENDED: '판매 종료',
  CANCELED: '취소됨',
}

/**
 * 목록의 박람회 카드 한 장.
 *
 * `minimumPrice`·`thumbnailUrl` 은 **없을 수 있다** — 티켓 상품이 아직 없거나 대표
 * 이미지를 안 올린 박람회다. 백엔드가 `non_null` 직렬화라 null 필드는 응답에서 아예
 * 빠지므로 `| null` 이 아니라 `?` 다 (Spec.md 1-1절).
 */
export type ExpoCard = {
  expoId: number
  title: string
  eventStartAt: string
  eventEndAt: string
  regionCode: string
  minimumPrice?: number
  availableQuantity: number
  displaySalesStatus: DisplaySalesStatus
  thumbnailUrl?: string
}

/** 박람회 상세. 카드에 없는 소개글과 이미지·자료 목록이 더 붙는다. */
export type ExpoDetail = {
  expoId: number
  title: string
  description: string
  regionCode: string
  eventStartAt: string
  eventEndAt: string
  salesStartAt: string
  salesEndAt: string
  eventStatus: string
  displaySalesStatus: DisplaySalesStatus
  minimumPrice?: number
  availableQuantity: number
  images: ExpoImage[]
  files: ExpoFile[]
}

/** 목록 필터. 비우면 전체다. */
export type ExpoCardQuery = {
  regionCode?: string
  keyword?: string
  sort?: 'POPULAR'
}

/** 공개 박람회 목록. `GET /api/expos`. 공개된 박람회만 나온다. */
export const fetchExpoCards = async (query: ExpoCardQuery = {}): Promise<ExpoCard[]> => {
  const { data } = await api.get<ApiEnvelope<ExpoCard[]>>('/expos', { params: query })
  return data.data
}

/** 공개 박람회 상세. `GET /api/expos/{expoId}`. 없거나 비공개면 400 이다. */
export const fetchExpoDetail = async (expoId: number): Promise<ExpoDetail> => {
  const { data } = await api.get<ApiEnvelope<ExpoDetail>>(`/expos/${expoId}`)
  return data.data
}
