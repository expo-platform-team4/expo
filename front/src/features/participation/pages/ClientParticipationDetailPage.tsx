'use client'

import { zodResolver } from '@hookform/resolvers/zod'
import Link from 'next/link'
import { useParams, useRouter } from 'next/navigation'
import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'

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
import { useCreateBoothOrder } from '@/features/booth/hooks'
import { getErrorMessage } from '@/lib/errorMessage'

import { BoothProductLayoutPreview } from '../components/BoothProductLayoutPreview'
import {
  useAvailableBoothProducts,
  useMyParticipationApplication,
  useUpdateParticipationApplication,
  useWithdrawParticipationApplication,
} from '../hooks'
import { applyParticipationSchema, type ApplyParticipationFormValues } from '../schemas'
import type { ParticipationApplicationStatus } from '../api'

const statusLabel = (status: ParticipationApplicationStatus): string => {
  switch (status) {
    case 'DRAFT':
      return '임시저장'
    case 'PAYMENT_PENDING':
      return '결제 대기'
    case 'SUBMITTED':
      return '제출 완료'
    case 'PAYMENT_FAILED':
      return '결제 실패'
    case 'CANCELED':
      return '철회됨'
    default:
      return status
  }
}

const statusVariant = (
  status: ParticipationApplicationStatus
): 'success' | 'neutral' | 'error' | 'info' => {
  switch (status) {
    case 'SUBMITTED':
      return 'success'
    case 'PAYMENT_PENDING':
      return 'info'
    case 'PAYMENT_FAILED':
    case 'CANCELED':
      return 'error'
    default:
      return 'neutral'
  }
}

/**
 * `/client/participations/[applicationId]`. CLIENT 전용.
 *
 * 목록 화면(`ClientParticipationListPage`)은 상태 뱃지만 보여주고 다음에 뭘 해야 하는지
 * 안내가 없다 — 이 상세 화면이 상태별로 실제 행동을 이어준다: 초안이면 수정·철회·주문 생성,
 * 결제 대기면 결제 계속하기 링크, 그 외 상태는 진행 상황만 보여준다.
 */
