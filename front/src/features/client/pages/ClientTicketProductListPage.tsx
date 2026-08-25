'use client'

import { zodResolver } from '@hookform/resolvers/zod'
import { useParams, useRouter } from 'next/navigation'
import { useState } from 'react'
import { useForm } from 'react-hook-form'

import {
  Badge,
  Button,
  Card,
  CardTitle,
  EmptyState,
  ErrorState,
  Input,
  LoadingBlock,
  PageHeader,
  Textarea,
} from '@/components/ui'
import type { BadgeVariant } from '@/components/ui'
import {
  useClientTicketProducts,
  useCreateTicketProduct,
  useUpdateTicketProduct,
  useUpdateTicketProductStatus,
} from '@/features/ticket/hooks'
import type { ClientTicketProduct, TicketProductStatus } from '@/features/ticket/api'
import {
  createTicketProductSchema,
  type CreateTicketProductFormValues,
} from '@/features/ticket/schemas'
import { formatCurrency } from '@/lib/currency'
import { formatDateTime, toDateInputValue } from '@/lib/date'
import { getErrorMessage } from '@/lib/errorMessage'

import { useClientMyExpos } from '../hooks'

const STATUS_LABEL: Record<TicketProductStatus, string> = {
  DRAFT: '작성 중',
  ON_SALE: '판매중',
  SOLD_OUT: '매진',
  SALE_ENDED: '판매 종료',
  CANCELED: '취소됨',
}

const STATUS_VARIANT: Record<TicketProductStatus, BadgeVariant> = {
  DRAFT: 'neutral',
  ON_SALE: 'success',
  SOLD_OUT: 'info',
  SALE_ENDED: 'neutral',
  CANCELED: 'error',
}

/**
 * `/client/expos/{expoId}/tickets` — 주최사가 자기 박람회의 티켓 상품을 만들고 판매를 시작한다.
 *
 * 새 상품은 항상 `DRAFT`(작성 중)로 만들어진다 — 이 화면에서 "판매 시작"을 눌러 `ON_SALE`로
 * 전환해야 `/expos/{expoId}` 공개 상세의 구매 가능 목록에 나타난다(그 목록은 `ON_SALE`만
 * 보여준다). 가격·재고 수정도 `DRAFT` 상태일 때만 서버가 허용한다 — 판매가 시작되면 이 화면은
 * "판매 취소"만 남기고 수정 버튼을 감춘다.
 */
