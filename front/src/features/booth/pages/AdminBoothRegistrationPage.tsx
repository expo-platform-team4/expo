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
import {
  useAdminVenueHalls,
  useAdminVenueZones,
  useAdminVirtualVenues,
} from '@/features/venue/hooks'
import { getErrorMessage } from '@/lib/errorMessage'

import {
  useAdminBoothProducts,
  useAdminBooths,
  useCreateBoothProductsBulk,
  useCreateBoothsBulk,
} from '../hooks'

type BoothRow = {
  boothNumber: string
}

/** 기본 부스 규격 — 화면에서는 입력받지 않고 그대로 등록에 실어 보낸다. */
const DEFAULT_SHAPE_CODE = 'STANDARD-3X3'
const DEFAULT_WIDTH = '3'
const DEFAULT_DEPTH = '3'

/** 자동 채번 시 몇 개마다 접두어(A, B, C…)를 바꿀지. */
const AUTO_PREFIX_GROUP_SIZE = 100

/** 0부터 시작하는 전체 순번을 "A-01" 형태의 부스 번호로 바꾼다 — 100개마다 접두어가 넘어간다. */
const boothNumberForIndex = (index: number) => {
  const prefix = String.fromCharCode(65 + Math.floor(index / AUTO_PREFIX_GROUP_SIZE))
  const seq = (index % AUTO_PREFIX_GROUP_SIZE) + 1
  return `${prefix}-${String(seq).padStart(2, '0')}`
}

const AUTO_BOOTH_NUMBER_PATTERN = /^([A-Z])-(\d+)$/

/**
 * "A-01" 형태의 자동 채번 부스 번호를 0부터 시작하는 전체 순번으로 되돌린다. 이 형태가
 * 아닌(수동으로 붙인) 부스 번호는 채번 기준에서 무시하도록 null 을 돌려준다.
 */
const indexForBoothNumber = (boothNumber: string): number | null => {
  const match = AUTO_BOOTH_NUMBER_PATTERN.exec(boothNumber.trim().toUpperCase())
  if (!match) return null
  const seq = Number(match[2])
  if (!Number.isInteger(seq) || seq < 1) return null
  return (match[1].charCodeAt(0) - 65) * AUTO_PREFIX_GROUP_SIZE + (seq - 1)
}

