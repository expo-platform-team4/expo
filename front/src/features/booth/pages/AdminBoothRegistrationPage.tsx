'use client'

import { Trash2 } from 'lucide-react'
import { useState } from 'react'

import {
  Badge,
  Button,
  Card,
  CardTitle,
  Checkbox,
  EmptyState,
  ErrorState,
  Input,
  LoadingBlock,
  PageHeader,
  Select,
} from '@/components/ui'
import { useAdminRecruitmentNotices } from '@/features/recruitment/hooks'
import { useAdminVenueHalls, useAdminVenueZones, useAdminVirtualVenues } from '@/features/venue/hooks'
import { formatCurrency } from '@/lib/currency'
import { getErrorMessage } from '@/lib/errorMessage'

import {
  useAdminBoothProducts,
  useAdminBooths,
  useCreateBoothProductsBulk,
  useCreateBoothsBulk,
} from '../hooks'

type BoothRow = {
  boothNumber: string
  shapeCode: string
  width: string
  depth: string
  sortOrder: string
}

const emptyBoothRow = (): BoothRow => ({
  boothNumber: '',
  shapeCode: 'STANDARD-3X3',
  width: '3',
  depth: '3',
  sortOrder: '',
})

/** 구역 안에 부스 공간을 여러 개 한 번에 등록하는 반복 행 폼. */
const BulkCreateBoothsSection = ({ zoneId }: { zoneId: number | null }) => {
  const [rows, setRows] = useState<BoothRow[]>([emptyBoothRow()])
  const [formError, setFormError] = useState<string | null>(null)
  const { data: existingBooths } = useAdminBooths(zoneId)
  const createMutation = useCreateBoothsBulk(zoneId ?? 0)

  const updateRow = (index: number, patch: Partial<BoothRow>) => {
    setRows((prev) => prev.map((row, i) => (i === index ? { ...row, ...patch } : row)))
  }
  const addRow = () => setRows((prev) => [...prev, emptyBoothRow()])
  const removeRow = (index: number) => setRows((prev) => prev.filter((_, i) => i !== index))

  const handleSubmit = () => {
    if (!zoneId) return
    setFormError(null)
    if (rows.some((row) => !row.boothNumber.trim() || !row.shapeCode.trim() || !row.width || !row.depth)) {
      setFormError('부스 번호·형태 코드·가로·세로는 모든 행에서 채워야 합니다.')
      return
    }
    const payload = rows.map((row) => ({
      boothNumber: row.boothNumber.trim(),
      shapeCode: row.shapeCode.trim(),
      width: Number(row.width),
      depth: Number(row.depth),
      sortOrder: row.sortOrder ? Number(row.sortOrder) : undefined,
    }))
    createMutation.mutate(payload, {
      onSuccess: () => setRows([emptyBoothRow()]),
      onError: (err) => setFormError(getErrorMessage(err)),
    })
  }

  if (!zoneId) {
    return (
      <EmptyState
        title="구역을 먼저 선택하세요"
        description="위에서 홀·구역을 골라야 부스를 등록할 수 있습니다."
      />
    )
  }

  return (
    <Card className="flex flex-col gap-4">
      <CardTitle>부스 공간 일괄 등록</CardTitle>
      {existingBooths && existingBooths.length > 0 && (
        <div>
          <p className="text-label-sm text-on-surface-variant mb-1">
            이미 등록된 부스 {existingBooths.length}개
          </p>
          <p className="text-body-md text-on-surface">
            {existingBooths.map((booth) => booth.boothNumber).join(', ')}
          </p>
        </div>
      )}

      <div className="flex flex-col gap-3">
        {rows.map((row, index) => (
          <div
            key={index}
            className="border-outline-variant grid grid-cols-2 items-end gap-2 rounded border p-3 sm:grid-cols-[1.2fr_1.2fr_0.8fr_0.8fr_0.8fr_auto]"
          >
            <Input
              label={index === 0 ? '부스 번호' : undefined}
              placeholder="A-01"
              value={row.boothNumber}
              onChange={(e) => updateRow(index, { boothNumber: e.target.value })}
            />
            <Input
              label={index === 0 ? '형태 코드' : undefined}
              placeholder="STANDARD-3X3"
              value={row.shapeCode}
              onChange={(e) => updateRow(index, { shapeCode: e.target.value })}
            />
            <Input
              label={index === 0 ? '가로(m)' : undefined}
              type="number"
              step="0.01"
              value={row.width}
              onChange={(e) => updateRow(index, { width: e.target.value })}
            />
            <Input
              label={index === 0 ? '세로(m)' : undefined}
              type="number"
              step="0.01"
              value={row.depth}
              onChange={(e) => updateRow(index, { depth: e.target.value })}
            />
            <Input
              label={index === 0 ? '정렬순서' : undefined}
              type="number"
              placeholder="선택"
              value={row.sortOrder}
              onChange={(e) => updateRow(index, { sortOrder: e.target.value })}
            />
            <Button
              type="button"
              variant="danger"
              size="sm"
              onClick={() => removeRow(index)}
              disabled={rows.length === 1}
              aria-label="이 행 삭제"
            >
              <Trash2 className="size-4" />
            </Button>
          </div>
        ))}
      </div>

      <div className="flex gap-2">
        <Button type="button" variant="secondary" size="sm" onClick={addRow}>
          행 추가
        </Button>
        <Button type="button" size="sm" loading={createMutation.isPending} onClick={handleSubmit}>
          일괄 등록
        </Button>
      </div>
      {formError && <p className="text-label-sm text-error">{formError}</p>}
    </Card>
  )
}

