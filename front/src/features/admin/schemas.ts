import { z } from 'zod'

/**
 * 카테고리 등록 폼. 백엔드 `CategoryCreateRequest` 의 `@NotBlank`·`@Size(max=100)` 를 그대로
 * 옮긴다(Spec.md 7절 — 정규식/길이 규칙은 백엔드와 정확히 같아야 한다).
 */
export const createCategorySchema = z.object({
  name: z
    .string()
    .min(1, '카테고리명은 필수입니다.')
    .max(100, '카테고리명은 100자 이하여야 합니다.'),
  sortOrder: z.string().optional(),
})

export type CreateCategoryFormValues = z.infer<typeof createCategorySchema>

/**
 * 카테고리 수정 폼. 백엔드 `CategoryUpdateRequest` 의 `@Size(max=100)` +
 * `@Pattern(".*\\S.*")`(공백만으로 이루어질 수 없음)를 그대로 옮긴다.
 */
export const updateCategorySchema = z.object({
  name: z
    .string()
    .max(100, '카테고리명은 100자 이하여야 합니다.')
    .regex(/.*\S.*/, '카테고리명은 공백만으로 이루어질 수 없습니다.')
    .optional()
    .or(z.literal('')),
  sortOrder: z.string().optional(),
  active: z.boolean(),
})

export type UpdateCategoryFormValues = z.infer<typeof updateCategorySchema>

/**
 * 장소 충돌 판정 폼. 백엔드 `DecideVenueRequest` — `decision` 만 필수다.
 */
export const decideVenueSchema = z.object({
  decision: z.enum(['ALLOWED', 'CANCELED'], { message: '장소 결정을 선택해 주세요.' }),
  reason: z.string().optional(),
})

export type DecideVenueFormValues = z.infer<typeof decideVenueSchema>

/**
 * 확정 장소 예약 생성 폼. 백엔드 `CreateVenueReservationRequest` 와 대응한다. 일시는
 * `<input type="datetime-local">` 값을 그대로 받고, 제출 직전에 ISO 문자열로 바꾼다.
 */
export const createVenueReservationSchema = z.object({
  useStartAt: z.string().min(1, '사용 시작 일시는 필수입니다.'),
  useEndAt: z.string().min(1, '사용 종료 일시는 필수입니다.'),
})

export type CreateVenueReservationFormValues = z.infer<typeof createVenueReservationSchema>

/**
 * 기업 모집 공고 초안 생성 폼. 백엔드 `CreateRecruitmentNoticeRequest`(admin dto) 와 대응한다.
 * 일시는 `<input type="datetime-local">` 값을 그대로 받고, 제출 직전에 ISO 문자열로 바꾼다.
 */
export const createAdminNoticeSchema = z.object({
  requestId: z.string().min(1, '근거 요청을 선택해 주세요.'),
  title: z.string().min(1, '제목은 필수입니다.').max(255, '제목은 255자 이하여야 합니다.'),
  content: z.string().min(1, '내용은 필수입니다.'),
  eligibility: z.string().optional(),
  submissionRequirements: z.string().optional(),
  applicationStartAt: z.string().min(1, '신청 시작 일시는 필수입니다.'),
  applicationEndAt: z.string().min(1, '신청 종료 일시는 필수입니다.'),
})

/**
 * 모집공고 생성 요청 작성 폼. 백엔드 `CreateRecruitmentNoticeRequestRequest` 와 대응한다.
 * 이전엔 CLIENT가 직접 작성했지만(구 `ClientRecruitmentNoticeRequestFormPage`), 이제는
 * 관리자가 승인된 박람회를 골라 대신 작성한다 — 그래서 `hostClientId` 대신 `expoId`를 받는다.
 */
export const createAdminNoticeRequestSchema = z
  .object({
    expoId: z.string().min(1, '박람회를 선택해 주세요.'),
    title: z.string().min(1, '제목은 필수입니다.').max(255, '제목은 255자 이하여야 합니다.'),
    description: z.string().min(1, '설명은 필수입니다.'),
    applicationStartAt: z.string().min(1, '신청 시작 일시는 필수입니다.'),
    applicationEndAt: z.string().min(1, '신청 종료 일시는 필수입니다.'),
    eventStartAt: z.string().min(1, '행사 시작 일시는 필수입니다.'),
    eventEndAt: z.string().min(1, '행사 종료 일시는 필수입니다.'),
    virtualVenueId: z.string().min(1, '가상 장소는 필수입니다.'),
    venueHallId: z.string().min(1, '희망 전시관은 필수입니다.'),
    venueZoneIds: z.array(z.string()).min(1, '희망 구역을 하나 이상 선택해 주세요.'),
    targetCompanyCount: z
      .string()
      .min(1, '목표 참가 기업 수는 필수입니다.')
      .regex(/^\d+$/, '숫자만 입력해 주세요.')
      .refine((value) => Number(value) >= 1, '목표 참가 기업 수는 1 이상이어야 합니다.'),
  })
  // 신청 마감이 행사 시작보다 늦으면 행사가 시작한 뒤에야 모집을 마감하는 꼴이 된다 — 백엔드
  // `APPLICATION_PERIOD_EXCEEDS_EVENT_PERIOD` 검증과 같은 규칙이다.
  .refine(
    (values) =>
      !values.applicationEndAt ||
      !values.eventStartAt ||
      new Date(values.applicationEndAt) <= new Date(values.eventStartAt),
    {
      message: '신청 종료 일시는 행사 시작 일시보다 늦을 수 없습니다.',
      path: ['applicationEndAt'],
    }
  )

export type CreateAdminNoticeRequestFormValues = z.infer<typeof createAdminNoticeRequestSchema>

export type CreateAdminNoticeFormValues = z.infer<typeof createAdminNoticeSchema>

/**
 * 송금 결과 기록 폼. 백엔드 `RemittanceRecordRequest` — `status` 만 필수다.
 */
export const recordRemittanceSchema = z.object({
  status: z.enum(['REMITTED', 'FAILED', 'PENDING', 'PROCESSING', 'CANCELED'], {
    message: '송금 상태를 선택해 주세요.',
  }),
  remittedAmount: z
    .string()
    .optional()
    .refine(
      (value) => !value || (/^\d+$/.test(value) && Number(value) >= 0),
      '송금액은 0 이상의 숫자여야 합니다.'
    ),
  referenceNumber: z.string().max(100, '거래번호는 100자 이하여야 합니다.').optional(),
  memo: z.string().optional(),
})

export type RecordRemittanceFormValues = z.infer<typeof recordRemittanceSchema>
