import type { HTMLAttributes } from 'react'

import { cn } from '@/lib/cn'

/**
 * Style.md 4절 "뱃지·칩".
 *
 *   success  성공/확정/체크인 완료 — tertiary-container 배경 (Soft Mint)
 *   neutral  카테고리 태그 — 연한 회색
 *   error    거절·실패·만료
 *   info     대기·검토중 (secondary 계열, 성공도 실패도 아닌 진행 상태)
 */
const VARIANT_CLASSES = {
  success: 'bg-tertiary-fixed/25 text-tertiary',
  neutral: 'bg-surface-container-high text-on-surface-variant',
  error: 'bg-error-container text-on-error-container',
  info: 'bg-secondary-container/20 text-secondary',
} as const

export type BadgeVariant = keyof typeof VARIANT_CLASSES

export type BadgeProps = HTMLAttributes<HTMLSpanElement> & {
  variant?: BadgeVariant
}

export const Badge = ({ variant = 'neutral', className, ...props }: BadgeProps) => (
  <span
    className={cn(
      'text-label-sm inline-flex items-center rounded-full px-2.5 py-1 font-semibold',
      VARIANT_CLASSES[variant],
      className
    )}
    {...props}
  />
)
