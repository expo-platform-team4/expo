'use client'

import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'

import { Button, Input, Textarea } from '@/components/ui'
import { uploadFile } from '@/features/file/api'
import { getErrorMessage } from '@/lib/errorMessage'

import { boothContentSchema, type BoothContentFormValues } from '../schemas'

export type BoothContentSubmitValues = BoothContentFormValues & {
  logoFileId: number | null
  mainImageFileId: number | null
}

/**
 * 로고·대표 이미지 한 장짜리 업로더. 고르면 바로 업로드하고(`ProfileEditPage`와 같은 2단계
 * 패턴 — 먼저 올려서 fileId 를 받고, 실제 저장은 폼 제출 시점에 그 id 를 실어 보낸다),
 * 결과 fileId 를 부모(`BoothContentForm`)의 상태로 올려보낸다.
 */
const SingleImageUploader = ({
  label,
  fileId,
  onChange,
}: {
  label: string
  fileId: number | null
  onChange: (fileId: number | null) => void
}) => {
  const [previewUrl, setPreviewUrl] = useState<string | null>(null)
  const [isUploading, setIsUploading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // 로컬 미리보기용 objectURL은 쓰고 나면 반드시 지운다 — 안 지우면 메모리에 계속 쌓인다.
  useEffect(() => {
    return () => {
      if (previewUrl) URL.revokeObjectURL(previewUrl)
    }
  }, [previewUrl])

  const handleFileSelected = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return

    setError(null)
    if (previewUrl) URL.revokeObjectURL(previewUrl)
    setPreviewUrl(URL.createObjectURL(file))
    setIsUploading(true)
    try {
      const uploaded = await uploadFile(file, 'BOOTH_IMAGE')
      onChange(uploaded.fileId)
    } catch (err) {
      setError(getErrorMessage(err))
      setPreviewUrl(null)
    } finally {
      setIsUploading(false)
    }
  }

  return (
    <div className="flex flex-col gap-1.5">
      <span className="text-label-md text-on-surface-variant font-medium">{label}</span>
      <div className="flex items-center gap-3">
        <div className="bg-surface-container-high border-outline-variant flex h-20 w-20 shrink-0 items-center justify-center overflow-hidden rounded border">
          {previewUrl || fileId ? (
            // eslint-disable-next-line @next/next/no-img-element -- 경로가 /api/files/{id}/content 라 Next 이미지 최적화 대상이 아니다.
            <img
              src={previewUrl ?? `/api/files/${fileId}/content`}
              alt={label}
              className="h-full w-full object-cover"
            />
          ) : (
            <span className="text-label-sm text-on-surface-variant">없음</span>
          )}
        </div>
        <div className="flex flex-col gap-1">
          <label className="w-fit">
            <span className="border-outline text-label-md hover:bg-surface-container-low inline-flex h-9 cursor-pointer items-center rounded border px-3">
              {isUploading ? '업로드 중...' : fileId ? '교체' : '업로드'}
            </span>
            <input
              type="file"
              accept="image/jpeg,image/png,image/webp"
              className="hidden"
              disabled={isUploading}
              onChange={handleFileSelected}
            />
          </label>
          {fileId != null && (
            <button
              type="button"
              className="text-label-sm text-error text-left"
              onClick={() => {
                if (previewUrl) URL.revokeObjectURL(previewUrl)
                setPreviewUrl(null)
                onChange(null)
              }}
            >
              제거
            </button>
          )}
        </div>
      </div>
      {error && <p className="text-label-sm text-error">{error}</p>}
    </div>
  )
}

/**
 * 부스 콘텐츠 작성·수정 공용 폼. `BoothContentSection` 이 신규 작성과 수정 두 곳에서 쓴다 —
 * 필드 구성이 완전히 같아 (백엔드도 `CreateBoothContentRequest`/`UpdateBoothContentRequest`
 * 가 `boothAllocationId` 유무 말고는 같은 모양이다) 폼 자체를 하나로 둔다.
 *
 * 로고·대표 이미지는 react-hook-form 이 아니라 별도 상태로 관리한다 — 텍스트 필드처럼
 * 값을 바로 들고 있는 게 아니라 "파일 선택 → 업로드 → fileId" 2단계를 거치기 때문이다.
 */
export const BoothContentForm = ({
  defaultValues,
  defaultLogoFileId,
  defaultMainImageFileId,
  onSubmit,
  submitting,
  submitLabel,
  formError,
}: {
  defaultValues: BoothContentFormValues
  defaultLogoFileId: number | null
  defaultMainImageFileId: number | null
  onSubmit: (values: BoothContentSubmitValues) => void
  submitting: boolean
  submitLabel: string
  formError?: string | null
}) => {
  const [logoFileId, setLogoFileId] = useState<number | null>(defaultLogoFileId)
  const [mainImageFileId, setMainImageFileId] = useState<number | null>(defaultMainImageFileId)

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<BoothContentFormValues>({
    resolver: zodResolver(boothContentSchema),
    defaultValues,
  })

  const submit = (values: BoothContentFormValues) => {
    onSubmit({ ...values, logoFileId, mainImageFileId })
  }

  return (
    <form className="flex flex-col gap-4" onSubmit={handleSubmit(submit)} noValidate>
      <div className="flex flex-col gap-4 sm:flex-row">
        <SingleImageUploader label="로고" fileId={logoFileId} onChange={setLogoFileId} />
        <SingleImageUploader
          label="대표 이미지"
          fileId={mainImageFileId}
          onChange={setMainImageFileId}
        />
      </div>

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
