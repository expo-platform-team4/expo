import { z } from 'zod'

/**
 * 박람회 개최 신청 폼. 백엔드 `ExpoOpeningRequestPayload` 와 대응한다(Spec.md 7절).
 *
 * 일시는 `<input type="datetime-local">` 값(로컬 시각 문자열)로 받고 제출 직전에
 * `toISOString()` 으로 UTC 로 바꾼다 — `recruitment/schemas.ts` 와 같은 방식이다.
 *
 * 기간 앞뒤 관계는 DB CHECK(`ck_expo_opening_requests_event`·`_sales`)가 최종적으로 막지만,
 * 서버까지 갔다 오기 전에 걸러 주는 편이 낫다.
 */
export const expoOpeningRequestSchema = z
  .object({
    title: z
      .string()
      .min(1, '박람회명은 필수입니다.')
      .max(255, '박람회명은 255자 이하여야 합니다.'),
    description: z.string().min(1, '상세 소개는 필수입니다.'),
    eventStartAt: z.string().min(1, '행사 시작일은 필수입니다.'),
    eventEndAt: z.string().min(1, '행사 종료일은 필수입니다.'),
    salesStartAt: z.string().min(1, '판매 시작일은 필수입니다.'),
    salesEndAt: z.string().min(1, '판매 종료일은 필수입니다.'),
    desiredVenueId: z.string().min(1, '희망 장소는 필수입니다.'),
    desiredVenueHallId: z.string().optional(),
    desiredVenueZoneId: z.string().optional(),
  })
  .refine((data) => new Date(data.eventEndAt) > new Date(data.eventStartAt), {
    message: '행사 종료일은 시작일보다 늦어야 합니다.',
    path: ['eventEndAt'],
  })
  .refine((data) => new Date(data.salesEndAt) > new Date(data.salesStartAt), {
    message: '판매 종료일은 시작일보다 늦어야 합니다.',
    path: ['salesEndAt'],
  })

export type ExpoOpeningRequestFormValues = z.infer<typeof expoOpeningRequestSchema>
