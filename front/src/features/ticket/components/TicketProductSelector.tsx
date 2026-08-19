import { Minus, Plus } from 'lucide-react'

import { Badge, Button, Card } from '@/components/ui'
import { formatCurrency } from '@/lib/currency'
import { formatDateTime } from '@/lib/date'

import type { PurchasableTicketProduct, TicketOrderItemPayload } from '../api'

const MAX_TOTAL_QUANTITY = 4

/**
 * 티켓 예매 화면 1·2 가 공유하는 상품 선택 UI. `features/ticket` 전용 컴포넌트다 —
 * `components/ui/*` 는 이 프로젝트 전역 공용 킷이라 여기 두지 않는다(작업 지시 —
 * `components/ui/*` 비소유. Style.md 4절 "새 컴포넌트를 만들지 않는다" 는 그 킷 얘기고,
 * 이건 화면 조합 컴포넌트라 다르다).
 *
 * 제어 컴포넌트다 — 선택 상태(`value`)를 부모(RHF `setValue`)가 들고 있고, 여기서는 UI 만
 * 그린다. 백엔드 제약 셋을 그대로 UI 로 막는다: 상품 종류 최대 4개, 상품당 최대 4매,
 * 재고·1회 최대 구매 수량, 그리고 **주문 전체 수량도 4매를 넘지 못한다**
 * (`TicketOrderService.MAX_TOTAL_QUANTITY`).
 */
export const TicketProductSelector = ({
  products,
  value,
  onChange,
  error,
}: {
  products: PurchasableTicketProduct[]
  value: TicketOrderItemPayload[]
  onChange: (items: TicketOrderItemPayload[]) => void
  error?: string
}) => {
  const totalQuantity = value.reduce((sum, item) => sum + item.quantity, 0)
  const totalAmount = value.reduce((sum, item) => {
    const product = products.find((p) => p.ticketProductId === item.ticketProductId)
    return sum + (product?.price ?? 0) * item.quantity
  }, 0)

  const quantityOf = (productId: number) =>
    value.find((item) => item.ticketProductId === productId)?.quantity ?? 0

  const setQuantity = (product: PurchasableTicketProduct, next: number) => {
    const clamped = Math.max(
      0,
      Math.min(next, product.maxQuantityPerOrder, product.availableQuantity)
    )
    const rest = value.filter((item) => item.ticketProductId !== product.ticketProductId)
    onChange(
      clamped > 0
        ? [...rest, { ticketProductId: product.ticketProductId, quantity: clamped }]
        : rest
    )
  }

  return (
    <div className="flex flex-col gap-4">
      <ul className="flex flex-col gap-3">
        {products.map((product) => {
          const quantity = quantityOf(product.ticketProductId)
          const soldOut = product.availableQuantity <= 0
          const atOrderLimit = totalQuantity >= MAX_TOTAL_QUANTITY && quantity === 0
          const canIncrease =
            !soldOut &&
            quantity < product.maxQuantityPerOrder &&
            quantity < product.availableQuantity &&
            totalQuantity < MAX_TOTAL_QUANTITY

          return (
            <li key={product.ticketProductId}>
              <Card className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                <div className="flex flex-col gap-1">
                  <div className="flex items-center gap-2">
                    <p className="text-title-lg text-on-surface font-semibold">{product.name}</p>
                    {soldOut && <Badge variant="neutral">품절</Badge>}
                  </div>
                  <p className="text-body-md text-on-surface-variant">{product.description}</p>
                  <p className="text-label-md text-on-surface font-medium">
                    {formatCurrency(product.price)}
                  </p>
                  <p className="text-label-sm text-on-surface-variant">
                    판매기간 {formatDateTime(product.salesStartAt)} ~{' '}
                    {formatDateTime(product.salesEndAt)} · 잔여 {product.availableQuantity}매 · 1회
                    최대 {product.maxQuantityPerOrder}매
                  </p>
                </div>

                <div className="flex items-center gap-3">
                  <Button
                    type="button"
                    variant="secondary"
                    size="sm"
                    aria-label={`${product.name} 수량 줄이기`}
                    disabled={quantity === 0}
                    onClick={() => setQuantity(product, quantity - 1)}
                  >
                    <Minus className="h-4 w-4" aria-hidden />
                  </Button>
                  <span className="text-title-lg w-6 text-center font-semibold" aria-live="polite">
                    {quantity}
                  </span>
                  <Button
                    type="button"
                    variant="secondary"
                    size="sm"
                    aria-label={`${product.name} 수량 늘리기`}
                    disabled={!canIncrease}
                    onClick={() => setQuantity(product, quantity + 1)}
                  >
                    <Plus className="h-4 w-4" aria-hidden />
                  </Button>
                </div>
                {atOrderLimit && (
                  <p className="text-label-sm text-on-surface-variant sm:hidden">
                    주문당 최대 {MAX_TOTAL_QUANTITY}매까지 선택할 수 있습니다.
                  </p>
                )}
              </Card>
            </li>
          )
        })}
      </ul>

      {error && <p className="text-label-sm text-error">{error}</p>}

      <div className="border-outline-variant flex items-center justify-between border-t pt-4">
        <p className="text-body-md text-on-surface-variant">
          선택 {totalQuantity}매 (최대 {MAX_TOTAL_QUANTITY}매)
        </p>
        <p className="text-title-lg text-on-surface font-semibold">
          {formatCurrency(totalAmount)}
          <span className="text-label-sm text-on-surface-variant ml-1 font-normal">
            (예약 수수료 별도, 실제 금액은 주문 생성 후 확인)
          </span>
        </p>
      </div>
    </div>
  )
}
