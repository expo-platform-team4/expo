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

import { SidebarShell, type SidebarMenuSection } from './SidebarShell'

/**
 * CLIENT 계정은 "박람회 주최사"와 "부스 참가기업" 두 역할을 겸한다 — 같은 계정으로 자기
 * 박람회도 열고, 남의 박람회에 부스로 참가도 한다. 이 둘을 섞어서 나열하면 지금 내가 뭘
 * 하려는 화면인지 헷갈려서, 역할별로 묶고 그 안에서는 실제 진행 순서대로 둔다.
 */
const CLIENT_MENU_SECTIONS: SidebarMenuSection[] = [
  { items: [{ label: '대시보드', href: '/client/dashboard', icon: LayoutDashboard }] },
  {
    title: '주최사 기능',
    items: [
      { label: '내 박람회', href: '/client/expos', icon: Building2 },
      {
        label: '모집공고 요청 현황',
        href: '/client/recruitment-notice-requests',
        icon: Megaphone,
      },
      { label: '체크인 현황', href: '/client/check-in', icon: ScanLine },
    ],
  },
  {
    title: '참가기업 기능',
    items: [
      { label: '부스 관리', href: '/client/booths', icon: Store },
      { label: '참여 신청 내역', href: '/client/participations', icon: Users },
    ],
  },
  {
    title: '기타',
    items: [
      { label: '배너 노출 신청', href: '/client/banner-requests', icon: ImageIcon },
      { label: '정산 리포트', href: '/client/settlements', icon: Receipt },
    ],
  },
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
      sections={CLIENT_MENU_SECTIONS}
      onLogout={() => logoutMutation.mutate()}
    >
      {children}
    </SidebarShell>
  )
}
