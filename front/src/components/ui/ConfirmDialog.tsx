'use client'

import { Button } from './Button'
import { Card } from './Card'


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
  if (!open) return null

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4"
      role="dialog"
      aria-modal="true"
    >
      <Card className="w-full max-w-sm">
        <h2 className="text-title-md text-on-surface mb-2 font-semibold">{title}</h2>
        {description && <p className="text-body-sm text-on-surface-variant mb-4">{description}</p>}
        <div className="mt-4 flex justify-end gap-2">
          <Button type="button" variant="secondary" size="sm" onClick={onCancel}>
            {cancelLabel}
          </Button>
          <Button
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
