'use client'

import { zodResolver } from '@hookform/resolvers/zod'
import { useRouter } from 'next/navigation'
import { useEffect, useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'

import { Button, Card, CardTitle, Input, LoadingBlock, Select, Textarea } from '@/components/ui'
import { useExpoCards } from '@/features/expo/hooks'
import { useVenueHalls, useVenueZones, useVirtualVenues } from '@/features/recruitment/hooks'
import { getErrorMessage } from '@/lib/errorMessage'
import { toDatetimeLocalValue } from '@/lib/date'

import { useAdminExpoOpeningRequests } from '../expoOpeningHooks'
import { useCreateAdminNoticeRequest } from '../recruitmentHooks'
import { createAdminNoticeRequestSchema, type CreateAdminNoticeRequestFormValues } from '../schemas'

/** 모집공고 신청 기간은 행사 일정 기준으로 고정한다 — 신청 시작은 행사 시작 4주 전, 종료는 행사 종료 2주 전. */
const APPLICATION_START_OFFSET_DAYS = 28
const APPLICATION_END_OFFSET_DAYS = 14

/** ISO 시각에서 캘린더 일수를 뺀다(시·분은 그대로 유지). */
const subtractDays = (isoInstant: string, days: number): string => {
  const date = new Date(isoInstant)
  date.setDate(date.getDate() - days)
  return date.toISOString()
}

/**
 * `/admin/recruitment-notice-requests/new`. 승인된 박람회를 골라 관리자가 대신 모집공고
 * 생성 요청을 작성한다 — 예전엔 CLIENT가 직접 썼지만(구 `ClientRecruitmentNoticeRequestFormPage`),
 * 결정된 서비스 흐름(클라이언트는 박람회 개최 신청만, 나머지는 관리자 담당)에 맞춰 옮겨왔다.
 *
 * `hostClientId`는 폼에서 안 받는다 — 서버가 `expoId`로 조회한 박람회 소유주를 그대로 쓴다.
 * 가상 장소 → 전시관(홀) → 구역 연쇄 선택은 기존 클라이언트 폼과 같은 패턴이다.
 */
const AdminRecruitmentNoticeRequestFormPage = () => {
  const router = useRouter()
  const { data: expos, isPending: exposPending } = useExpoCards()
  const { data: virtualVenues, isPending: venuesPending } = useVirtualVenues()
  const { data: approvedOpeningRequests } = useAdminExpoOpeningRequests('APPROVED')
  const createMutation = useCreateAdminNoticeRequest()
  const [formError, setFormError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    setValue,
    control,
    formState: { errors },
  } = useForm<CreateAdminNoticeRequestFormValues>({
    resolver: zodResolver(createAdminNoticeRequestSchema),
    defaultValues: {
      expoId: '',
      title: '',
      description: '',
      applicationStartAt: '',
      applicationEndAt: '',
      eventStartAt: '',
      eventEndAt: '',
      virtualVenueId: '',
      venueHallId: '',
      venueZoneIds: [],
      targetCompanyCount: '',
    },
  })

  const selectedExpoId = useWatch({ control, name: 'expoId' })
  const selectedVirtualVenueId = useWatch({ control, name: 'virtualVenueId' })
  const selectedHallId = useWatch({ control, name: 'venueHallId' })

  const { data: venueHalls, isPending: hallsPending } = useVenueHalls(
    selectedVirtualVenueId ? Number(selectedVirtualVenueId) : null
  )
  const { data: venueZones, isPending: zonesPending } = useVenueZones(
    selectedHallId ? Number(selectedHallId) : null
  )

  const openingRequest = approvedOpeningRequests?.find(
    (request) => request.createdExpoId === Number(selectedExpoId)
  )
  const eventScheduleLocked = Boolean(openingRequest)
  // 홀·구역은 박람회 신청 때 안 정했을 수 있다(선택 항목) — 그때는 잠그지 않고 admin이
  // 직접 고르게 둔다. 값이 있을 때만 잠근다.
  const hallLocked = Boolean(openingRequest?.desiredVenueHallId)
  const zoneLocked = Boolean(openingRequest?.desiredVenueZoneId)
  const lockedZone = zoneLocked
    ? venueZones?.find((zone) => zone.id === openingRequest?.desiredVenueZoneId)
    : undefined

  useEffect(() => {
    if (!openingRequest) return
    setValue('eventStartAt', toDatetimeLocalValue(openingRequest.eventStartAt))
    setValue('eventEndAt', toDatetimeLocalValue(openingRequest.eventEndAt))
    setValue(
      'applicationStartAt',
      toDatetimeLocalValue(subtractDays(openingRequest.eventStartAt, APPLICATION_START_OFFSET_DAYS))
    )
    setValue(
      'applicationEndAt',
      toDatetimeLocalValue(subtractDays(openingRequest.eventEndAt, APPLICATION_END_OFFSET_DAYS))
    )
    setValue('virtualVenueId', String(openingRequest.desiredVenueId))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [openingRequest?.id])

  // 홀 선택지는 virtualVenueId 를 고른 뒤 별도 쿼리로 늦게 들어온다 — <option> 이 실제로
  // DOM 에 있어야 select 값이 반영되므로, 목록이 뜨고 나서(그리고 실제로 그 홀이 있을 때만)
  // 채운다. 로딩 중에 바로 setValue 하면 <option value="2"> 가 아직 없어 조용히 무시된다.
  useEffect(() => {
    if (!openingRequest?.desiredVenueHallId || hallsPending) return
    if (!venueHalls?.some((hall) => hall.id === openingRequest.desiredVenueHallId)) return
    setValue('venueHallId', String(openingRequest.desiredVenueHallId))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [openingRequest?.id, hallsPending, venueHalls])

  // 구역도 같은 이유로 홀 선택 후 목록이 뜬 다음에 채운다.
  useEffect(() => {
    if (!openingRequest?.desiredVenueZoneId || zonesPending) return
    if (!venueZones?.some((zone) => zone.id === openingRequest.desiredVenueZoneId)) return
    setValue('venueZoneIds', [String(openingRequest.desiredVenueZoneId)])
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [openingRequest?.id, zonesPending, venueZones])

  const onSubmit = (values: CreateAdminNoticeRequestFormValues) => {
    setFormError(null)
    createMutation.mutate(
      {
        expoId: Number(values.expoId),
        title: values.title,
        description: values.description,
        applicationStartAt: new Date(values.applicationStartAt).toISOString(),
        applicationEndAt: new Date(values.applicationEndAt).toISOString(),
        eventStartAt: new Date(values.eventStartAt).toISOString(),
        eventEndAt: new Date(values.eventEndAt).toISOString(),
        virtualVenueId: Number(values.virtualVenueId),
        venueHallId: Number(values.venueHallId),
        venueZoneIds: values.venueZoneIds.map(Number),
        targetCompanyCount: Number(values.targetCompanyCount),
      },
      {
        onSuccess: () => router.push('/admin/recruitment-notice-requests'),
        onError: (error) => setFormError(getErrorMessage(error)),
      }
    )
  }

  return (
    <div className="flex justify-center">
      <Card className="w-full max-w-2xl">
        <CardTitle>모집공고 생성 요청 작성</CardTitle>
        <form className="mt-4 flex flex-col gap-4" onSubmit={handleSubmit(onSubmit)} noValidate>
          {exposPending ? (
            <LoadingBlock label="박람회 목록을 불러오는 중입니다" />
          ) : (
            <Select label="대상 박람회" error={errors.expoId?.message} {...register('expoId')}>
              <option value="">선택해 주세요</option>
              {expos?.map((expo) => (
                <option key={expo.expoId} value={expo.expoId}>
                  #{expo.expoId} · {expo.title}
                </option>
              ))}
            </Select>
          )}

          <Input label="제목" error={errors.title?.message} {...register('title')} />
          <Textarea label="설명" error={errors.description?.message} {...register('description')} />

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Input
              label="신청 시작 일시"
              type="datetime-local"
              readOnly={eventScheduleLocked}
              className={eventScheduleLocked ? 'bg-surface-container-low' : undefined}
              hint={eventScheduleLocked ? '행사 시작일 4주 전으로 고정됩니다.' : undefined}
              error={errors.applicationStartAt?.message}
              {...register('applicationStartAt')}
            />
            <Input
              label="신청 종료 일시"
              type="datetime-local"
              readOnly={eventScheduleLocked}
              className={eventScheduleLocked ? 'bg-surface-container-low' : undefined}
              hint={eventScheduleLocked ? '행사 종료일 2주 전으로 고정됩니다.' : undefined}
              error={errors.applicationEndAt?.message}
              {...register('applicationEndAt')}
            />
            <Input
              label="행사 시작 일시"
              type="datetime-local"
              readOnly={eventScheduleLocked}
              className={eventScheduleLocked ? 'bg-surface-container-low' : undefined}
              hint={eventScheduleLocked ? '박람회 신청 때 정한 일정입니다.' : undefined}
              error={errors.eventStartAt?.message}
              {...register('eventStartAt')}
            />
            <Input
              label="행사 종료 일시"
              type="datetime-local"
              readOnly={eventScheduleLocked}
              className={eventScheduleLocked ? 'bg-surface-container-low' : undefined}
              hint={eventScheduleLocked ? '박람회 신청 때 정한 일정입니다.' : undefined}
              error={errors.eventEndAt?.message}
              {...register('eventEndAt')}
            />
          </div>

          {venuesPending ? (
            <LoadingBlock label="가상 장소를 불러오는 중입니다" />
          ) : (
            <Select
              label="희망 가상 장소"
              disabled={eventScheduleLocked}
              hint={eventScheduleLocked ? '박람회 신청 때 정한 장소입니다.' : undefined}
              error={errors.virtualVenueId?.message}
              {...register('virtualVenueId', {
                onChange: () => {
                  setValue('venueHallId', '')
                  setValue('venueZoneIds', [])
                },
              })}
            >
              <option value="">선택해 주세요</option>
              {virtualVenues?.map((venue) => (
                <option key={venue.id} value={venue.id}>
                  {venue.name}
                  {venue.address ? ` · ${venue.address}` : ''}
                </option>
              ))}
            </Select>
          )}

          <Select
            label="희망 전시관(홀)"
            disabled={hallLocked || !selectedVirtualVenueId}
            hint={
              hallLocked
                ? '박람회 신청 때 정한 전시관입니다.'
                : !selectedVirtualVenueId
                  ? '먼저 가상 장소를 선택해 주세요.'
                  : undefined
            }
            error={errors.venueHallId?.message}
            {...register('venueHallId', {
              onChange: () => setValue('venueZoneIds', []),
            })}
          >
            <option value="">
              {hallsPending && selectedVirtualVenueId ? '불러오는 중…' : '선택해 주세요'}
            </option>
            {venueHalls?.map((hall) => (
              <option key={hall.id} value={hall.id}>
                {hall.name}
              </option>
            ))}
          </Select>

          {zoneLocked ? (
            <div className="flex flex-col gap-1.5">
              <label className="text-label-md text-on-surface-variant font-medium">희망 구역</label>
              <div className="border-outline-variant bg-surface-container-low text-body-md flex h-11 items-center rounded border px-3">
                {lockedZone
                  ? `${lockedZone.name} · 최대 ${lockedZone.maxBoothCount}부스`
                  : '불러오는 중…'}
              </div>
              <p className="text-label-sm text-on-surface-variant">
                박람회 신청 때 정한 구역입니다.
              </p>
            </div>
          ) : (
            <Select
              label="희망 구역 (여러 개 선택 가능)"
              multiple
              size={4}
              disabled={!selectedHallId}
              hint={
                !selectedHallId
                  ? '먼저 전시관(홀)을 선택해 주세요.'
                  : 'Ctrl(⌘) 클릭으로 여러 구역을 고를 수 있습니다.'
              }
              error={errors.venueZoneIds?.message}
              {...register('venueZoneIds')}
            >
              {zonesPending && selectedHallId ? (
                <option disabled>불러오는 중…</option>
              ) : (
                venueZones?.map((zone) => (
                  <option key={zone.id} value={zone.id}>
                    {zone.name} · 최대 {zone.maxBoothCount}부스
                  </option>
                ))
              )}
            </Select>
          )}
          <Input
            label="목표 참가 기업 수"
            inputMode="numeric"
            error={errors.targetCompanyCount?.message}
            {...register('targetCompanyCount')}
          />
          {formError && <p className="text-label-sm text-error">{formError}</p>}

          <Button type="submit" size="lg" loading={createMutation.isPending}>
            요청 작성
          </Button>
        </form>
      </Card>
    </div>
  )
}

export default AdminRecruitmentNoticeRequestFormPage
