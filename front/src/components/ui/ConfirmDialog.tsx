'use client'

import { useEffect, useId, useRef } from 'react'

import { Button } from './Button'
import { Card } from './Card'

/**
 * `window.confirm` 대체용 커스텀 확인 모달. 브라우저 네이티브 확인창은 스타일을 못 입혀
 * 화면 톤과 안 맞는다 — 탈퇴처럼 되돌릴 수 없는 액션에서 한 번 더 확인받을 때 쓴다.
 *
 * 접근성: `aria-modal`만으로는 포커스가 배경 요소로 새는 걸 막지 못한다 — 열릴 때 취소
 * 버튼으로 포커스를 옮기고(파괴적 액션이라 기본 포커스를 더 안전한 쪽에 둔다), Tab/Shift+Tab을
 * 모달 안의 두 버튼 사이에서만 돌게 가둔다. 닫히면 열기 전 포커스를 되돌린다.
 */
export const ConfirmDialog = ({
  open,
  title,
  description,
  confirmLabel = '확인',
  cancelLabel = '취소',
  danger = false,
  loading = false,
  onConfirm,
  onCancel,
}: {
  open: boolean
  title: string
  description?: string
  confirmLabel?: string
  cancelLabel?: string
  danger?: boolean
  loading?: boolean
  onConfirm: () => void
  onCancel: () => void
}) => {
  const titleId = useId()
  const cancelRef = useRef<HTMLButtonElement>(null)
  const confirmRef = useRef<HTMLButtonElement>(null)
  const previousFocusRef = useRef<HTMLElement | null>(null)

  useEffect(() => {
    if (!open) return

    previousFocusRef.current = document.activeElement as HTMLElement | null
    cancelRef.current?.focus()

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key !== 'Tab') return
      const first = cancelRef.current
      const last = confirmRef.current
      if (!first || !last) return

      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault()
        last.focus()
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault()
        first.focus()
      }
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => {
      document.removeEventListener('keydown', handleKeyDown)
      previousFocusRef.current?.focus()
    }
  }, [open])

  if (!open) return null

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby={titleId}
    >
      <Card className="w-full max-w-sm">
        <h2 id={titleId} className="text-title-md text-on-surface mb-2 font-semibold">
          {title}
        </h2>
        {description && <p className="text-body-sm text-on-surface-variant mb-4">{description}</p>}
        <div className="mt-4 flex justify-end gap-2">
          <Button ref={cancelRef} type="button" variant="secondary" size="sm" onClick={onCancel}>
            {cancelLabel}
          </Button>
          <Button
            ref={confirmRef}
            type="button"
            variant={danger ? 'outline-danger' : 'outline-primary'}
            size="sm"
            loading={loading}
            onClick={onConfirm}
          >
            {confirmLabel}
          </Button>
        </div>
      </Card>
    </div>
  )
}