type ProductRowState = {
  included: boolean
  supplyPrice: string
  vatAmount: string
}

/**
 * 선택한 공고에, 선택한 구역의 부스들을 판매 상품으로 일괄 등록하는 섹션.
 * 이미 그 공고에 상품으로 등록된 부스는 목록에서 아예 뺀다 — 중복 등록을 시도할 일이 없다.
 */
const BulkCreateBoothProductsSection = ({
  noticeId,
  zoneId,
}: {
  noticeId: number | null
  zoneId: number | null
}) => {
  const [formError, setFormError] = useState<string | null>(null)
  const [rowState, setRowState] = useState<Record<number, ProductRowState>>({})
  const { data: booths, isPending: boothsPending, isError: boothsError, error: boothsErr } =
    useAdminBooths(zoneId)
  const { data: products, isPending: productsPending } = useAdminBoothProducts(noticeId)
  const createMutation = useCreateBoothProductsBulk(noticeId ?? 0)

  if (!noticeId || !zoneId) {
    return (
      <EmptyState
        title="공고와 구역을 먼저 선택하세요"
        description="위에서 공고·홀·구역을 골라야 부스 상품을 등록할 수 있습니다."
      />
    )
  }
  if (boothsPending || productsPending) {
    return <LoadingBlock label="부스·상품 정보를 불러오는 중입니다" />
  }
  if (boothsError) {
    return <ErrorState error={boothsErr} />
  }

  const registeredBoothIds = new Set((products ?? []).map((product) => product.boothId))
  const registrableBooths = booths.filter((booth) => !registeredBoothIds.has(booth.id))

  const stateFor = (boothId: number): ProductRowState =>
    rowState[boothId] ?? { included: true, supplyPrice: '', vatAmount: '0' }

  const updateRow = (boothId: number, patch: Partial<ProductRowState>) => {
    setRowState((prev) => ({ ...prev, [boothId]: { ...stateFor(boothId), ...patch } }))
  }

  const handleSubmit = () => {
    setFormError(null)
    const selected = registrableBooths.filter((booth) => stateFor(booth.id).included)
    if (selected.length === 0) {
      setFormError('등록할 부스를 하나 이상 선택하세요.')
      return
    }
    if (selected.some((booth) => !stateFor(booth.id).supplyPrice)) {
      setFormError('선택한 부스는 공급가를 모두 입력해야 합니다.')
      return
    }
    const payload = selected.map((booth) => ({
      recruitmentNoticeId: noticeId,
      boothId: booth.id,
      supplyPrice: Number(stateFor(booth.id).supplyPrice),
      vatAmount: Number(stateFor(booth.id).vatAmount || '0'),
      vatIncluded: true,
      paymentEnabled: true,
    }))
    createMutation.mutate(payload, {
      onSuccess: () => setRowState({}),
      onError: (err) => setFormError(getErrorMessage(err)),
    })
  }

  return (
    <Card className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <CardTitle className="mb-0">부스 상품 일괄 등록</CardTitle>
        <Badge variant="neutral">이미 등록됨 {registeredBoothIds.size}개</Badge>
      </div>

      {registrableBooths.length === 0 ? (
        <EmptyState
          title="등록할 부스가 없습니다"
          description="이 구역의 부스가 이미 전부 이 공고의 판매 상품으로 등록돼 있거나, 구역에 등록된 부스가 없습니다."
        />
      ) : (
        <div className="flex flex-col gap-3">
          {registrableBooths.map((booth) => {
            const state = stateFor(booth.id)
            return (
              <div
                key={booth.id}
                className="border-outline-variant grid grid-cols-2 items-end gap-2 rounded border p-3 sm:grid-cols-[auto_1fr_1fr_1fr]"
              >
                <Checkbox
                  label={booth.boothNumber}
                  checked={state.included}
                  onChange={(e) => updateRow(booth.id, { included: e.target.checked })}
                />
                <Input
                  label="공급가(원)"
                  type="number"
                  value={state.supplyPrice}
                  onChange={(e) => updateRow(booth.id, { supplyPrice: e.target.value })}
                  disabled={!state.included}
                />
                <Input
                  label="부가세(원)"
                  type="number"
                  value={state.vatAmount}
                  onChange={(e) => updateRow(booth.id, { vatAmount: e.target.value })}
                  disabled={!state.included}
                />
                <p className="text-label-sm text-on-surface-variant">
                  총액{' '}
                  {formatCurrency(Number(state.supplyPrice || 0) + Number(state.vatAmount || 0))}
                </p>
              </div>
            )
          })}
        </div>
      )}

      {registrableBooths.length > 0 && (
        <Button type="button" size="sm" loading={createMutation.isPending} onClick={handleSubmit}>
          일괄 등록
        </Button>
      )}
      {formError && <p className="text-label-sm text-error">{formError}</p>}
    </Card>
  )
}

