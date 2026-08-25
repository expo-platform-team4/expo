'use client'

import { zodResolver } from '@hookform/resolvers/zod'
import { useRouter } from 'next/navigation'
import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'

import { Button, Card, CardTitle, Input, LoadingBlock, Select, Textarea } from '@/components/ui'
import type { RecruitmentNoticeRequest } from '@/features/recruitment/api'
import { toDatetimeLocalValue } from '@/lib/date'
import { getErrorMessage } from '@/lib/errorMessage'

import { useAdminNoticeRequests, useCreateAdminNotice } from '../recruitmentHooks'
import { createAdminNoticeSchema, type CreateAdminNoticeFormValues } from '../schemas'

/**
 * `/admin/recruitment-notices/new`. 승인된 모집공고 생성 요청을 근거로 기업 모집 공고
 * 초안을 만든다. `CreateRecruitmentNoticeRequest`(admin dto) 를 그대로 채우는 폼이다.
 */
const AdminRecruitmentNoticeFormPage = () => {
  const router = useRouter()
  const { data: requests, isPending: requestsPending } = useAdminNoticeRequests()
  const createMutation = useCreateAdminNotice()
  const [formError, setFormError] = useState<string | null>(null)

  /**
   * 이 요청으로 초안을 만들 수 없는 이유. 만들 수 있으면 `null` 이다.
   *
   * <p>백엔드 `RecruitmentNoticeService.create()` 가 검사하는 조건과 **같아야 한다.** 어긋나면
   * 화면은 만들 수 있다고 하고 서버는 거절하는, 지금 고치는 바로 그 상태로 돌아간다.
   *
   * <p>`status === 'APPROVED'` 만 보던 것이 원래 버그였다. 그 상태는 **장소 판정의 부산물**이라
   * (장소를 허용하면 자동으로 APPROVED 가 된다), 예약을 아직 안 잡은 요청까지 통과시켰다.
   */
  const blockingReason = (request: RecruitmentNoticeRequest): string | null => {
    if (request.venueDecision !== 'ALLOWED') {
      return '장소 판정 필요'
    }
    if (request.noticeCreated) {
      return '이미 공고 생성됨'
    }
    if (!request.venueReservationConfirmed) {
      return '장소 예약 확정 필요'
    }
    return null
  }

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors },
  } = useForm<CreateAdminNoticeFormValues>({
    resolver: zodResolver(createAdminNoticeSchema),
    defaultValues: {
      requestId: '',
      title: '',
      content: '',
      eligibility: '',
      submissionRequirements: '',
      applicationStartAt: '',
      applicationEndAt: '',
    },
  })

  const selectedRequestId = watch('requestId')

  // 요청을 고르면 이미 그 요청에 있는 제목·내용·신청 기간을 그대로 가져와 채운다 — 관리자가
  // 방금 승인한 내용을 다시 타이핑할 필요가 없다.
  useEffect(() => {
    if (!selectedRequestId) return
    const request = (requests ?? []).find((r) => String(r.id) === selectedRequestId)
    if (!request) return
    setValue('title', request.title, { shouldValidate: true })
    setValue('content', request.description, { shouldValidate: true })
    setValue('applicationStartAt', toDatetimeLocalValue(request.applicationStartAt), {
      shouldValidate: true,
    })
    setValue('applicationEndAt', toDatetimeLocalValue(request.applicationEndAt), {
      shouldValidate: true,
    })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedRequestId])

  const onSubmit = (values: CreateAdminNoticeFormValues) => {
    setFormError(null)
    createMutation.mutate(
      {
        requestId: Number(values.requestId),
        title: values.title,
        content: values.content,
        eligibility: values.eligibility || undefined,
        submissionRequirements: values.submissionRequirements || undefined,
        applicationStartAt: new Date(values.applicationStartAt).toISOString(),
        applicationEndAt: new Date(values.applicationEndAt).toISOString(),
      },
      {
        onSuccess: () => router.push('/admin/recruitment-notices'),
        onError: (error) => setFormError(getErrorMessage(error)),
      }
    )
  }

  return (
    <div className="flex justify-center">
      <Card className="w-full max-w-2xl">
        <CardTitle>기업 모집 공고 작성</CardTitle>
        <form className="mt-4 flex flex-col gap-4" onSubmit={handleSubmit(onSubmit)} noValidate>
          {requestsPending ? (
            <LoadingBlock label="승인된 요청을 불러오는 중입니다" />
          ) : (
            <Select
              label="근거 모집공고 생성 요청"
              hint="장소 예약까지 확정된 요청만 고를 수 있습니다. 나머지는 이유와 함께 회색으로 보입니다."
              error={errors.requestId?.message}
              {...register('requestId')}
            >
              <option value="">선택해 주세요</option>
              {/*
                고를 수 없는 요청도 **보여준다.** 목록에서 아예 빼면 관리자는 "내가 방금 판정한
                요청이 왜 없지" 로 헤맨다 - 무엇이 남았는지 알려 주는 편이 낫다.
              */}
              {(requests ?? []).map((request) => {
                const reason = blockingReason(request)
                return (
                  <option key={request.id} value={request.id} disabled={reason !== null}>
                    #{request.id} · {request.title}
                    {reason ? ` (${reason})` : ''}
                  </option>
                )
              })}
            </Select>
          )}

          <Input label="공고 제목" error={errors.title?.message} {...register('title')} />
          <Textarea label="공고 내용" error={errors.content?.message} {...register('content')} />
          <Textarea
            label="참가 자격 요건"
            hint="선택 항목입니다."
            error={errors.eligibility?.message}
            {...register('eligibility')}
          />
          <Textarea
            label="제출 자료 요구사항"
            hint="JSON 문자열 형식(선택 항목)입니다."
            error={errors.submissionRequirements?.message}
            {...register('submissionRequirements')}
          />

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
          </div>

          {formError && <p className="text-label-sm text-error">{formError}</p>}

          <Button type="submit" size="lg" loading={createMutation.isPending}>
            초안 생성
          </Button>
        </form>
      </Card>
    </div>
  )
}

export default AdminRecruitmentNoticeFormPage
