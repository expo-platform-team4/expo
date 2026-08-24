import type { HTMLAttributes } from 'react'

import { cn } from '@/lib/cn'

/**
 * Style.md 4절 "카드" — 흰 배경(surface-container-lowest) + Level 2 그림자.
 * 그림자 값은 디자인 시스템 원문을 그대로 옮겼다. 화면마다 shadow-lg 등을 임의로 쓰지 않는다.
 */
const LEVEL_2_SHADOW =
  'shadow-[0_4px_6px_-1px_rgba(26,43,75,0.05),0_2px_4px_-1px_rgba(26,43,75,0.03)]'

export const Card = ({ className, ...props }: HTMLAttributes<HTMLDivElement>) => (
  <div
    className={cn('bg-surface-container-lowest rounded-md p-6', LEVEL_2_SHADOW, className)}
    {...props}
  />
)

/** 카드 제목. Style.md 가 지정한 대로 primary 색을 쓴다. */
export const CardTitle = ({ className, ...props }: HTMLAttributes<HTMLHeadingElement>) => (
  <h3 className={cn('text-title-lg text-primary mb-2 font-semibold', className)} {...props} />
)
