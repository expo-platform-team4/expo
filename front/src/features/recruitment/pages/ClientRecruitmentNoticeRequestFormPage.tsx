'use client'

import { zodResolver } from '@hookform/resolvers/zod'
import { useRouter } from 'next/navigation'
import { useState } from 'react'
import { useForm } from 'react-hook-form'

import { Button, Card, CardTitle, Input, LoadingBlock, Select, Textarea } from '@/components/ui'
import { getErrorMessage } from '@/lib/errorMessage'

import { useCreateRecruitmentNoticeRequest, useVirtualVenues } from '../hooks'
import { createNoticeRequestSchema, type CreateNoticeRequestFormValues } from '../schemas'

/**
 * `/client/recruitment-notice-requests/new`. Function.md 3절 — "새 요청 작성". CLIENT 전용.
 *
 * `CreateRecruitmentNoticeRequestRequest` 를 그대로 채우는 실제 폼이다. 가상 장소는
 * `GET /api/virtual-venues`(공개)로 고를 수 있지만, 그 하위 홀·구역 목록 조회는 ADMIN
 * 전용이라(`recruitment/api.ts` 의 `listVirtualVenues` 주석 참고) 전시관·구역은 숫자 ID
 * 직접 입력으로 받는다 — Function.md 7절 "API 부재를 화면에 명시한다" 원칙에 따라 힌트
 * 문구로 그 사실을 알린다.
 */
const ClientRecruitmentNoticeRequestFormPage = () => {
  const router = useRouter()
  const { data: virtualVenues, isPending: venuesPending } = useVirtualVenues()
  const createMutation = useCreateRecruitmentNoticeRequest()
  const [formError, setFormError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
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
      venueZoneIds: '',
      targetCompanyCount: '',
      requestedBoothConfig: '',
    },
  })

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
        venueZoneIds: values.venueZoneIds.split(',').map((part) => Number(part.trim())),
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
              {...register('virtualVenueId')}
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

          <Input
            label="희망 전시관(홀) ID"
            hint="홀 목록 조회는 관리자 전용 API 라 화면에서 고를 수 없습니다. 주최사 담당자에게 확인한 ID를 입력해 주세요."
            error={errors.venueHallId?.message}
            {...register('venueHallId')}
          />
          <Input
            label="희망 구역 ID (쉼표로 구분, 하나 이상)"
            placeholder="예: 12,13"
            hint="구역 목록 조회도 관리자 전용 API 라 화면에서 고를 수 없습니다."
            error={errors.venueZoneIds?.message}
            {...register('venueZoneIds')}
          />
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