/**
 * `/admin/booths`. ADMIN 전용.
 *
 * 부스 공간(물리적 자리) 등록과, 그 부스를 특정 공고의 판매 상품으로 등록하는 것은 생명주기가
 * 다르다 — 부스 공간은 여러 공고에 걸쳐 재사용될 수 있고, 판매 상품은 "이 공고에 이 가격으로"
 * 라는 한 번의 스냅샷이다. 그래서 한 행에 섞지 않고 별도 섹션 두 개로 나눴다.
 */
const AdminBoothRegistrationPage = () => {
  const { data: venues, isPending: venuesPending } = useAdminVirtualVenues()
  const venueId = venues?.[0]?.id
  const { data: halls, isPending: hallsPending } = useAdminVenueHalls(venueId)
  const [hallId, setHallId] = useState<number | null>(null)
  const { data: zones, isPending: zonesPending } = useAdminVenueZones(hallId ?? undefined)
  const [zoneId, setZoneId] = useState<number | null>(null)
  const { data: notices, isPending: noticesPending } = useAdminRecruitmentNotices()
  const [noticeId, setNoticeId] = useState<number | null>(null)

  return (
    <div>
      <PageHeader
        title="부스 등록"
        description="부스 공간을 등록하고, 특정 공고의 판매 상품으로 붙입니다."
      />

      <div className="flex flex-col gap-6">
        <Card className="flex flex-col gap-4">
          <CardTitle>홀·구역 선택</CardTitle>
          {venuesPending || hallsPending ? (
            <LoadingBlock label="홀 목록을 불러오는 중입니다" />
          ) : (
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
              <Select
                label="홀"
                value={hallId ?? ''}
                onChange={(e) => {
                  setHallId(e.target.value ? Number(e.target.value) : null)
                  setZoneId(null)
                }}
              >
                <option value="">홀을 선택하세요</option>
                {halls?.map((hall) => (
                  <option key={hall.id} value={hall.id}>
                    {hall.name}
                  </option>
                ))}
              </Select>
              <Select
                label="구역"
                value={zoneId ?? ''}
                onChange={(e) => setZoneId(e.target.value ? Number(e.target.value) : null)}
                disabled={!hallId || zonesPending}
              >
                <option value="">구역을 선택하세요</option>
                {zones?.map((zone) => (
                  <option key={zone.id} value={zone.id}>
                    {zone.name}
                  </option>
                ))}
              </Select>
              <Select
                label="공고 (부스 상품 등록용)"
                value={noticeId ?? ''}
                onChange={(e) => setNoticeId(e.target.value ? Number(e.target.value) : null)}
                disabled={noticesPending}
              >
                <option value="">공고를 선택하세요</option>
                {notices?.map((notice) => (
                  <option key={notice.id} value={notice.id}>
                    #{notice.id} {notice.title}
                  </option>
                ))}
              </Select>
            </div>
          )}
        </Card>

        <BulkCreateBoothsSection zoneId={zoneId} />
        <BulkCreateBoothProductsSection noticeId={noticeId} zoneId={zoneId} />
      </div>
    </div>
  )
}

export default AdminBoothRegistrationPage