/** 홀 안에 부스 공간을 여러 개 한 번에 등록하는 반복 행 폼. */
const BulkCreateBoothsSection = ({ zoneId }: { zoneId: number | null }) => {
  const [rows, setRows] = useState<BoothRow[]>([])
  const [bulkCount, setBulkCount] = useState('10')
  const [formError, setFormError] = useState<string | null>(null)
  const [showExistingBooths, setShowExistingBooths] = useState(false)
  const { data: existingBooths } = useAdminBooths(zoneId)
  const createMutation = useCreateBoothsBulk(zoneId ?? 0)

  const updateRow = (index: number, patch: Partial<BoothRow>) => {
    setRows((prev) => prev.map((row, i) => (i === index ? { ...row, ...patch } : row)))
  }
  const removeRow = (index: number) => setRows((prev) => prev.filter((_, i) => i !== index))

  const addAutoNumberedRows = () => {
    const count = Number(bulkCount)
    if (!count || count < 1) return
    const knownIndices = [
      ...(existingBooths ?? []).map((booth) => indexForBoothNumber(booth.boothNumber)),
      ...rows.map((row) => indexForBoothNumber(row.boothNumber)),
    ]
    const maxIndex = knownIndices.reduce<number>(
      (max, index) => (index !== null && index > max ? index : max),
      -1
    )
    const base = maxIndex + 1
    const newRows = Array.from({ length: count }, (_, i) => ({
      boothNumber: boothNumberForIndex(base + i),
    }))
    setRows((prev) => [...prev, ...newRows])
  }

  const handleSubmit = () => {
    if (!zoneId) return
    setFormError(null)
    if (rows.some((row) => !row.boothNumber.trim())) {
      setFormError('부스 번호는 모든 행에서 채워야 합니다.')
      return
    }
    const payload = rows.map((row) => ({
      boothNumber: row.boothNumber.trim(),
      shapeCode: DEFAULT_SHAPE_CODE,
      width: Number(DEFAULT_WIDTH),
      depth: Number(DEFAULT_DEPTH),
    }))
    createMutation.mutate(payload, {
      onSuccess: () => setRows([]),
      onError: (err) => setFormError(getErrorMessage(err)),
    })
  }

  if (!zoneId) {
    return (
      <EmptyState
        title="공고를 먼저 선택하세요"
        description="위에서 공고를 선택해 전시장·홀을 정해야 부스를 등록할 수 있습니다."
      />
    )
  }

  return (
    <Card className="flex flex-col gap-4">
      <CardTitle>부스 공간 일괄 등록</CardTitle>
      {existingBooths && existingBooths.length > 0 && (
        <div className="flex flex-col gap-2">
          <Button
            type="button"
            variant="secondary"
            size="sm"
            className="self-start"
            onClick={() => setShowExistingBooths((prev) => !prev)}
          >
            {showExistingBooths
              ? '이미 등록된 부스 목록 접기'
              : `이미 등록된 부스 ${existingBooths.length}개 보기`}
          </Button>
          {showExistingBooths && (
            <p className="text-body-md text-on-surface border-outline-variant max-h-40 overflow-y-auto rounded border p-3">
              {existingBooths.map((booth) => booth.boothNumber).join(', ')}
            </p>
          )}
        </div>
      )}

      <div className="flex items-end gap-2">
        <Input
          label="생성 개수"
          type="number"
          min="1"
          value={bulkCount}
          onChange={(e) => setBulkCount(e.target.value)}
          className="w-32"
        />
        <Button type="button" variant="secondary" size="sm" onClick={addAutoNumberedRows}>
          자동 채번해서 추가
        </Button>
      </div>
      <p className="text-label-sm text-on-surface-variant -mt-2">
        부스 번호는 100개마다 A, B, C… 접두어가 바뀌며 자동으로 채워집니다(예: A-01 ~ A-100, B-01
        ~). 필요하면 아래에서 개별 수정할 수 있습니다.
      </p>

      {rows.length > 0 && (
        <div className="flex flex-col gap-3">
          {rows.map((row, index) => (
            <div
              key={index}
              className="border-outline-variant grid grid-cols-[1fr_auto] items-end gap-2 rounded border p-3"
            >
              <Input
                label={index === 0 ? '부스 번호' : undefined}
                placeholder="A-01"
                value={row.boothNumber}
                onChange={(e) => updateRow(index, { boothNumber: e.target.value })}
              />
              <Button
                type="button"
                variant="danger"
                size="sm"
                onClick={() => removeRow(index)}
                aria-label="이 행 삭제"
              >
                <Trash2 className="size-4" />
              </Button>
            </div>
          ))}
        </div>
      )}

      {rows.length > 0 && (
        <div className="flex gap-2">
          <Button type="button" size="sm" loading={createMutation.isPending} onClick={handleSubmit}>
            일괄 등록
          </Button>
        </div>
      )}
      {formError && <p className="text-label-sm text-error">{formError}</p>}
    </Card>
  )
}

type ProductRowState = {
  included: boolean
  supplyPrice: string
}

/** "A-01" → "A". 하이픈이 없으면 부스 번호 전체를 그룹으로 본다. */
const boothGroupPrefix = (boothNumber: string) => boothNumber.split('-')[0] || boothNumber

type GroupPriceState = {
  supplyPrice: string
}

