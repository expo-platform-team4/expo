'use client'

import { zodResolver } from '@hookform/resolvers/zod'
import { useRouter } from 'next/navigation'
import { useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'

import { Button, Card, CardTitle, Input, LoadingBlock, Select, Textarea } from '@/components/ui'
import { getErrorMessage } from '@/lib/errorMessage'

import {
  useCreateRecruitmentNoticeRequest,
  useVenueHalls,
  useVenueZones,
  useVirtualVenues,
} from '../hooks'
import { createNoticeRequestSchema, type CreateNoticeRequestFormValues } from '../schemas'

/**
 * `/client/recruitment-notice-requests/new`. Function.md 3절 — "새 요청 작성". CLIENT 전용.
 *
 * `CreateRecruitmentNoticeRequestRequest` 를 그대로 채우는 실제 폼이다. 가상 장소 → 전시관(홀)
 * → 구역 순으로 실제 드롭다운을 연쇄시킨다(PR #110 로 공개 홀·구역 목록 API 가 생겨서 이제
 * 가능하다 — 전에는 ADMIN 전용이라 숫자 ID 직접 입력을 받았다).
 */
const ClientRecruitmentNoticeRequestFormPage = () => {
  const router = useRouter()
  const { data: virtualVenues, isPending: venuesPending } = useVirtualVenues()
  const createMutation = useCreateRecruitmentNoticeRequest()
  const [formError, setFormError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    setValue,
    control,
    formState: { errors },
  } = useForm<CreateNoticeRequestFormValues>({
    resolver: zodResolver(createNoticeRequestSchema),
    defaultValues: {
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
      requestedBoothConfig: '',
    },
  })

  const selectedVirtualVenueId = useWatch({ control, name: 'virtualVenueId' })
  const selectedHallId = useWatch({ control, name: 'venueHallId' })

  const { data: venueHalls, isPending: hallsPending } = useVenueHalls(
    selectedVirtualVenueId ? Number(selectedVirtualVenueId) : null
  )
  const { data: venueZones, isPending: zonesPending } = useVenueZones(
    selectedHallId ? Number(selectedHallId) : null
  )

  const onSubmit = (values: CreateNoticeRequestFormValues) => {
    setFormError(null)
    createMutation.mutate(
      {
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
        requestedBoothConfig: values.requestedBoothConfig || undefined,
      },
      {
        onSuccess: (result) => router.push(`/client/recruitment-notice-requests/${result.id}`),
        onError: (error) => setFormError(getErrorMessage(error)),
      }
    )
  }

  return (
    <div className="flex justify-center">
      <Card className="w-full max-w-2xl">
        <CardTitle>공고 생성 요청</CardTitle>
        <form className="mt-4 flex flex-col gap-4" onSubmit={handleSubmit(onSubmit)} noValidate>
          <Input label="제목" error={errors.title?.message} {...register('title')} />
          <Textarea label="설명" error={errors.description?.message} {...register('description')} />

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Input
              label="신청 시작 일시"
              type="datetime-local"
              error={errors.applicationStartAt?.message}
              {...register('applicationStartAt')}
            />
            <Input
              label="신청 종료 일시"
              type="datetime-local"
              error={errors.applicationEndAt?.message}
              {...register('applicationEndAt')}
            />
            <Input
              label="행사 시작 일시"
              type="datetime-local"
              error={errors.eventStartAt?.message}
              {...register('eventStartAt')}
            />
            <Input
              label="행사 종료 일시"
              type="datetime-local"
              error={errors.eventEndAt?.message}
              {...register('eventEndAt')}
            />
          </div>

          {venuesPending ? (
            <LoadingBlock label="가상 장소를 불러오는 중입니다" />
          ) : (
            <Select
              label="희망 가상 장소"
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
            disabled={!selectedVirtualVenueId}
            hint={!selectedVirtualVenueId ? '먼저 가상 장소를 선택해 주세요.' : undefined}
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
          <Input
            label="목표 참가 기업 수"
            inputMode="numeric"
            error={errors.targetCompanyCount?.message}
            {...register('targetCompanyCount')}
          />
          <Textarea
            label="희망 부스 구성"
            hint="JSON 문자열 형식(선택 항목)입니다."
            error={errors.requestedBoothConfig?.message}
            {...register('requestedBoothConfig')}
          />

          {formError && <p className="text-label-sm text-error">{formError}</p>}

          <Button type="submit" size="lg" loading={createMutation.isPending}>
            요청 제출
          </Button>
        </form>
      </Card>
    </div>
  )
}

export default ClientRecruitmentNoticeRequestFormPage
