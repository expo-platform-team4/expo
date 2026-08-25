'use client'

import { zodResolver } from '@hookform/resolvers/zod'
import { useRouter } from 'next/navigation'
import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'

import { Button, Card, CardTitle, Input, LoadingBlock, Select, Textarea } from '@/components/ui'
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

  const approvedRequests = (requests ?? []).filter((request) => request.status === 'APPROVED')

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
    const request = approvedRequests.find((r) => String(r.id) === selectedRequestId)
    if (!request) return
    setValue('title', request.title)
    setValue('content', request.description)
    setValue('applicationStartAt', toDatetimeLocalValue(request.applicationStartAt))
    setValue('applicationEndAt', toDatetimeLocalValue(request.applicationEndAt))
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
              hint="APPROVED 상태의 요청만 고를 수 있습니다."
              error={errors.requestId?.message}
              {...register('requestId')}
            >
              <option value="">선택해 주세요</option>
              {approvedRequests.map((request) => (
                <option key={request.id} value={request.id}>
                  #{request.id} · {request.title}
                </option>
              ))}
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
