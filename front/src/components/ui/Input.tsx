import { forwardRef, useId } from 'react'
import type { InputHTMLAttributes } from 'react'

import { cn } from '@/lib/cn'

/**
 * Style.md 4절 "입력 필드" — 8px 라운드, 1px 테두리. 포커스 시 secondary 테두리 + 글로우.
 *
 * 라벨과 에러 메시지를 컴포넌트 안에서 함께 다룬다. 화면마다 <label>+<input>+에러 문구를
 * 따로 조립하면 간격·타이포가 조금씩 갈린다.
 */
export type InputProps = InputHTMLAttributes<HTMLInputElement> & {
  label?: string
  error?: string
  /** React Hook Form 의 register() 반환값을 그대로 스프레드하는 용도라 name 이 필수는 아니다. */
  hint?: string
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, hint, id, className, ...props }, ref) => {
    const generatedId = useId()
    const inputId = id ?? generatedId

    return (
      <div className="flex flex-col gap-1.5">
        {label && (
          <label htmlFor={inputId} className="text-label-md text-on-surface-variant font-medium">
            {label}
          </label>
        )}
        <input
          ref={ref}
          id={inputId}
          aria-invalid={Boolean(error)}
          aria-describedby={error ? `${inputId}-error` : hint ? `${inputId}-hint` : undefined}
          className={cn(
            'border-outline-variant bg-surface-container-lowest text-body-md h-11 rounded border px-3',
            'focus:border-secondary focus:ring-secondary/20 focus:ring-2 focus:outline-none',
            'disabled:bg-surface-container-low disabled:opacity-60',
            error && 'border-error focus:border-error focus:ring-error/20',
            className
          )}
          {...props}
        />
        {error ? (
          <p id={`${inputId}-error`} className="text-label-sm text-error">
            {error}
          </p>
        ) : hint ? (
          <p id={`${inputId}-hint`} className="text-label-sm text-on-surface-variant">
            {hint}
          </p>
        ) : null}
      </div>
    )
  }
)

Input.displayName = 'Input'
