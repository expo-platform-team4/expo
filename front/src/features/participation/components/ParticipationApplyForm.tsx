'use client'

import { zodResolver } from '@hookform/resolvers/zod'
import Link from 'next/link'
import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'

import { Badge, Button, Input, Select, Textarea } from '@/components/ui'
import { useClientDashboardProfile } from '@/features/client/hooks'
import { getErrorMessage } from '@/lib/errorMessage'

import { useAvailableBoothProducts, useCreateParticipationApplication } from '../hooks'
import { applyParticipationSchema, type ApplyParticipationFormValues } from '../schemas'
import { BoothProductLayoutPreview } from './BoothProductLayoutPreview'

/**
 * 공고 상세 화면(`/recruitment-notices/{noticeId}`)에 얹는 참여 신청 폼. **로그인한 CLIENT
 * 에게만 렌더링된다** — 그 판단은 `RecruitmentNoticeDetailPage` 가 한다.
 */
export const ParticipationApplyForm = ({ noticeId }: { noticeId: number }) => {
  const { data: profile } = useClientDashboardProfile()
  const { data: boothProducts } = useAvailableBoothProducts(noticeId)
  const applyMutation = useCreateParticipationApplication()
  const [formError, setFormError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors, dirtyFields },
  } = useForm<ApplyParticipationFormValues>({
    resolver: zodResolver(applyParticipationSchema),
    defaultValues: {
      companyNameSnapshot: '',
      participationPurpose: '',
      exhibitDescription: '',
      selectedBoothProductId: '',
    },
  })

  const selectedBoothProductId = watch('selectedBoothProductId')

  // 기업명은 클라이언트 프로필의 회사명으로 미리 채운다. 사용자가 직접 고친 뒤에는 덮어쓰지 않는다.
  useEffect(() => {
    if (profile?.companyName && !dirtyFields.companyNameSnapshot) {
      setValue('companyNameSnapshot', profile.companyName)
    }
  }, [profile, dirtyFields.companyNameSnapshot, setValue])

  if (applyMutation.isSuccess) {
    return (
      <div className="border-outline-variant rounded-md border border-dashed px-6 py-8 text-center">
        <Badge variant="success" className="mb-3">
          신청이 접수되었습니다
        </Badge>
        <p className="text-body-md text-on-surface-variant">
          <Link href="/client/participations" className="text-secondary font-semibold">
            참여 신청 내역
          </Link>
          에서 진행 상황을 확인할 수 있습니다.
        </p>
      </div>
    )
  }

  const onSubmit = (values: ApplyParticipationFormValues) => {
    setFormError(null)
    applyMutation.mutate(
      {
        recruitmentNoticeId: noticeId,
        companyNameSnapshot: values.companyNameSnapshot,
        participationPurpose: values.participationPurpose || undefined,
        exhibitDescription: values.exhibitDescription || undefined,
        selectedBoothProductId: values.selectedBoothProductId
          ? Number(values.selectedBoothProductId)
          : undefined,
      },
      { onError: (error) => setFormError(getErrorMessage(error)) }
    )
  }

  return (
    <form className="flex flex-col gap-4" onSubmit={handleSubmit(onSubmit)} noValidate>
      <Input
        label="참가 기업명"
        error={errors.companyNameSnapshot?.message}
        {...register('companyNameSnapshot')}
      />
      <Textarea
        label="참여 목적"
        hint="선택 항목입니다."
        error={errors.participationPurpose?.message}
        {...register('participationPurpose')}
      />
      <Textarea
        label="전시 품목 설명"
        hint="선택 항목입니다."
        error={errors.exhibitDescription?.message}
        {...register('exhibitDescription')}
      />
      <Select
        label="선택한 부스 상품"
        hint="선택 항목입니다. 이 공고에 등록된, 구매 가능한 부스 상품만 나타납니다."
        error={errors.selectedBoothProductId?.message}
        {...register('selectedBoothProductId')}
      >
        <option value="">선택 안 함</option>
        {boothProducts?.map((product) => (
          <option key={product.id} value={product.id}>
            {product.boothNumber} · {product.venueHallName} {product.venueZoneName} ·{' '}
            {Number(product.totalPrice).toLocaleString('ko-KR')}원
          </option>
        ))}
      </Select>

      <BoothProductLayoutPreview
        product={boothProducts?.find((product) => String(product.id) === selectedBoothProductId)}
      />

      {formError && <p className="text-label-sm text-error">{formError}</p>}

      <Button type="submit" size="lg" loading={applyMutation.isPending}>
        참여 신청하기
      </Button>
    </form>
  )
}
