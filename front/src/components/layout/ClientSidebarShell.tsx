'use client'

import {
  Building2,
  ImageIcon,
  LayoutDashboard,
  Megaphone,
  Receipt,
  ScanLine,
  Store,
  Users,
} from 'lucide-react'
import type { ReactNode } from 'react'

import { ErrorState, LoadingBlock } from '@/components/ui'
import { useLogout } from '@/features/auth/hooks'
import { useClientDashboardProfile } from '@/features/client/hooks'

import { SidebarShell, type SidebarMenuItem } from './SidebarShell'

const CLIENT_MENU_ITEMS: SidebarMenuItem[] = [
  { label: '대시보드', href: '/client/dashboard', icon: LayoutDashboard },
  { label: '내 박람회', href: '/client/expos', icon: Building2 },
  { label: '부스 관리', href: '/client/booths', icon: Store },
  { label: '모집공고 요청 현황', href: '/client/recruitment-notice-requests', icon: Megaphone },
  { label: '배너 노출 신청', href: '/client/banner-requests', icon: ImageIcon },
  { label: '참여 신청 내역', href: '/client/participations', icon: Users },
  { label: '정산 리포트', href: '/client/settlements', icon: Receipt },
  { label: '체크인 현황', href: '/client/check-in', icon: ScanLine },
]

/** `/client/**` 전용 조립. `MemberSidebarShell` 과 `SidebarShell` 은 공유하고 데이터 출처만 다르다. */
export const ClientSidebarShell = ({ children }: { children: ReactNode }) => {
  const { data: profile, isPending, isError, error, refetch } = useClientDashboardProfile()
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
    <SidebarShell
      profile={{
        name: profile.nickname,
        subtitle: profile.companyName,
        editHref: '/client/profile',
      }}
      menuItems={CLIENT_MENU_ITEMS}
      onLogout={() => logoutMutation.mutate()}
    >
      {children}
    </SidebarShell>
  )
}
