import { z } from 'zod'

/**
 * 참여 신청 폼. 백엔드 `CreateParticipationApplicationRequest` 와 필드가 대응한다.
 * `recruitmentNoticeId` 는 라우트 파라미터에서 오므로 폼 스키마에 넣지 않는다.
 */
export const applyParticipationSchema = z.object({
  companyNameSnapshot: z
    .string()
    .min(1, '기업명은 필수입니다.')
    .max(150, '기업명은 150자 이하여야 합니다.'),
  participationPurpose: z.string().optional(),
  exhibitDescription: z.string().optional(),
  /** `<select>` 값은 문자열이다. 빈 문자열이면 "선택 안 함". */
  selectedBoothProductId: z.string().optional(),
})

export type ApplyParticipationFormValues = z.infer<typeof applyParticipationSchema>
