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
 * 일시는 `<input type="datetime-local">` 값을 그대로 받고, 제출 직전에 ISO 문자열로 바꾼다
 * (`ClientRecruitmentNoticeRequestFormPage` 와 같은 패턴).
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
