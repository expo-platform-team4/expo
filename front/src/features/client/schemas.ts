import { z } from 'zod'

/**
 * 박람회 개최 신청 폼. 백엔드 `ExpoOpeningRequestPayload` 와 대응한다(Spec.md 7절).
 *
 * 행사 시작·종료일, 티켓 판매 시작·종료일 전부 `<input type="date">`(날짜만)로 받는다 —
 * 시간은 관례상 고정해서 매번 고르게 하지 않는다(제출 직전에 시간을 붙인다). 행사는 오전
 * 9시 시작·오후 9시 종료, 판매 기간은 그날 자정부터 자정 직전까지다.
 *
 * 판매 시작·종료일은 사용자가 직접 고르지 않는다 — 화면에서 행사 시작일 2주 전·행사
 * 종료일 하루 전으로 자동 계산해 채운다(읽기 전용). 그래서 여기 검증은 그 값이 실제로
 * 들어왔는지만 확인한다.
 *
 * 날짜만 비교하므로 같은 날짜(당일 행사)도 유효하다 — 종료일이 시작일보다 이르지만
 * 않으면(`>=`) 통과시킨다.
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
    categoryId: z.string().optional(),
  })
  .refine((data) => new Date(data.eventEndAt) >= new Date(data.eventStartAt), {
    message: '행사 종료일은 시작일보다 빠를 수 없습니다.',
    path: ['eventEndAt'],
  })
  .refine((data) => new Date(data.salesEndAt) > new Date(data.salesStartAt), {
    message: '판매 종료일은 시작일보다 늦어야 합니다.',
    path: ['salesEndAt'],
  })

export type ExpoOpeningRequestFormValues = z.infer<typeof expoOpeningRequestSchema>
