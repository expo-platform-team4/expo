'use client'

import { zodResolver } from '@hookform/resolvers/zod'
import { useRouter } from 'next/navigation'
import { useEffect, useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'

import {
  Badge,
  Button,
  Card,
  CardTitle,
  ErrorState,
  Input,
  LoadingBlock,
  PageHeader,
  Select,
  Textarea,
} from '@/components/ui'
import { useExpoCategories } from '@/features/expo/hooks'
import { useVenueHalls, useVenueZones, useVirtualVenues } from '@/features/recruitment/hooks'
import { formatDate } from '@/lib/date'
import { getErrorMessage } from '@/lib/errorMessage'

import { EXPO_OPENING_STATUS_LABEL, type ExpoOpeningRequestStatus } from '../expoOpeningApi'
import {
  useCreateExpoOpeningRequest,
  useMyExpoOpeningRequests,
  useSubmitExpoOpeningRequest,
} from '../hooks'
import { expoOpeningRequestSchema, type ExpoOpeningRequestFormValues } from '../schemas'

/** 행사 시작·종료 시각을 고정한다 — 날짜만 고르고 시간은 매번 묻지 않는다. */
const EVENT_START_TIME = '09:00:00'
const EVENT_END_TIME = '21:00:00'

/** 티켓 판매 시작·종료 시각도 고정한다 — 판매일 자정부터 그날 자정 직전까지. */
const SALES_START_TIME = '00:00:00'
const SALES_END_TIME = '23:59:59'

/** 티켓 판매 기간은 직접 고르지 않는다 — 행사 시작일 2주 전 ~ 행사 종료일 하루 전으로 자동 계산한다. */
const SALES_START_OFFSET_DAYS = -14
const SALES_END_OFFSET_DAYS = -1

/** "YYYY-MM-DD" 문자열에 날짜를 더한다(음수면 뺀다). 로컬 날짜로만 계산해 타임존 어긋남이 없다. */
const addDaysToDateString = (dateString: string, deltaDays: number): string => {
  const [year, month, day] = dateString.split('-').map(Number)
  const date = new Date(year, month - 1, day)
  date.setDate(date.getDate() + deltaDays)
  const yyyy = date.getFullYear()
  const mm = String(date.getMonth() + 1).padStart(2, '0')
  const dd = String(date.getDate()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd}`
}

const STATUS_VARIANT: Record<ExpoOpeningRequestStatus, 'success' | 'neutral' | 'error' | 'info'> = {
  DRAFT: 'neutral',
  SUBMITTED: 'info',
  UNDER_REVIEW: 'info',
  APPROVED: 'success',
  REJECTED: 'error',
  CANCELED: 'neutral',
}

/**
 * `/client/expos/new`. Function.md 3절 — "박람회 개최 신청".
 *
 * 디자인(`13484d75`)의 "임시저장"·"심사요청" 두 버튼을 그대로 살렸다 — 백엔드 상태도
 * DRAFT/SUBMITTED 로 나뉜다.
 *
 * **카테고리**는 관리자가 미리 만들어 둔 활성 카테고리 중 하나를 고르는 드롭다운이다 — 승인 시
 * `expo_categories` 로 그대로 옮겨져 박람회 검색·필터에 쓰인다. 백엔드는 목록(`categoryIds`)을
 * 받지만 폼에서는 한 개만 고르게 해서 배열에 담아 보낸다. **대표 이미지·소개 자료는 아직
 * 뺐다.** 디자인에는 있지만 파일 도메인 자체가 아직 없다(이슈 #93). 대신 디자인에 없던
 * **판매 기간**을 넣었다 — 승인 시 만들 `expos` 행이 이 값을 NOT NULL 로 요구한다. 자세한
 * 배경은 이슈 #116.
 */
const ClientExpoApplicationPage = () => {
  const router = useRouter()
  const [formError, setFormError] = useState<string | null>(null)

  const { data: requests, isPending, isError, error, refetch } = useMyExpoOpeningRequests()
  const { data: venues, isPending: venuesPending } = useVirtualVenues()
  const { data: categories, isPending: categoriesPending } = useExpoCategories()
  const createMutation = useCreateExpoOpeningRequest()
  const submitMutation = useSubmitExpoOpeningRequest()

  const {
    register,
    handleSubmit,
    control,
    setValue,
    reset,
    formState: { errors },
  } = useForm<ExpoOpeningRequestFormValues>({
    resolver: zodResolver(expoOpeningRequestSchema),
    defaultValues: {
      title: '',
      description: '',
      eventStartAt: '',
      eventEndAt: '',
      salesStartAt: '',
      salesEndAt: '',
      desiredVenueId: '',
      desiredVenueHallId: '',
      desiredVenueZoneId: '',
      categoryId: '',
    },
  })

  const eventStartAt = useWatch({ control, name: 'eventStartAt' })
  const eventEndAt = useWatch({ control, name: 'eventEndAt' })
  const selectedVenueId = useWatch({ control, name: 'desiredVenueId' })

  useEffect(() => {
    if (!eventStartAt) return
    setValue('salesStartAt', addDaysToDateString(eventStartAt, SALES_START_OFFSET_DAYS), {
      shouldValidate: true,
    })
  }, [eventStartAt, setValue])

  useEffect(() => {
    if (!eventEndAt) return
    setValue('salesEndAt', addDaysToDateString(eventEndAt, SALES_END_OFFSET_DAYS), {
      shouldValidate: true,
    })
  }, [eventEndAt, setValue])
  const selectedHallId = useWatch({ control, name: 'desiredVenueHallId' })
  const { data: halls, isPending: hallsPending } = useVenueHalls(
    selectedVenueId ? Number(selectedVenueId) : null
  )
  const { data: zones, isPending: zonesPending } = useVenueZones(
    selectedHallId ? Number(selectedHallId) : null
  )

  const save = (values: ExpoOpeningRequestFormValues, submitNow: boolean) => {
    setFormError(null)
    createMutation.mutate(
      {
        submitNow,
        content: {
          title: values.title,
          description: values.description,
          eventStartAt: new Date(`${values.eventStartAt}T${EVENT_START_TIME}`).toISOString(),
          eventEndAt: new Date(`${values.eventEndAt}T${EVENT_END_TIME}`).toISOString(),
          salesStartAt: new Date(`${values.salesStartAt}T${SALES_START_TIME}`).toISOString(),
          salesEndAt: new Date(`${values.salesEndAt}T${SALES_END_TIME}`).toISOString(),
          desiredVenueId: Number(values.desiredVenueId),
          desiredVenueHallId: values.desiredVenueHallId
            ? Number(values.desiredVenueHallId)
            : undefined,
          desiredVenueZoneId: values.desiredVenueZoneId
            ? Number(values.desiredVenueZoneId)
            : undefined,
          categoryIds: values.categoryId ? [Number(values.categoryId)] : undefined,
        },
      },
      {
        onSuccess: () => reset(),
        onError: (err) => setFormError(getErrorMessage(err)),
      }
    )
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="박람회 개최 신청"
        description="신청하면 관리자 심사를 거쳐 박람회가 개설됩니다."
        action={
          <Button variant="secondary" onClick={() => router.push('/client/expos')}>
            내 박람회
          </Button>
        }
      />

      <Card>
        <CardTitle>기본 정보</CardTitle>
        <form
          className="mt-4 flex flex-col gap-4"
          onSubmit={handleSubmit((values) => save(values, true))}
          noValidate
        >
          <Input label="박람회명" error={errors.title?.message} {...register('title')} />
          <Textarea
            label="상세 소개"
            hint="행사의 목적, 주요 프로그램 등을 적어 주세요."
            error={errors.description?.message}
            {...register('description')}
          />

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Input
              label="행사 시작일"
              type="date"
              hint="오전 9시 시작으로 고정됩니다."
              error={errors.eventStartAt?.message}
              {...register('eventStartAt')}
            />
            <Input
              label="행사 종료일"
              type="date"
              hint="오후 9시 종료로 고정됩니다."
              min={eventStartAt || undefined}
              error={errors.eventEndAt?.message}
              {...register('eventEndAt')}
            />
            <Input
              label="티켓 판매 시작"
              type="date"
              readOnly
              className="bg-surface-container-low"
              hint="행사 시작일 2주 전으로 자동 계산됩니다."
              error={errors.salesStartAt?.message}
              {...register('salesStartAt')}
            />
            <Input
              label="티켓 판매 종료"
              type="date"
              readOnly
              className="bg-surface-container-low"
              hint="행사 종료일 하루 전으로 자동 계산됩니다."
              error={errors.salesEndAt?.message}
              {...register('salesEndAt')}
            />
          </div>

          {venuesPending ? (
            <LoadingBlock label="장소를 불러오는 중입니다" />
          ) : (
            <Select
              label="희망 장소"
              hint="승인 시 이 장소의 지역이 박람회 지역으로 등록됩니다."
              error={errors.desiredVenueId?.message}
              {...register('desiredVenueId', {
                onChange: () => {
                  setValue('desiredVenueHallId', '')
                  setValue('desiredVenueZoneId', '')
                },
              })}
            >
              <option value="">선택해 주세요</option>
              {venues?.map((venue) => (
                <option key={venue.id} value={venue.id}>
                  {venue.name}
                  {venue.address ? ` · ${venue.address}` : ''}
                </option>
              ))}
            </Select>
          )}

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Select
              label="희망 전시관 (선택)"
              disabled={!selectedVenueId}
              hint={!selectedVenueId ? '먼저 장소를 선택해 주세요.' : undefined}
              {...register('desiredVenueHallId', {
                onChange: () => setValue('desiredVenueZoneId', ''),
              })}
            >
              <option value="">{hallsPending && selectedVenueId ? '불러오는 중…' : '미정'}</option>
              {halls?.map((hall) => (
                <option key={hall.id} value={hall.id}>
                  {hall.name}
                </option>
              ))}
            </Select>
            <Select
              label="희망 구역 (선택)"
              disabled={!selectedHallId}
              hint={!selectedHallId ? '먼저 전시관을 선택해 주세요.' : undefined}
              {...register('desiredVenueZoneId')}
            >
              <option value="">{zonesPending && selectedHallId ? '불러오는 중…' : '미정'}</option>
              {zones?.map((zone) => (
                <option key={zone.id} value={zone.id}>
                  {zone.name}
                </option>
              ))}
            </Select>
          </div>

          {categoriesPending ? (
            <LoadingBlock label="카테고리를 불러오는 중입니다" />
          ) : (
            <Select label="카테고리 (선택)" {...register('categoryId')}>
              <option value="">미정</option>
              {categories?.map((category) => (
                <option key={category.id} value={category.id}>
                  {category.name}
                </option>
              ))}
            </Select>
          )}

          {formError && <p className="text-label-sm text-error">{formError}</p>}

          <div className="flex flex-wrap gap-2">
            <Button type="submit" size="lg" loading={createMutation.isPending}>
              심사 요청
            </Button>
            <Button
              type="button"
              variant="secondary"
              size="lg"
              loading={createMutation.isPending}
              onClick={handleSubmit((values) => save(values, false))}
            >
              임시저장
            </Button>
          </div>
        </form>
      </Card>

      <div>
        <CardTitle className="mb-3">내 신청 내역</CardTitle>
        {isError ? (
          <ErrorState error={error} onRetry={() => refetch()} />
        ) : isPending ? (
          <LoadingBlock label="신청 내역을 불러오는 중입니다" />
        ) : requests.length === 0 ? (
          <Card>
            <p className="text-body-md text-on-surface-variant">아직 신청한 박람회가 없습니다.</p>
          </Card>
        ) : (
          <div className="flex flex-col gap-3">
            {requests.map((request) => (
              <Card key={request.id}>
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <div className="min-w-0">
                    <p className="text-title-lg text-on-surface font-semibold">{request.title}</p>
                    <p className="text-body-sm text-on-surface-variant mt-1">
                      행사 {formatDate(request.eventStartAt)} ~ {formatDate(request.eventEndAt)}
                      {request.desiredVenueName ? ` · ${request.desiredVenueName}` : ''}
                    </p>
                  </div>
                  <Badge variant={STATUS_VARIANT[request.status]}>
                    {EXPO_OPENING_STATUS_LABEL[request.status]}
                  </Badge>
                </div>

                {request.status === 'REJECTED' && request.rejectionReason && (
                  <p className="bg-error-container text-on-error-container text-body-md mt-3 rounded px-3 py-2">
                    반려 사유: {request.rejectionReason}
                  </p>
                )}

                <div className="mt-3 flex flex-wrap gap-2">
                  {request.status === 'DRAFT' && (
                    <Button
                      type="button"
                      size="sm"
                      loading={submitMutation.isPending}
                      onClick={() => submitMutation.mutate(request.id)}
                    >
                      심사 요청
                    </Button>
                  )}
                  {request.status === 'APPROVED' && request.createdExpoId && (
                    <Button
                      type="button"
                      variant="secondary"
                      size="sm"
                      onClick={() => router.push('/client/expos')}
                    >
                      개설된 박람회 보기
                    </Button>
                  )}
                </div>
              </Card>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

export default ClientExpoApplicationPage
