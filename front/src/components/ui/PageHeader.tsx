import type { ReactNode } from 'react'

export const PageHeader = ({
  title,
  description,
  action,
}: {
  title: string
  description?: string
  action?: ReactNode
}) => (
  <div className="mb-6 flex items-start justify-between gap-4">
    <div>
      <h1 className="text-headline-sm text-on-background font-semibold">{title}</h1>
      {description && <p className="text-body-md text-on-surface-variant mt-1">{description}</p>}
    </div>
    {action}
  </div>
)
