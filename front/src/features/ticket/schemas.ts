import { z } from 'zod'

/**
 * 백엔드 `TicketOrderItemRequest`/`MemberTicketOrderRequest`/`GuestTicketOrderRequest`/
 * `GuestTicketSearchRequest` 의 검증 규칙과 정확히 같아야 한다(Spec.md 7절). 백엔드 소스
 * (`backend/src/main/java/com/expo/ticket/dto/*.java`)를 그대로 옮겼다.
 *
 * 주의 — `GuestTicketOrderRequest.phoneNumber` 에는 `@Pattern` 이 없다(`@NotBlank @Size(max=20)`
 * 뿐이다). `features/auth/schemas.ts` 의 회원가입 휴대폰 정규식을 여기 그대로 가져오면
 * 백엔드보다 더 엄격해져서 서버는 받아 줄 값을 프론트가 먼저 막게 된다 — 옮기지 않는다.
 */

/** `MemberTicketOrderRequest.items`/`GuestTicketOrderRequest.items` 의 `@Size(max=4)`. */
const MAX_ITEMS = 4
/** `TicketOrderService` 의 `MAX_TOTAL_QUANTITY` — 항목 수와 별개로 총 수량도 4매를 넘을 수 없다. */
const MAX_TOTAL_QUANTITY = 4

/** `TicketOrderItemRequest` — `@NotNull ticketProductId`, `@Positive @Max(4) quantity`. */
export const ticketOrderItemSchema = z.object({
  ticketProductId: z.number().int().positive(),
  quantity: z
    .number()
    .int()
    .min(1, '수량은 1개 이상이어야 합니다.')
    .max(4, '한 상품당 최대 4매까지 구매할 수 있습니다.'),
})

const itemsShape = z
  .array(ticketOrderItemSchema)
  .min(1, '티켓 상품을 1개 이상 선택해 주세요.')
  .max(MAX_ITEMS, `티켓 상품은 최대 ${MAX_ITEMS}종까지 선택할 수 있습니다.`)
  .refine((items) => items.reduce((sum, item) => sum + item.quantity, 0) <= MAX_TOTAL_QUANTITY, {
    message: `주문당 총 수량은 최대 ${MAX_TOTAL_QUANTITY}매까지입니다.`,
  })

/** `MemberTicketOrderRequest`. */
export const memberOrderSchema = z.object({ items: itemsShape })
export type MemberOrderFormValues = z.infer<typeof memberOrderSchema>

/** `GuestTicketOrderRequest`. */
export const guestOrderSchema = z.object({
  items: itemsShape,
  name: z.string().min(1, '이름은 필수입니다.').max(100, '이름은 100자 이하여야 합니다.'),
  phoneNumber: z.string().min(1, '연락처는 필수입니다.').max(20, '연락처는 20자 이하여야 합니다.'),
  age: z
    .number({ error: '나이는 숫자여야 합니다.' })
    .int()
    .positive('나이는 1 이상의 숫자여야 합니다.'),
  password: z.string().min(1, '비밀번호는 필수입니다.'),
})
export type GuestOrderFormValues = z.infer<typeof guestOrderSchema>

/**
 * 티켓 상품 생성 폼. 백엔드 `TicketProductCreateRequest` 의 검증 규칙과 짝이다.
 * 일시는 `<input type="datetime-local">` 값을 그대로 받고, 제출 직전에 ISO 문자열로 바꾼다
 * (`AdminRecruitmentNoticeFormPage` 와 같은 패턴). 시작<종료 체크는 백엔드도 하지만
 * `INVALID_SALES_PERIOD` 왕복 없이 바로 알려주는 게 나아서 프론트에도 둔다.
 */
export const createTicketProductSchema = z
  .object({
    name: z.string().min(1, '상품명은 필수입니다.').max(150, '상품명은 150자 이하여야 합니다.'),
    description: z.string().optional(),
    price: z.number({ error: '가격은 숫자여야 합니다.' }).min(0, '가격은 0 이상이어야 합니다.'),
    salesStartAt: z.string().min(1, '판매 시작 일시는 필수입니다.'),
    salesEndAt: z.string().min(1, '판매 종료 일시는 필수입니다.'),
    totalQuantity: z
      .number({ error: '총 수량은 숫자여야 합니다.' })
      .int()
      .min(0, '총 수량은 0 이상이어야 합니다.'),
    maxQuantityPerOrder: z
      .number({ error: '1회 최대 구매 수량은 숫자여야 합니다.' })
      .int()
      .min(1, '1회 최대 구매 수량은 1 이상이어야 합니다.')
      .max(4, '1회 최대 구매 수량은 4 이하여야 합니다.'),
  })
  .refine((values) => new Date(values.salesStartAt) < new Date(values.salesEndAt), {
    message: '판매 시작 일시는 종료 일시보다 빨라야 합니다.',
    path: ['salesEndAt'],
  })
export type CreateTicketProductFormValues = z.infer<typeof createTicketProductSchema>

/** `GuestTicketSearchRequest`. */
export const guestOrderSearchSchema = z.object({
  orderNumber: z.string().min(1, '주문번호는 필수입니다.'),
  password: z
    .string()
    .min(1, '비밀번호는 필수입니다.')
    .max(100, '비밀번호는 100자 이하여야 합니다.'),
  phoneNumber: z.string().min(1, '연락처는 필수입니다.').max(20, '연락처는 20자 이하여야 합니다.'),
})
export type GuestOrderSearchFormValues = z.infer<typeof guestOrderSearchSchema>