const ClientParticipationDetailPage = () => {
  const params = useParams<{ applicationId: string }>()
  const applicationId = Number(params.applicationId)
  const router = useRouter()

  const {
    data: application,
    isPending,
    isError,
    error,
    refetch,
  } = useMyParticipationApplication(applicationId)
  const { data: boothProducts } = useAvailableBoothProducts(
    application?.recruitmentNoticeId ?? null
  )
  const updateMutation = useUpdateParticipationApplication()
  const withdrawMutation = useWithdrawParticipationApplication()
  const createOrderMutation = useCreateBoothOrder()

  const [editing, setEditing] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    reset,
    watch,
    formState: { errors },
  } = useForm<ApplyParticipationFormValues>({
    resolver: zodResolver(applyParticipationSchema),
    defaultValues: {
      companyNameSnapshot: '',
      participationPurpose: '',
      exhibitDescription: '',
      selectedBoothProductId: '',
    },
  })

  useEffect(() => {
    if (!application) return
    reset({
      companyNameSnapshot: application.companyNameSnapshot,
      participationPurpose: application.participationPurpose ?? '',
      exhibitDescription: application.exhibitDescription ?? '',
      selectedBoothProductId: application.selectedBoothProductId
        ? String(application.selectedBoothProductId)
        : '',
    })
  }, [application, reset])

  const selectedBoothProductId = watch('selectedBoothProductId')

  if (isPending) {
    return <LoadingBlock label="신청서를 불러오는 중입니다" />
  }
  if (isError) {
    return <ErrorState error={error} onRetry={() => refetch()} />
  }

  const isDraft = application.status === 'DRAFT'

  const onSubmit = (values: ApplyParticipationFormValues) => {
    setFormError(null)
    updateMutation.mutate(
      {
        applicationId,
        payload: {
          companyNameSnapshot: values.companyNameSnapshot,
          participationPurpose: values.participationPurpose || undefined,
          exhibitDescription: values.exhibitDescription || undefined,
          selectedBoothProductId: values.selectedBoothProductId
            ? Number(values.selectedBoothProductId)
            : undefined,
        },
      },
      {
        onSuccess: () => setEditing(false),
        onError: (err) => setFormError(getErrorMessage(err)),
      }
    )
  }

  const handleWithdraw = () => {
    if (!window.confirm('참여 신청을 철회할까요? 되돌릴 수 없습니다.')) return
    withdrawMutation.mutate(applicationId)
  }

  const handleCreateOrder = () => {
    createOrderMutation.mutate(applicationId, {
      onSuccess: (order) => router.push(`/client/booth-orders/${order.id}`),
    })
  }

  return (
    <div>
      <PageHeader title="참여 신청 상세" description={`공고 #${application.recruitmentNoticeId}`} />

      <div className="flex flex-col gap-6">
        <Card className="flex flex-col gap-4">
          <div className="flex items-center justify-between">
            <CardTitle className="mb-0">신청 내용</CardTitle>
            <Badge variant={statusVariant(application.status)}>
              {statusLabel(application.status)}
            </Badge>
          </div>

          {editing ? (
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
                hint="선택 항목입니다."
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
                product={boothProducts?.find(
                  (product) => String(product.id) === selectedBoothProductId
                )}
              />
              {formError && <p className="text-label-sm text-error">{formError}</p>}
              <div className="flex gap-2">
                <Button type="submit" loading={updateMutation.isPending}>
                  저장
                </Button>
                <Button type="button" variant="secondary" onClick={() => setEditing(false)}>
                  취소
                </Button>
              </div>
            </form>
          ) : (
            <dl className="text-body-md text-on-surface grid grid-cols-1 gap-3 sm:grid-cols-2">
              <div>
                <dt className="text-label-sm text-on-surface-variant">참가 기업명</dt>
                <dd>{application.companyNameSnapshot}</dd>
              </div>
              <div>
                <dt className="text-label-sm text-on-surface-variant">선택한 부스 상품</dt>
                <dd>
                  {application.selectedBoothProductId
                    ? `#${application.selectedBoothProductId}`
                    : '선택 안 함'}
                </dd>
              </div>
              {application.participationPurpose && (
                <div className="sm:col-span-2">
                  <dt className="text-label-sm text-on-surface-variant">참여 목적</dt>
                  <dd className="whitespace-pre-wrap">{application.participationPurpose}</dd>
                </div>
              )}
              {application.exhibitDescription && (
                <div className="sm:col-span-2">
                  <dt className="text-label-sm text-on-surface-variant">전시 품목 설명</dt>
                  <dd className="whitespace-pre-wrap">{application.exhibitDescription}</dd>
                </div>
              )}
            </dl>
          )}

          {!editing && isDraft && (
            <div className="flex flex-wrap gap-2">
              <Button type="button" variant="secondary" size="sm" onClick={() => setEditing(true)}>
                수정
              </Button>
              <Button
                type="button"
                variant="danger"
                size="sm"
                loading={withdrawMutation.isPending}
                onClick={handleWithdraw}
              >
                철회
              </Button>
            </div>
          )}
          {withdrawMutation.isError && (
            <p className="text-label-sm text-error">{getErrorMessage(withdrawMutation.error)}</p>
          )}
        </Card>

        {isDraft && application.selectedBoothProductId != null && (
          <Card className="flex flex-col gap-3">
            <CardTitle className="mb-0">부스 주문</CardTitle>
            <p className="text-body-md text-on-surface-variant">
              선택한 부스 상품으로 주문을 만들고 결제를 진행합니다. 주문은 15분간 유효합니다.
            </p>
            <Button
              type="button"
              size="lg"
              loading={createOrderMutation.isPending}
              onClick={handleCreateOrder}
            >
              주문하고 결제하기
            </Button>
            {createOrderMutation.isError && (
              <p className="text-label-sm text-error">
                {getErrorMessage(createOrderMutation.error)}
              </p>
            )}
          </Card>
        )}

        {application.status === 'PAYMENT_PENDING' && application.boothOrderId != null && (
          <Card className="flex flex-col gap-2">
            <CardTitle className="mb-0">결제 대기 중</CardTitle>
            <Link
              href={`/client/booth-orders/${application.boothOrderId}`}
              className="text-secondary text-label-md underline"
            >
              결제 계속하기
            </Link>
          </Card>
        )}

        {application.status === 'SUBMITTED' && (
          <Card className="flex flex-col gap-2">
            <CardTitle className="mb-0">신청이 제출됐습니다</CardTitle>
            <p className="text-body-md text-on-surface-variant">
              결제가 완료되어 부스가 확정 배정됐습니다. 배정 결과는 내 부스 목록에서 확인할 수
              있습니다.
            </p>
            <Link href="/client/booths" className="text-secondary text-label-md underline">
              내 부스 목록으로 이동
            </Link>
          </Card>
        )}
      </div>
    </div>
  )
}

export default ClientParticipationDetailPage
