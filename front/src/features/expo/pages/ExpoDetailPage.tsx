'use client'

import Link from 'next/link'
import { useParams } from 'next/navigation'

import {
  Badge,
  Button,
  Card,
  CardTitle,
  EmptyState,
  ErrorState,
  LoadingBlock,
  PageHeader,
} from '@/components/ui'
import { useAuthStore } from '@/lib/auth'
import { formatDateTime } from '@/lib/date'

import type { PurchasableTicketProduct } from '../api'
import { usePurchasableTicketProducts } from '../hooks'

/**
 * `/expos/{expoId}` — 박람회 상세. Function.md 2절 "박람회 정보 + 구매 가능 티켓 상품 목록".
 *
 * **박람회 자체(제목·소개·일정·장소)를 조회하는 공개 API 가 없다**(이슈 #107) — 그래서
 * "박람회 소개" 영역은 준비 중으로 둔다. 실제로 붙는 것은 구매 가능한 티켓 상품 목록
 * (`GET /api/expos/{expoId}/ticket-products/purchasable`) 뿐이다. 이 엔드포인트는
 * expoId 를 이미 안다는 전제로만 호출 가능해서, 목록 화면(`ExpoListPage`)에서 여기로 올
 * 방법은 없고 주소를 직접 아는 경우에만 열린다.
 */
const ExpoDetailPage = () => {
  const params = useParams<{ expoId: string }>()
  const parsedExpoId = Number(params.expoId)
  const expoId = Number.isInteger(parsedExpoId) && parsedExpoId > 0 ? parsedExpoId : null

  const { data, isPending, error, refetch } = usePurchasableTicketProducts(expoId)
  const accessToken = useAuthStore((state) => state.accessToken)

  if (expoId === null) {
    return (
      <div>
        <PageHeader title="박람회 상세" />
        <EmptyState title="잘못된 접근입니다" description="박람회 주소가 올바르지 않습니다." />
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-8">
      <PageHeader
        title={`박람회 #${expoId}`}
        description="박람회 소개는 아직 준비 중입니다. 아래에서 구매 가능한 티켓을 확인하세요."
      />

      <section aria-label="박람회 소개">
        <EmptyState
          notReady
          title="박람회 소개"
          description="박람회 소개·일정·장소 정보를 보여주는 API 가 아직 연결되지 않았습니다."
        />
      </section>

      <section aria-label="구매 가능한 티켓" className="flex flex-col gap-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-title-lg text-on-background font-semibold">구매 가능한 티켓</h2>
          <div className="flex gap-2">
            <Link href={`/orders?expoId=${expoId}`}>
              <Button size="sm">{accessToken ? '예매하기' : '회원 예매하기'}</Button>
            </Link>
            <Link href={`/orders/guest?expoId=${expoId}`}>
              <Button variant="secondary" size="sm">
                비회원 예매하기
              </Button>
            </Link>
          </div>
        </div>

        {isPending ? (
          <LoadingBlock label="티켓 정보를 불러오는 중입니다" />
        ) : error ? (
          <ErrorState error={error} onRetry={refetch} />
        ) : data.length === 0 ? (
          <EmptyState
            title="구매 가능한 티켓이 없습니다"
            description="현재 판매 중인 티켓 상품이 없습니다."
          />
        ) : (
          <ul className="grid gap-4 sm:grid-cols-2">
            {data.map((product) => (
              <li key={product.ticketProductId}>
                <TicketProductCard product={product} />
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}

const TicketProductCard = ({ product }: { product: PurchasableTicketProduct }) => {
  const soldOut = product.availableQuantity <= 0

  return (
    <Card className="flex flex-col gap-2">
      <div className="flex items-start justify-between gap-2">
        <CardTitle className="mb-0">{product.name}</CardTitle>
        {soldOut ? <Badge variant="neutral">품절</Badge> : <Badge variant="success">판매중</Badge>}
      </div>
      <p className="text-body-md text-on-surface-variant">{product.description}</p>
      <p className="text-title-lg text-on-surface font-semibold">
        {product.price.toLocaleString('ko-KR')}원
      </p>
      <p className="text-label-md text-on-surface-variant">
        판매기간 {formatDateTime(product.salesStartAt)} ~ {formatDateTime(product.salesEndAt)}
      </p>
      <p className="text-label-sm text-on-surface-variant">
        잔여 {product.availableQuantity}매 · 1회 최대 {product.maxQuantityPerOrder}매
      </p>
    </Card>
  )
}

export default ExpoDetailPage
