'use client'

import { zodResolver } from '@hookform/resolvers/zod'
import { useRouter } from 'next/navigation'
import { useState } from 'react'
import { useForm } from 'react-hook-form'

import { Button, Card, CardTitle, Input, LoadingBlock } from '@/components/ui'
import { useClientMyExpos } from '@/features/client/hooks'
import { uploadFile } from '@/features/file/api'
import { Select, Textarea } from '@/components/ui'
import { getErrorMessage } from '@/lib/errorMessage'

import { bannerImageUrl } from '../api'
import { useCreateBannerRequest } from '../hooks'
import { createBannerRequestSchema, type CreateBannerRequestFormValues } from '../schemas'

/**
 * `/client/banner-requests/new` — 주최사가 자기 박람회를 메인 배너에 올려 달라고 신청한다.
 *
 * <h2>승인된 박람회만 고를 수 있다</h2>
 *
 * 배너는 **이미 승인된 박람회를 광고하는 것**이라, 심사 중이거나 반려된 박람회는 선택지에서
 * 뺀다. 넣어 두면 신청은 되는데 승인 시점에 광고할 대상이 없는 상태가 만들어진다.
 *
 * <h2>이미지는 고르는 즉시 올린다</h2>
 *
 * 신청 API 는 파일 자체가 아니라 `imageFileId` 를 받는다. 그래서 제출할 때 한 번에 보내지
 * 못하고, 파일을 고른 시점에 먼저 올려 ID 를 받아 둬야 한다. 업로드가 끝나기 전에는 제출
 * 버튼을 눌러도 스키마가 막는다.
 */
const ClientBannerRequestFormPage = () => {
  const router = useRouter()
  const { data: expos, isPending: exposPending } = useClientMyExpos()
  const createMutation = useCreateBannerRequest()
  const [formError, setFormError] = useState<string | null>(null)
  const [uploading, setUploading] = useState(false)
  const [preview, setPreview] = useState<number | null>(null)

  const {
    register,
    handleSubmit,
    setValue,
    setError,
    formState: { errors },
  } = useForm<CreateBannerRequestFormValues>({
    resolver: zodResolver(createBannerRequestSchema),
    defaultValues: {
      expoId: '',
      imageFileId: '',
      headline: '',
      requestedStartAt: '',
      requestedEndAt: '',
    },
  })

  const approvedExpos = expos?.filter((expo) => expo.reviewStatus === 'APPROVED') ?? []

  const onFileChange = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    if (!file) {
      return
    }
    // 새 파일을 고른 순간 이전 값을 버린다. 남겨 두면 업로드가 실패했을 때 앞서 올린 fileId 가
    // 그대로 남아, 사용자는 새 이미지를 골랐다고 믿는 채로 **옛 이미지가 딸린 신청**을 제출하게
    // 된다. 오류 메시지는 뜨지만 폼은 유효한 값을 들고 있어 제출이 막히지 않는다.
    //
    // 여기서는 검증을 돌리지 않는다(`shouldValidate` 없음). 업로드 중에 "이미지를 올려 주세요"
    // 가 먼저 떠 버린다.
    setValue('imageFileId', '')
    setPreview(null)

    setUploading(true)
    try {
      const uploaded = await uploadFile(file, 'BANNER_IMAGE')
      setValue('imageFileId', String(uploaded.fileId), { shouldValidate: true })
      setPreview(uploaded.fileId)
    } catch (error) {
      // 업로드 실패를 폼 오류로 되돌린다. 그냥 두면 파일을 골랐는데 아무 일도 안 일어난 것처럼 보인다.
      setError('imageFileId', { message: getErrorMessage(error) })
    } finally {
      setUploading(false)
    }
  }

  const onSubmit = (values: CreateBannerRequestFormValues) => {
    setFormError(null)
    createMutation.mutate(
      {
        expoId: Number(values.expoId),
        imageFileId: Number(values.imageFileId),
        headline: values.headline || undefined,
        requestedStartAt: new Date(values.requestedStartAt).toISOString(),
        requestedEndAt: new Date(values.requestedEndAt).toISOString(),
      },
      {
        onSuccess: () => router.push('/client/banner-requests'),
        onError: (error) => setFormError(getErrorMessage(error)),
      }
    )
  }

  return (
    <div className="flex justify-center">
      <Card className="w-full max-w-2xl">
        <CardTitle>배너 노출 신청</CardTitle>
        <p className="text-label-sm text-on-surface-variant mt-2">
          신청하면 바로 심사 대기 상태가 됩니다. 관리자가 승인하면 희망 기간에 맞춰 메인 홈 상단에
          노출됩니다.
        </p>

        <form className="mt-4 flex flex-col gap-4" onSubmit={handleSubmit(onSubmit)} noValidate>
          {exposPending ? (
            <LoadingBlock label="내 박람회를 불러오는 중입니다" />
          ) : (
            <Select
              label="홍보할 박람회"
              hint={
                approvedExpos.length === 0
                  ? '승인된 박람회가 없습니다. 박람회 개최 승인을 먼저 받아 주세요.'
                  : undefined
              }
              error={errors.expoId?.message}
              {...register('expoId')}
            >
              <option value="">선택해 주세요</option>
              {approvedExpos.map((expo) => (
                <option key={expo.expoId} value={expo.expoId}>
                  {expo.title}
                </option>
              ))}
            </Select>
          )}

          <div>
            <label className="text-label-md text-on-surface mb-1 block" htmlFor="banner-image">
              배너 이미지
            </label>
            <input
              id="banner-image"
              type="file"
              accept="image/*"
              onChange={onFileChange}
              className="text-label-sm text-on-surface-variant w-full"
            />
            {uploading && <p className="text-label-sm mt-1">이미지를 올리는 중입니다…</p>}
            {errors.imageFileId && (
              <p className="text-label-sm text-error mt-1">{errors.imageFileId.message}</p>
            )}
            {preview !== null && (
              /* eslint-disable-next-line @next/next/no-img-element -- 백엔드 프록시 경로라 Next 이미지 최적화 대상이 아니다. */
              <img
                src={bannerImageUrl(preview)}
                alt="올린 배너 이미지 미리보기"
                className="mt-2 h-32 w-full rounded-md object-cover"
              />
            )}
            <input type="hidden" {...register('imageFileId')} />
          </div>

          <Textarea
            label="배너 문구"
            rows={2}
            hint="선택 항목입니다. 이미지 아래에 함께 표시됩니다 (150자 이하)."
            error={errors.headline?.message}
            {...register('headline')}
          />

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Input
              label="희망 노출 시작일시"
              type="datetime-local"
              error={errors.requestedStartAt?.message}
              {...register('requestedStartAt')}
            />
            <Input
              label="희망 노출 종료일시"
              type="datetime-local"
              error={errors.requestedEndAt?.message}
              {...register('requestedEndAt')}
            />
          </div>

          {formError && <p className="text-label-sm text-error">{formError}</p>}

          <Button type="submit" size="lg" loading={createMutation.isPending} disabled={uploading}>
            신청 제출
          </Button>
        </form>
      </Card>
    </div>
  )
}

export default ClientBannerRequestFormPage
