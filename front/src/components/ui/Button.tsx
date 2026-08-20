import { forwardRef } from 'react'
import type { ButtonHTMLAttributes } from 'react'

import { cn } from '@/lib/cn'

/**
 * 이 프로젝트의 유일한 버튼 컴포넌트다.
 *
 * "라운딩만 살짝 다른 버튼을 다른 컴포넌트로 만들지 않는다" — variant·size 로 갈라서
 * 화면마다 새 버튼을 만들지 않는다. 새 스타일이 필요하면 여기 variant 를 추가하지,
 * 화면 코드에 className 을 덧붙여 우회하지 않는다.
 */
const VARIANT_CLASSES = {
  // 주요 액션. Style.md 4절 "버튼(주)" — secondary 배경 + 흰 글씨
  primary: 'bg-secondary text-on-secondary hover:bg-secondary-container',
  // 보조 액션. Style.md 4절 "버튼(보조)" — 1px 아웃라인, 배경 없음
  secondary: 'border border-outline text-on-surface hover:bg-surface-container-low bg-transparent',
  // 파괴적 액션 (취소·삭제·거절)
  danger: 'bg-error text-on-error hover:opacity-90',
  // 최소 강조. 텍스트만 있는 링크형 액션
  ghost: 'bg-transparent text-secondary hover:bg-surface-container-low',
} as const

const SIZE_CLASSES = {
  sm: 'h-9 px-3 text-label-md',
  md: 'h-11 px-4 text-label-md',
  lg: 'h-12 px-6 text-title-lg',
} as const

export type ButtonVariant = keyof typeof VARIANT_CLASSES
export type ButtonSize = keyof typeof SIZE_CLASSES

export type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant
  size?: ButtonSize
  /** 제출·요청 중 상태. 클릭을 막고 텍스트 대신 스피너를 보여준다. */
  loading?: boolean
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  (
    { variant = 'primary', size = 'md', loading = false, disabled, className, children, ...props },
    ref
  ) => {
    return (
      <button
        ref={ref}
        disabled={disabled || loading}
        aria-busy={loading}
        className={cn(
          'inline-flex items-center justify-center gap-2 rounded font-medium transition-colors',
          'focus-visible:ring-secondary focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:outline-none',
          'disabled:pointer-events-none disabled:opacity-50',
          VARIANT_CLASSES[variant],
          SIZE_CLASSES[size],
          className
        )}
        {...props}
      >
        {loading && (
          <span
            className="h-4 w-4 animate-spin rounded-full border-2 border-current/30 border-t-current"
            aria-hidden
          />
        )}
        {children}
      </button>
    )
  }
)

Button.displayName = 'Button'