const ClientTicketProductListPage = () => {
  const params = useParams<{ expoId: string }>()
  const router = useRouter()
  const parsedExpoId = Number(params.expoId)
  const expoId = Number.isInteger(parsedExpoId) && parsedExpoId > 0 ? parsedExpoId : null

  if (expoId === null) {
    return (
      <div>
        <PageHeader title="티켓 상품 관리" />
        <EmptyState title="잘못된 접근입니다" description="박람회 주소가 올바르지 않습니다." />
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="티켓 상품 관리"
        description="여기서 만든 상품을 게시해야 예매 화면에 노출됩니다."
        action={
          <Button variant="secondary" onClick={() => router.push(`/expos/${expoId}`)}>
            공개 화면 보기
          </Button>
        }
      />
      <CreateProductForm expoId={expoId} />
      <ProductList expoId={expoId} />
    </div>
  )
}

const CreateProductForm = ({ expoId }: { expoId: number }) => {
  const [open, setOpen] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)
  const createMutation = useCreateTicketProduct(expoId)
  const { data: myExpos } = useClientMyExpos()
  const expo = myExpos?.find((item) => item.expoId === expoId)

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<CreateTicketProductFormValues>({
    resolver: zodResolver(createTicketProductSchema),
    defaultValues: {
      name: '',
      description: '',
      price: 0,
      totalQuantity: 0,
      maxQuantityPerOrder: 4,
    },
  })

  if (!open) {
    return (
      <Button onClick={() => setOpen(true)} className="self-start">
        티켓 상품 만들기
      </Button>
    )
  }

  const onSubmit = (values: CreateTicketProductFormValues) => {
    if (!expo) {
      setFormError('박람회 정보를 아직 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.')
      return
    }
    setFormError(null)
    createMutation.mutate(
      {
        name: values.name,
        description: values.description || undefined,
        price: values.price,
        salesStartAt: expo.salesStartAt,
        salesEndAt: expo.salesEndAt,
        totalQuantity: values.totalQuantity,
        maxQuantityPerOrder: values.maxQuantityPerOrder,
      },
      {
        onSuccess: () => {
          reset()
          setOpen(false)
        },
        onError: (error) => setFormError(getErrorMessage(error)),
      }
    )
  }

  return (
    <Card>
      <CardTitle>티켓 상품 만들기</CardTitle>
      <form className="mt-4 flex flex-col gap-4" onSubmit={handleSubmit(onSubmit)} noValidate>
        <Input label="상품명" error={errors.name?.message} {...register('name')} />
        <Textarea
          label="설명 (선택)"
          error={errors.description?.message}
          {...register('description')}
        />
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <Input
            label="가격"
            type="number"
            min={0}
            error={errors.price?.message}
            {...register('price', { valueAsNumber: true })}
          />
          <Input
            label="총 수량"
            type="number"
            min={0}
            error={errors.totalQuantity?.message}
            {...register('totalQuantity', { valueAsNumber: true })}
          />
          <Input
            label="1회 최대 구매 수량"
            type="number"
            min={1}
            max={4}
            error={errors.maxQuantityPerOrder?.message}
            {...register('maxQuantityPerOrder', { valueAsNumber: true })}
          />
        </div>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Input
            label="티켓 판매 시작"
            type="date"
            readOnly
            className="bg-surface-container-low"
            hint="박람회 개최 신청 때 정한 기간으로 고정됩니다."
            value={expo ? toDateInputValue(expo.salesStartAt) : ''}
          />
          <Input
            label="티켓 판매 종료"
            type="date"
            readOnly
            className="bg-surface-container-low"
            hint="박람회 개최 신청 때 정한 기간으로 고정됩니다."
            value={expo ? toDateInputValue(expo.salesEndAt) : ''}
          />
        </div>

        {formError && <p className="text-label-sm text-error">{formError}</p>}

        <div className="flex gap-2">
          <Button type="submit" loading={createMutation.isPending}>
            만들기
          </Button>
          <Button
            type="button"
            variant="ghost"
            disabled={createMutation.isPending}
            onClick={() => {
              reset()
              setFormError(null)
              setOpen(false)
            }}
          >
            취소
          </Button>
        </div>
      </form>
    </Card>
  )
}

const ProductList = ({ expoId }: { expoId: number }) => {
  const { data, isPending, isError, error, refetch } = useClientTicketProducts(expoId)

  if (isError) {
    return <ErrorState error={error} onRetry={() => refetch()} />
  }

  if (isPending) {
    return <LoadingBlock label="티켓 상품을 불러오는 중입니다" />
  }

  if (data.length === 0) {
    return (
      <EmptyState
        title="등록한 티켓 상품이 없습니다"
        description="위에서 첫 티켓 상품을 만들어 보세요."
      />
    )
  }

  return (
    <ul className="flex flex-col gap-4">
      {data.map((product) => (
        <li key={product.ticketProductId}>
          <ProductCard expoId={expoId} product={product} />
        </li>
      ))}
    </ul>
  )
}

