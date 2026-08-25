'use client'

import { useVenueZones } from '@/features/recruitment/hooks'

import type { BoothProduct } from '../api'

/**
 * 선택한 부스 상품이 속한 홀의 배치도 미리보기. 참여 신청·수정 폼이 공용으로 쓴다.
 *
 * 부스 상품(`BoothProductResponse`)에는 배치도 파일 ID가 없다 — `venueHallId`/`venueZoneId`
 * 만 있다. 그 홀의 배치도는 공개 전시장/홀 조회(`GET /api/venue-halls/{hallId}/zones`,
 * `features/recruitment/api.ts`)로 따로 가져와야 한다.
 */
export const BoothProductLayoutPreview = ({ product }: { product: BoothProduct | undefined }) => {
  const { data: zones } = useVenueZones(product?.venueHallId ?? null)
  const zone = zones?.find((z) => z.id === product?.venueZoneId)

  if (!product || !zone?.layoutFileId) {
    return null
  }

  return (
    <div>
      <p className="text-label-sm text-on-surface-variant mb-1">{zone.name} 배치도</p>
      {/* eslint-disable-next-line @next/next/no-img-element -- 경로가 /api/files/{id}/content 라 Next 이미지 최적화 대상이 아니다. */}
      <img
        src={`/api/files/${zone.layoutFileId}/content`}
        alt={`${zone.name} 배치도`}
        className="border-outline-variant w-full rounded border object-contain"
      />
    </div>
  )
}
