'use client'

import { Bell, Building2, LayoutDashboard, ListChecks, Megaphone, Receipt, Tag } from 'lucide-react'
import type { ReactNode } from 'react'

import { ErrorState, LoadingBlock } from '@/components/ui'
import { useLogout, useMyProfile } from '@/features/auth/hooks'

import { AdminShell, type AdminMenuSection } from './AdminShell'

const ADMIN_SECTIONS: AdminMenuSection[] = [
  {
    title: '핵심 관리',
    items: [
      { label: '대시보드', href: '/admin', icon: LayoutDashboard },
      { label: '박람회 개최 승인 관리', href: '/admin/expos', icon: Building2 },
      { label: '공고 신청 관리', href: '/admin/recruitment-notice-requests', icon: ListChecks },
      { label: '공고 모집 관리', href: '/admin/recruitment-notices', icon: Megaphone },
      { label: '정산 관리', href: '/admin/settlements', icon: Receipt },
    ],
  },
  {
    title: '시스템 관리',
    items: [
      { label: '카테고리 관리', href: '/admin/categories', icon: Tag },
      { label: '알림 이력·재발송', href: '/admin/notifications', icon: Bell },
    ],
  },
]

/** `/admin/**` 전용 조립. Style.md 5-3 절 — `AdminShell` 은 구조만 두고 메뉴·프로필만 채운다. */
export const AdminShellWrapper = ({ children }: { children: ReactNode }) => {
  const { data: profile, isPending, isError, error, refetch } = useMyProfile()
  const logoutMutation = useLogout()

  if (isError) {
    return (
      <div className="mx-auto max-w-[1280px] px-4 py-8">
        <ErrorState error={error} onRetry={() => refetch()} />
      </div>
    )
  }

  if (isPending || !profile) {
    return <LoadingBlock label="프로필을 불러오는 중입니다" />
  }

  return (
    <AdminShell
      sections={ADMIN_SECTIONS}
      adminName={profile.nickname}
      onLogout={() => logoutMutation.mutate()}
    >
      {children}
    </AdminShell>
  )
}
