import { z } from 'zod'

/**
 * 배너 노출 신청 폼. 백엔드 `BannerApplicationCreateRequest` 와 필드가 대응한다
 * (Spec.md 7절 — 서버 검증 규칙을 그대로 옮긴다).
 *
 * 일시는 `<input type="datetime-local">` 값(로컬 시각 문자열)을 그대로 받고, 제출 직전에
 * `new Date(value).toISOString()` 으로 바꾼다.
 *
 * 이미지 파일 ID 는 폼 필드로 두되 사용자가 입력하지 않는다 — 파일을 고르면 먼저 업로드하고
 * 돌려받은 `fileId` 를 `setValue` 로 넣는다. **업로드가 끝나야 신청할 수 있다**는 뜻이라,
 * 값이 비면 "이미지를 올려 주세요" 로 막힌다.
 */
export const createBannerRequestSchema = z
  .object({
    expoId: z.string().min(1, '홍보할 박람회를 선택해 주세요.'),
    imageFileId: z.string().min(1, '배너 이미지를 올려 주세요.'),
    headline: z.string().max(150, '배너 문구는 150자 이하여야 합니다.').optional(),
    requestedStartAt: z.string().min(1, '희망 노출 시작일시는 필수입니다.'),
    requestedEndAt: z.string().min(1, '희망 노출 종료일시는 필수입니다.'),
  })
  // 서버도 막지만(@Future), 시작일이 과거면 400 을 받고 나서야 알게 된다. 미리 잡는다.
  .refine((values) => new Date(values.requestedStartAt) > new Date(), {
    path: ['requestedStartAt'],
    message: '희망 노출 시작일시는 미래여야 합니다.',
  })
  .refine((values) => new Date(values.requestedEndAt) > new Date(values.requestedStartAt), {
    path: ['requestedEndAt'],
    message: '종료일시는 시작일시보다 뒤여야 합니다.',
  })

export type CreateBannerRequestFormValues = z.infer<typeof createBannerRequestSchema>

/** 반려 사유. 백엔드 `BannerApplicationRejectRequest` 와 같은 규칙이다. */
export const rejectBannerSchema = z.object({
  reason: z
    .string()
    .min(1, '반려 사유는 필수입니다.')
    .max(1000, '반려 사유는 1000자 이하여야 합니다.'),
})

export type RejectBannerFormValues = z.infer<typeof rejectBannerSchema>
