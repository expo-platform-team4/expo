import { z } from 'zod'

/**
 * 티켓 코드 수동 입력. 백엔드 `ManualCheckInRequest` 는 `@NotBlank` 뿐이다(길이·형식 제약
 * 없음) — Zod 도 그 이상으로 엄격하게 만들지 않는다(Spec.md 7절).
 */
export const manualCheckInSchema = z.object({
  ticketCode: z.string().trim().min(1, '티켓 코드를 입력해 주세요.'),
})
export type ManualCheckInFormValues = z.infer<typeof manualCheckInSchema>
