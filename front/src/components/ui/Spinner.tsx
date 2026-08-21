import { cn } from '@/lib/cn'

const SIZE_CLASSES = {
  sm: 'h-4 w-4 border-2',
  md: 'h-6 w-6 border-2',
  lg: 'h-10 w-10 border-[3px]',
} as const

export const Spinner = ({
  size = 'md',
  className,
}: {
  size?: keyof typeof SIZE_CLASSES
  className?: string
}) => (
  <span
    role="status"
    aria-label="불러오는 중"
    className={cn(
      'border-outline-variant border-t-secondary animate-spin rounded-full',
      SIZE_CLASSES[size],
      className
    )}
  />
)

/** 페이지·카드 하나를 통째로 로딩 상태로 채울 때. 화면마다 "불러오는 중..." 문구를 새로 짓지 않는다. */
export const LoadingBlock = ({ label = '불러오는 중입니다' }: { label?: string }) => (
  <div className="flex flex-col items-center justify-center gap-3 py-16">
    <Spinner size="lg" />
    <p className="text-body-md text-on-surface-variant">{label}</p>
  </div>
)
