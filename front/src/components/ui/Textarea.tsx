import { forwardRef, useId } from 'react'
import type { TextareaHTMLAttributes } from 'react'

import { cn } from '@/lib/cn'

/** Style.md 4절 "입력 필드" 스타일을 그대로 따르는 여러 줄 입력. `Input` 과 짝이다. */
export type TextareaProps = TextareaHTMLAttributes<HTMLTextAreaElement> & {
  label?: string
  error?: string
  hint?: string
}

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(
  ({ label, error, hint, id, className, rows = 4, ...props }, ref) => {
    const generatedId = useId()
    const fieldId = id ?? generatedId

    return (
      <div className="flex flex-col gap-1.5">
        {label && (
          <label htmlFor={fieldId} className="text-label-md text-on-surface-variant font-medium">
            {label}
          </label>
        )}
        <textarea
          ref={ref}
          id={fieldId}
          rows={rows}
          aria-invalid={Boolean(error)}
          className={cn(
            'border-outline-variant bg-surface-container-lowest text-body-md rounded border px-3 py-2',
            'focus:border-secondary focus:ring-secondary/20 focus:ring-2 focus:outline-none',
            'disabled:bg-surface-container-low disabled:opacity-60',
            error && 'border-error focus:border-error focus:ring-error/20',
            className
          )}
          {...props}
        />
        {error ? (
          <p className="text-label-sm text-error">{error}</p>
        ) : hint ? (
          <p className="text-label-sm text-on-surface-variant">{hint}</p>
        ) : null}
      </div>
    )
  }
)

Textarea.displayName = 'Textarea'
