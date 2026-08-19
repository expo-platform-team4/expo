import { forwardRef, useId } from 'react'
import type { ReactNode, SelectHTMLAttributes } from 'react'

import { cn } from '@/lib/cn'

/** Style.md 4절 "입력 필드" 스타일을 그대로 따르는 드롭다운. `Input` 과 짝이다. */
export type SelectProps = SelectHTMLAttributes<HTMLSelectElement> & {
  label?: string
  error?: string
  hint?: string
  children: ReactNode
}

export const Select = forwardRef<HTMLSelectElement, SelectProps>(
  ({ label, error, hint, id, className, children, ...props }, ref) => {
    const generatedId = useId()
    const fieldId = id ?? generatedId

    return (
      <div className="flex flex-col gap-1.5">
        {label && (
          <label htmlFor={fieldId} className="text-label-md text-on-surface-variant font-medium">
            {label}
          </label>
        )}
        <select
          ref={ref}
          id={fieldId}
          aria-invalid={Boolean(error)}
          className={cn(
            'border-outline-variant bg-surface-container-lowest text-body-md h-11 rounded border px-3',
            'focus:border-secondary focus:ring-secondary/20 focus:ring-2 focus:outline-none',
            'disabled:bg-surface-container-low disabled:opacity-60',
            error && 'border-error focus:border-error focus:ring-error/20',
            className
          )}
          {...props}
        >
          {children}
        </select>
        {error ? (
          <p className="text-label-sm text-error">{error}</p>
        ) : hint ? (
          <p className="text-label-sm text-on-surface-variant">{hint}</p>
        ) : null}
      </div>
    )
  }
)

Select.displayName = 'Select'
