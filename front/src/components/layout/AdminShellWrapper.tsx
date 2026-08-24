'use client'

import {
  Bell,
  Building2,
  LayoutDashboard,
  ListChecks,
  MapPin,
  Megaphone,
  Receipt,
  Tag,
} from 'lucide-react'
import type { ReactNode } from 'react'

import { useLogout } from '@/features/auth/hooks'
import { useAuthStore } from '@/lib/auth'

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
      { label: '가상 장소 관리', href: '/admin/venues', icon: MapPin },
      { label: '알림 이력·재발송', href: '/admin/notifications', icon: Bell },
    ],
  },
]

/**
 * `/admin/**` 전용 조립. Style.md 5-3 절 — `AdminShell` 은 구조만 두고 메뉴·프로필만 채운다.
 *
 * `useMyProfile()`(`GET /api/users/me/profile`)을 쓰지 않는다 — `SecurityConfig` 가
 * `/api/users/**` 를 `hasAnyRole("MEMBER","CLIENT")` 로만 열어 둬서 ADMIN 계정은 401 을
 * 받는다(관리자 전용 "내 프로필" 엔드포인트가 따로 없다). 로그인 응답에 이미 실려 오는
 * `nickname` 을 store 에서 바로 읽는 것으로 충분하다 — 추가 API 호출이 필요 없다.
 */
export const AdminShellWrapper = ({ children }: { children: ReactNode }) => {
  const nickname = useAuthStore((state) => state.nickname)
  const logoutMutation = useLogout()

  return (
    <AdminShell
      sections={ADMIN_SECTIONS}
      adminName={nickname ?? '관리자'}
      onLogout={() => logoutMutation.mutate()}
    >
      {children}
    </AdminShell>
  )
}
