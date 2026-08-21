import { Check } from 'lucide-react'
import { forwardRef, useId } from 'react'
import type { InputHTMLAttributes, ReactNode } from 'react'

import { cn } from '@/lib/cn'

/**
 * 약관 동의·다중 선택에 쓰는 유일한 체크박스. 회원가입 두 화면(일반·기업)이 약관 3개를
 * 각각 손으로 그리면 겉모습이 미묘하게 갈린다 — 여기 하나로 통일한다.
 */
export type CheckboxProps = Omit<InputHTMLAttributes<HTMLInputElement>, 'type'> & {
  label: ReactNode
  error?: string
}

export const Checkbox = forwardRef<HTMLInputElement, CheckboxProps>(
  ({ label, error, id, className, ...props }, ref) => {
    const generatedId = useId()
    const inputId = id ?? generatedId

    return (
      <div className="flex flex-col gap-1">
        <label htmlFor={inputId} className="group flex cursor-pointer items-center gap-2">
          <span className="relative inline-flex h-5 w-5 shrink-0 items-center justify-center">
            <input
              ref={ref}
              id={inputId}
              type="checkbox"
              aria-invalid={Boolean(error)}
              className={cn(
                'peer border-outline size-5 appearance-none rounded border',
                'checked:border-secondary checked:bg-secondary',
                'focus-visible:ring-secondary focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:outline-none',
                className
              )}
              {...props}
            />
            <Check
              className="text-on-secondary pointer-events-none absolute h-3.5 w-3.5 opacity-0 peer-checked:opacity-100"
              aria-hidden
            />
          </span>
          <span className="text-label-md text-on-surface">{label}</span>
        </label>
        {error && <p className="text-label-sm text-error ml-7">{error}</p>}
      </div>
    )
  }
)

Checkbox.displayName = 'Checkbox'
