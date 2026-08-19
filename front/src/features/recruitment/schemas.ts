import { z } from 'zod'

/**
 * `공고 생성 요청` 폼. 백엔드 `CreateRecruitmentNoticeRequestRequest` 와 필드가 대응한다
 * (Spec.md 7절 — 서버 검증 규칙을 그대로 옮긴다).
 *
 * 일시 입력은 `<input type="datetime-local">` 값(로컬 시각 문자열)을 그대로 받고, 제출
 * 직전에 `new Date(value).toISOString()` 으로 UTC Instant 문자열로 바꾼다(폼 값 자체는
 * 문자열로 두고, 페이지에서 변환 후 payload 를 만든다).
 *
 * `venueHallId`·`venueZoneIds` 는 원래 선택지여야 하지만, 홀·구역 목록 조회 API
 * (`/api/admin/virtual-venues/{id}/halls`, `/api/admin/venue-halls/{hallId}/zones`)가
 * ADMIN 전용이라 CLIENT 화면에서는 부를 수 없다(`recruitment/api.ts` 의 `listVirtualVenues`
 * 주석 참고). 그래서 숫자 ID 직접 입력으로 받는다.
 */
export const createNoticeRequestSchema = z.object({
  title: z.string().min(1, '제목은 필수입니다.').max(255, '제목은 255자 이하여야 합니다.'),
  description: z.string().min(1, '설명은 필수입니다.'),
  applicationStartAt: z.string().min(1, '신청 시작 일시는 필수입니다.'),
  applicationEndAt: z.string().min(1, '신청 종료 일시는 필수입니다.'),
  eventStartAt: z.string().min(1, '행사 시작 일시는 필수입니다.'),
  eventEndAt: z.string().min(1, '행사 종료 일시는 필수입니다.'),
  virtualVenueId: z.string().min(1, '가상 장소는 필수입니다.'),
  venueHallId: z
    .string()
    .min(1, '희망 전시관 ID는 필수입니다.')
    .regex(/^\d+$/, '전시관 ID는 숫자여야 합니다.'),
  venueZoneIds: z
    .string()
    .min(1, '구역 ID를 하나 이상 입력해 주세요.')
    .refine(
      (value) => value.split(',').every((part) => /^\s*\d+\s*$/.test(part)),
      '구역 ID는 쉼표(,)로 구분된 숫자여야 합니다. 예: 12,13'
    ),
  targetCompanyCount: z
    .string()
    .min(1, '목표 참가 기업 수는 필수입니다.')
    .regex(/^\d+$/, '숫자만 입력해 주세요.')
    .refine((value) => Number(value) >= 1, '목표 참가 기업 수는 1 이상이어야 합니다.'),
  requestedBoothConfig: z.string().optional(),
})

export type CreateNoticeRequestFormValues = z.infer<typeof createNoticeRequestSchema>
