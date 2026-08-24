'use client'

import { useRef, useState } from 'react'
import type { ChangeEvent } from 'react'

import {
  Badge,
  Button,
  Card,
  CardTitle,
  EmptyState,
  ErrorState,
  LoadingBlock,
  PageHeader,
} from '@/components/ui'
import { uploadFile } from '@/features/file/api'
import { getErrorMessage } from '@/lib/errorMessage'

import type { VenueHall, VenueZone, VirtualVenue } from '../api'
import {
  useAdminVenueHalls,
  useAdminVenueZones,
  useAdminVirtualVenues,
  useUpdateVenueHallLayout,
  useUpdateVenueZoneLayout,
} from '../hooks'

/**
 * 배치도 썸네일 + 업로드/교체 버튼. 홀·구역 카드에서 공용으로 쓴다 — `onUpload` 만 갈아 끼운다.
 *
 * 파일을 고르면 (1) `POST /api/files`(purpose=VENUE_LAYOUT)로 먼저 올리고 (2) 받은 fileId 를
 * `onUpload` 로 넘겨 홀/구역에 붙이는 2단계 — `ProfileEditPage` 의 프로필 이미지 교체와 같은 패턴이다.
 */
const LayoutUploader = ({
  layoutFileId,
  onUpload,
  isSaving,
}: {
  layoutFileId: number | null
  onUpload: (fileId: number) => Promise<unknown>
  isSaving: boolean
}) => {
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [previewUrl, setPreviewUrl] = useState<string | null>(null)
  const [isUploading, setIsUploading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleFileSelected = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    event.target.value = '' // 같은 파일을 다시 골라도 onChange가 뜨도록 초기화
    if (!file) return

    setError(null)
    setPreviewUrl(URL.createObjectURL(file))
    setIsUploading(true)
    try {
      const uploaded = await uploadFile(file, 'VENUE_LAYOUT')
      await onUpload(uploaded.fileId)
    } catch (err) {
      setError(getErrorMessage(err))
      setPreviewUrl(null)
    } finally {
      setIsUploading(false)
    }
  }

  const busy = isUploading || isSaving

  return (
    <div>
      <input
        ref={fileInputRef}
        type="file"
        accept="image/jpeg,image/png,image/webp"
        className="hidden"
        onChange={handleFileSelected}
      />
      <div className="bg-surface-container-high text-on-surface-variant mb-2 flex h-20 w-full items-center justify-center overflow-hidden rounded">
        {previewUrl || layoutFileId ? (
          // eslint-disable-next-line @next/next/no-img-element -- 경로가 /api/files/{id}/content 라 Next 이미지 최적화 대상이 아니다.
          <img
            src={previewUrl ?? `/api/files/${layoutFileId}/content`}
            alt="배치도"
            className="h-full w-full object-cover"
          />
        ) : (
          <span className="text-label-sm">배치도 없음</span>
        )}
      </div>
      <Button
        type="button"
        variant="secondary"
        size="sm"
        className="w-full"
        loading={busy}
        onClick={() => fileInputRef.current?.click()}
      >
        {layoutFileId ? '배치도 교체' : '배치도 업로드'}
      </Button>
      {error && <p className="text-label-sm text-error mt-1">{error}</p>}
    </div>
  )
}

/** 구역 한 칸. 홀 카드 안 그리드에 놓인다. */
const ZoneCard = ({ hallId, zone }: { hallId: number; zone: VenueZone }) => {
  const updateLayoutMutation = useUpdateVenueZoneLayout(hallId)

  return (
    <div className="border-outline-variant rounded border p-2.5">
      <LayoutUploader
        layoutFileId={zone.layoutFileId}
        isSaving={updateLayoutMutation.isPending}
        onUpload={(layoutFileId) =>
          updateLayoutMutation.mutateAsync({ zoneId: zone.id, layoutFileId })
        }
      />
      <p className="text-label-md mt-2 font-medium">{zone.name}</p>
      <p className="text-label-sm text-on-surface-variant">부스 최대 {zone.maxBoothCount}개</p>
    </div>
  )
}