const ProductCard = ({ expoId, product }: { expoId: number; product: ClientTicketProduct }) => {
  const [editing, setEditing] = useState(false)
  const [price, setPrice] = useState(String(product.price))
  const [totalQuantity, setTotalQuantity] = useState(String(product.totalQuantity))
  const [error, setError] = useState<string | null>(null)

  const updateMutation = useUpdateTicketProduct(expoId)
  const statusMutation = useUpdateTicketProductStatus(expoId)

  const canEdit = product.status === 'DRAFT'
  const canPublish = product.status === 'DRAFT'
  const canCancel = product.status === 'DRAFT' || product.status === 'ON_SALE'

  const submitEdit = () => {
    setError(null)
    updateMutation.mutate(
      {
        ticketProductId: product.ticketProductId,
        payload: { price: Number(price), totalQuantity: Number(totalQuantity) },
      },
      {
        onSuccess: () => setEditing(false),
        onError: (err) => setError(getErrorMessage(err)),
      }
    )
  }

  return (
    <Card>
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="flex flex-wrap items-center gap-2">
            <h3 className="text-title-lg text-on-surface font-semibold">{product.name}</h3>
            <Badge variant={STATUS_VARIANT[product.status]}>{STATUS_LABEL[product.status]}</Badge>
          </div>
          {product.description && (
            <p className="text-body-sm text-on-surface-variant mt-1">{product.description}</p>
          )}
          <p className="text-body-sm text-on-surface-variant mt-2">
            판매 기간 {formatDateTime(product.salesStartAt)} ~ {formatDateTime(product.salesEndAt)}
          </p>
        </div>

        <div className="flex gap-2">
          {canPublish && (
            <Button
              size="sm"
              loading={statusMutation.isPending}
              onClick={() =>
                statusMutation.mutate({
                  ticketProductId: product.ticketProductId,
                  status: 'ON_SALE',
                })
              }
            >
              판매 시작
            </Button>
          )}
          {canCancel && (
            <Button
              size="sm"
              variant="danger"
              loading={statusMutation.isPending}
              onClick={() =>
                statusMutation.mutate({
                  ticketProductId: product.ticketProductId,
                  status: 'CANCELED',
                })
              }
            >
              판매 취소
            </Button>
          )}
        </div>
      </div>

      {!editing ? (
        <dl className="text-body-sm text-on-surface-variant mt-4 grid grid-cols-2 gap-y-1 sm:grid-cols-4">
          <div>
            <dt className="text-label-sm">가격</dt>
            <dd className="text-on-surface">{formatCurrency(product.price)}</dd>
          </div>
          <div>
            <dt className="text-label-sm">총 수량</dt>
            <dd className="text-on-surface">{product.totalQuantity.toLocaleString('ko-KR')}매</dd>
          </div>
          <div>
            <dt className="text-label-sm">잔여 수량</dt>
            <dd className="text-on-surface">
              {product.availableQuantity.toLocaleString('ko-KR')}매
            </dd>
          </div>
          <div>
            <dt className="text-label-sm">1회 최대 구매</dt>
            <dd className="text-on-surface">{product.maxQuantityPerOrder}매</dd>
          </div>
        </dl>
      ) : (
        <div className="mt-4 flex flex-wrap items-end gap-3">
          <div className="w-32">
            <Input
              label="가격"
              type="number"
              min={0}
              value={price}
              onChange={(e) => setPrice(e.target.value)}
            />
          </div>
          <div className="w-32">
            <Input
              label="총 수량"
              type="number"
              min={0}
              value={totalQuantity}
              onChange={(e) => setTotalQuantity(e.target.value)}
            />
          </div>
          <Button size="sm" loading={updateMutation.isPending} onClick={submitEdit}>
            저장
          </Button>
          <Button size="sm" variant="ghost" onClick={() => setEditing(false)}>
            취소
          </Button>
        </div>
      )}

      {error && <p className="text-error text-label-sm mt-2">{error}</p>}
      {statusMutation.isError && (
        <p className="text-error text-label-sm mt-2">
          {getErrorMessage(statusMutation.error, '상태 변경에 실패했습니다.')}
        </p>
      )}

      {canEdit && !editing && (
        <Button size="sm" variant="secondary" className="mt-3" onClick={() => setEditing(true)}>
          가격·수량 수정
        </Button>
      )}
    </Card>
  )
}

export default ClientTicketProductListPage
