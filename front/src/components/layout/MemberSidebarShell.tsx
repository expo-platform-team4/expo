'use client'

import { Receipt, Ticket } from 'lucide-react'
import type { ReactNode } from 'react'

import { ErrorState, LoadingBlock } from '@/components/ui'
import { useLogout, useMyProfile } from '@/features/auth/hooks'

import { SidebarShell, type SidebarMenuSection } from './SidebarShell'

const MEMBER_MENU_SECTIONS: SidebarMenuSection[] = [
  {
    items: [
      { label: '예매 내역', href: '/mypage/orders', icon: Receipt },
      { label: '나의 티켓', href: '/mypage/tickets', icon: Ticket },
    ],
  },
]

/** `/mypage/**` 전용 조립. `SidebarShell` 은 그대로 두고 프로필·메뉴만 회원용으로 채운다. */
export const MemberSidebarShell = ({ children }: { children: ReactNode }) => {
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
    <SidebarShell
      profile={{
        name: profile.nickname,
        subtitle: profile.phoneNumber ?? '전화번호 미등록',
        editHref: '/mypage/profile',
        avatarUrl: profile.profileImageFileId
          ? `/api/files/${profile.profileImageFileId}/content`
          : undefined,
      }}
      sections={MEMBER_MENU_SECTIONS}
      onLogout={() => logoutMutation.mutate()}
    >
      {children}
    </SidebarShell>
  )
}