/** 홀 한 건 — 배치도 + 그 아래 구역 그리드. */
const HallSection = ({ venueId, hall }: { venueId: number; hall: VenueHall }) => {
  const updateLayoutMutation = useUpdateVenueHallLayout(venueId)
  const { data: zones, isPending, isError, error, refetch } = useAdminVenueZones(hall.id)

  return (
    <Card>
      <div className="flex gap-4">
        <div className="w-36 shrink-0">
          <LayoutUploader
            layoutFileId={hall.layoutFileId}
            isSaving={updateLayoutMutation.isPending}
            onUpload={(layoutFileId) =>
              updateLayoutMutation.mutateAsync({ hallId: hall.id, layoutFileId })
            }
          />
        </div>
        <div>
          <CardTitle className="mb-1">{hall.name}</CardTitle>
          <Badge variant="neutral">{hall.hallCode}</Badge>
        </div>
      </div>

      <div className="border-outline-variant mt-4 border-t pt-4">
        <p className="text-label-sm text-on-surface-variant mb-2">구역 {zones?.length ?? '—'}개</p>
        {isPending ? (
          <LoadingBlock label="구역을 불러오는 중입니다" />
        ) : isError ? (
          <ErrorState error={error} onRetry={() => refetch()} />
        ) : zones.length === 0 ? (
          <p className="text-label-sm text-on-surface-variant">등록된 구역이 없습니다.</p>
        ) : (
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-5">
            {zones.map((zone) => (
              <ZoneCard key={zone.id} hallId={hall.id} zone={zone} />
            ))}
          </div>
        )}
      </div>
    </Card>
  )
}

/** 장소 하나 — 그 안의 홀 목록. */
const VenueHalls = ({ venue }: { venue: VirtualVenue }) => {
  const { data: halls, isPending, isError, error, refetch } = useAdminVenueHalls(venue.id)

  if (isPending) return <LoadingBlock label="홀 목록을 불러오는 중입니다" />
  if (isError) return <ErrorState error={error} onRetry={() => refetch()} />
  if (halls.length === 0) {
    return (
      <EmptyState
        title="등록된 홀이 없습니다"
        description="시드 데이터가 아직 안 들어간 환경일 수 있습니다."
      />
    )
  }

  return (
    <div className="flex flex-col gap-6">
      {halls.map((hall) => (
        <HallSection key={hall.id} venueId={venue.id} hall={hall} />
      ))}
    </div>
  )
}

/**
 * `/admin/venues`. ADMIN 전용.
 *
 * 장소·홀·구역은 킨텍스 시드 마이그레이션으로 이미 트리거 한도(장소 1개·홀 2개·구역 홀당 5개)
 * 까지 차 있어 등록 폼을 두지 않는다 — 새로 만들어도 서버가 거절한다. 이 화면이 실제로 하는
 * 일은 이미 있는 홀·구역에 배치도 이미지를 붙이거나 교체하는 것뿐이다.
 */
const AdminVenueManagementPage = () => {
  const { data: venues, isPending, isError, error, refetch } = useAdminVirtualVenues()

  return (
    <div>
      <PageHeader
        title="가상 장소 관리"
        description="홀·구역별 배치도를 등록·교체합니다. 장소·홀·구역 자체는 시드 데이터로 고정되어 있습니다."
      />

      {isPending ? (
        <LoadingBlock label="장소를 불러오는 중입니다" />
      ) : isError ? (
        <ErrorState error={error} onRetry={() => refetch()} />
      ) : venues.length === 0 ? (
        <EmptyState
          title="등록된 가상 장소가 없습니다"
          description="시드 데이터가 아직 안 들어간 환경일 수 있습니다."
        />
      ) : (
        <VenueHalls venue={venues[0]} />
      )}
    </div>
  )
}

export default AdminVenueManagementPage
