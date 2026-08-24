import { z } from 'zod'

/**
 * 부스 콘텐츠 작성·수정 폼. 백엔드 `CreateBoothContentRequest`/`UpdateBoothContentRequest` 와
 * 필드가 대응한다. 길이 제한은 `BoothContent` 엔티티의 `@Column(length = ...)` 를 그대로
 * 옮겼다 — 전용 `@Size` 검증 애너테이션은 없지만, DB 컬럼 길이를 넘기면 500 으로 깨지므로
 * 폼에서 먼저 막는다(Spec.md 7절의 "백엔드 제약과 정확히 같게" 원칙을 컬럼 길이에도 적용).
 */
export const boothContentSchema = z.object({
  companyDisplayName: z
    .string()
    .min(1, '기업 노출명은 필수입니다.')
    .max(150, '기업 노출명은 150자 이하여야 합니다.'),
  title: z
    .string()
    .min(1, '콘텐츠 제목은 필수입니다.')
    .max(200, '콘텐츠 제목은 200자 이하여야 합니다.'),
  companyDescription: z.string().optional(),
  boothDescription: z.string().optional(),
  productDescription: z.string().optional(),
})

export type BoothContentFormValues = z.infer<typeof boothContentSchema>
