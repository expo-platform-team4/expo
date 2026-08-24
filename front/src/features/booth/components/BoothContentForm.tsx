'use client'

import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'

import { Button, Input, Textarea } from '@/components/ui'

import { boothContentSchema, type BoothContentFormValues } from '../schemas'

/**
 * 부스 콘텐츠 작성·수정 공용 폼. `BoothContentSection` 이 신규 작성과 수정 두 곳에서 쓴다 —
 * 필드 구성이 완전히 같아 (백엔드도 `CreateBoothContentRequest`/`UpdateBoothContentRequest`
 * 가 `boothAllocationId` 유무 말고는 같은 모양이다) 폼 자체를 하나로 둔다.
 */
export const BoothContentForm = ({
  defaultValues,
  onSubmit,
  submitting,
  submitLabel,
  formError,
}: {
  defaultValues: BoothContentFormValues
  onSubmit: (values: BoothContentFormValues) => void
  submitting: boolean
  submitLabel: string
  formError?: string | null
}) => {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<BoothContentFormValues>({
    resolver: zodResolver(boothContentSchema),
    defaultValues,
  })

  return (
    <form className="flex flex-col gap-4" onSubmit={handleSubmit(onSubmit)} noValidate>
      <Input
        label="기업 노출명"
        error={errors.companyDisplayName?.message}
        {...register('companyDisplayName')}
      />
      <Input label="콘텐츠 제목" error={errors.title?.message} {...register('title')} />
      <Textarea
        label="기업 소개"
        hint="선택 항목입니다."
        error={errors.companyDescription?.message}
        {...register('companyDescription')}
      />
      <Textarea
        label="부스 소개"
        hint="선택 항목입니다."
        error={errors.boothDescription?.message}
        {...register('boothDescription')}
      />
      <Textarea
        label="제품 소개"
        hint="선택 항목입니다."
        error={errors.productDescription?.message}
        {...register('productDescription')}
      />

      {formError && <p className="text-label-sm text-error">{formError}</p>}

      <Button type="submit" loading={submitting} className="self-start">
        {submitLabel}
      </Button>
    </form>
  )
}