/**
 * 선택한 공고에, 그 공고의 홀에 속한 부스들을 판매 상품으로 일괄 등록하는 섹션.
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
  const [groupPrices, setGroupPrices] = useState<Record<string, GroupPriceState>>({})
  const [showIndividualRows, setShowIndividualRows] = useState(false)
  const {
    data: booths,
    isPending: boothsPending,
    isError: boothsError,
    error: boothsErr,
  } = useAdminBooths(zoneId)
  const {
    data: products,
    isPending: productsPending,
    isError: productsError,
    error: productsErr,
  } = useAdminBoothProducts(noticeId)
  const createMutation = useCreateBoothProductsBulk(noticeId ?? 0)

  if (!noticeId || !zoneId) {
    return (
      <EmptyState
        title="공고를 먼저 선택하세요"
        description="위에서 공고를 선택해야 부스 상품을 등록할 수 있습니다."
      />
    )
  }
  if (boothsPending || productsPending) {
    return <LoadingBlock label="부스·상품 정보를 불러오는 중입니다" />
  }
  if (boothsError) {
    return <ErrorState error={boothsErr} />
  }
  if (productsError) {
    return <ErrorState error={productsErr} />
  }

  const registeredBoothIds = new Set(products.map((product) => product.boothId))
  const registrableBooths = booths.filter((booth) => !registeredBoothIds.has(booth.id))

  const stateFor = (boothId: number): ProductRowState =>
    rowState[boothId] ?? { included: true, supplyPrice: '' }

  const updateRow = (boothId: number, patch: Partial<ProductRowState>) => {
    setRowState((prev) => ({ ...prev, [boothId]: { ...stateFor(boothId), ...patch } }))
  }

  const groups = registrableBooths.reduce<Record<string, typeof registrableBooths>>(
    (acc, booth) => {
      const key = boothGroupPrefix(booth.boothNumber)
      acc[key] = [...(acc[key] ?? []), booth]
      return acc
    },
    {}
  )
  const groupKeys = Object.keys(groups).sort()

  const groupPriceFor = (key: string): GroupPriceState => groupPrices[key] ?? { supplyPrice: '' }

  const updateGroupPrice = (key: string, patch: Partial<GroupPriceState>) => {
    setGroupPrices((prev) => ({ ...prev, [key]: { ...groupPriceFor(key), ...patch } }))
  }

  const applyGroupPrice = (key: string) => {
    const { supplyPrice } = groupPriceFor(key)
    if (!supplyPrice) return
    setRowState((prev) => {
      const next = { ...prev }
      for (const booth of groups[key]) {
        next[booth.id] = { included: true, supplyPrice }
      }
      return next
    })
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
      vatAmount: 0,
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
          description="이 홀의 부스가 이미 전부 이 공고의 판매 상품으로 등록돼 있거나, 홀에 등록된 부스가 없습니다."
        />
      ) : (
        <div className="flex flex-col gap-2">
          <p className="text-label-sm text-on-surface-variant">
            그룹별 가격 일괄 적용 — 부스 번호 접두어(A, B, C…)가 같은 부스끼리 한 번에 가격을
            채웁니다.
          </p>
          {groupKeys.map((key) => {
            const price = groupPriceFor(key)
            return (
              <div
                key={key}
                className="border-outline-variant grid grid-cols-2 items-end gap-2 rounded border p-3 sm:grid-cols-[auto_1fr_auto]"
              >
                <p className="text-label-md font-medium">
                  {key}그룹 ({groups[key].length}개)
                </p>
                <Input
                  label="공급가(원)"
                  type="number"
                  value={price.supplyPrice}
                  onChange={(e) => updateGroupPrice(key, { supplyPrice: e.target.value })}
                />
                <Button
                  type="button"
                  variant="secondary"
                  size="sm"
                  onClick={() => applyGroupPrice(key)}
                >
                  그룹에 적용
                </Button>
              </div>
            )
          })}
        </div>
      )}

      {registrableBooths.length > 0 && (
        <div className="flex flex-col gap-2">
          <Button
            type="button"
            variant="secondary"
            size="sm"
            className="self-start"
            onClick={() => setShowIndividualRows((prev) => !prev)}
          >
            {showIndividualRows
              ? '개별 부스 목록 접기'
              : `개별 부스 목록 펼치기 (${registrableBooths.length}개)`}
          </Button>

          {showIndividualRows && (
            <div className="border-outline-variant max-h-[480px] overflow-y-auto rounded border">
              <table className="w-full text-left">
                <thead className="bg-surface-container-low sticky top-0">
                  <tr className="text-label-sm text-on-surface-variant">
                    <th className="px-3 py-2 font-medium">부스</th>
                    <th className="px-3 py-2 font-medium">공급가(원)</th>
                  </tr>
                </thead>
                <tbody>
                  {registrableBooths.map((booth) => {
                    const state = stateFor(booth.id)
                    return (
                      <tr key={booth.id} className="border-outline-variant border-t">
                        <td className="px-3 py-1.5">
                          <Checkbox
                            label={booth.boothNumber}
                            checked={state.included}
                            onChange={(e) => updateRow(booth.id, { included: e.target.checked })}
                          />
                        </td>
                        <td className="px-3 py-1.5">
                          <Input
                            type="number"
                            value={state.supplyPrice}
                            onChange={(e) => updateRow(booth.id, { supplyPrice: e.target.value })}
                            disabled={!state.included}
                            className="w-28"
                          />
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          )}
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
  const { data: notices, isPending: noticesPending } = useAdminRecruitmentNotices()
  const [noticeId, setNoticeId] = useState<number | null>(null)
  const notice = notices?.find((item) => item.id === noticeId)

  const { data: venues } = useAdminVirtualVenues()
  const venueId = venues?.[0]?.id
  const { data: halls, isPending: hallsPending } = useAdminVenueHalls(venueId)
  const { data: zones, isPending: zonesPending } = useAdminVenueZones(
    notice?.venueHallId ?? undefined
  )

  // 홀·전시장은 이제 별도로 고르지 않는다 — 선택한 공고에 이미 정해져 있어(공고를 만들 때
  // 박람회 신청의 희망 장소를 그대로 잠가서 씀), 여기서는 그 값을 그대로 읽어 보여주기만 한다.
  const hallId = notice?.venueHallId ?? null
  const zoneId = notice?.venueZoneIds[0] ?? null
  const hallName = halls?.find((hall) => hall.id === hallId)?.name
  const zoneName = zones?.find((zone) => zone.id === zoneId)?.name

  return (
    <div>
      <PageHeader
        title="부스 등록"
        description="부스 공간을 등록하고, 특정 공고의 판매 상품으로 붙입니다."
      />

      <div className="flex flex-col gap-6">
        <Card className="flex flex-col gap-4">
          <CardTitle>공고 선택</CardTitle>
          <Select
            label="공고 (부스 상품 등록용)"
            value={noticeId ?? ''}
            onChange={(e) => setNoticeId(e.target.value ? Number(e.target.value) : null)}
            disabled={noticesPending}
          >
            <option value="">공고를 선택하세요</option>
            {notices?.map((item) => (
              <option key={item.id} value={item.id}>
                #{item.id} {item.title}
              </option>
            ))}
          </Select>

          {noticeId && (
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <div className="flex flex-col gap-1.5">
                <label className="text-label-md text-on-surface-variant font-medium">전시장</label>
                <div className="border-outline-variant bg-surface-container-low text-body-md flex h-11 items-center rounded border px-3">
                  {hallsPending ? '불러오는 중…' : (hallName ?? '-')}
                </div>
              </div>
              <div className="flex flex-col gap-1.5">
                <label className="text-label-md text-on-surface-variant font-medium">홀</label>
                <div className="border-outline-variant bg-surface-container-low text-body-md flex h-11 items-center rounded border px-3">
                  {zonesPending ? '불러오는 중…' : (zoneName ?? '-')}
                </div>
              </div>
            </div>
          )}
        </Card>

        <BulkCreateBoothsSection key={zoneId ?? 'none'} zoneId={zoneId} />
        <BulkCreateBoothProductsSection
          key={`${zoneId ?? 'none'}-${noticeId ?? 'none'}`}
          noticeId={noticeId}
          zoneId={zoneId}
        />
      </div>
    </div>
  )
}

export default AdminBoothRegistrationPage
