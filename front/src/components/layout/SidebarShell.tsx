'use client'

import { LogOut } from 'lucide-react'
import Link from 'next/link'
import { usePathname } from 'next/navigation'
import type { ComponentType, ReactNode } from 'react'

import { cn } from '@/lib/cn'

import { Header } from './Header'

export type SidebarMenuItem = {
  label: string
  href: string
  icon: ComponentType<{ className?: string }>
}

export type SidebarProfile = {
  name: string
  subtitle: string
  avatarUrl?: string
  editHref: string
}

/**
 * 마이페이지·클라이언트 포털이 공유하는 셸. Style.md 5-2 절 —
 * "둘이 완전히 같은 구조를 공유한다. 내용(메뉴 항목, 프로필 데이터)만 주입한다."
 *
 * 역할별로 다른 컴포넌트를 만들지 않는다 — `menuItems`·`profile` 로만 갈라서, 메뉴가
 * "회원 마이페이지용", "클라이언트 포털용" 처럼 겹치는 두 컴포넌트가 생기지 않게 한다.
 */
export const SidebarShell = ({
  profile,
  menuItems,
  onLogout,
  children,
}: {
  profile: SidebarProfile
  menuItems: SidebarMenuItem[]
  onLogout: () => void
  children: ReactNode
}) => {
  const pathname = usePathname()

  return (
    <div className="bg-surface min-h-screen">
      <Header />
      <div className="mx-auto flex max-w-[1280px] items-start gap-8 px-4 py-8">
        <aside className="border-outline-variant bg-surface-container-lowest sticky top-24 flex w-64 shrink-0 flex-col gap-4 rounded-md border p-6">
          <div className="flex flex-col items-center gap-1 text-center">
            <div
              className="bg-surface-container mb-2 flex h-16 w-16 items-center justify-center overflow-hidden rounded-full"
              aria-hidden
            >
              {profile.avatarUrl ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img src={profile.avatarUrl} alt="" className="h-full w-full object-cover" />
              ) : (
                <span className="text-title-lg text-on-surface-variant font-semibold">
                  {profile.name.slice(0, 1)}
                </span>
              )}
            </div>
            <p className="text-title-md text-on-surface font-semibold">{profile.name}</p>
            <p className="text-label-sm text-on-surface-variant">{profile.subtitle}</p>
            <Link href={profile.editHref} className="text-label-sm text-secondary mt-2 font-medium">
              프로필 수정
            </Link>
          </div>

          <hr className="border-outline-variant" />

          <nav className="flex flex-col gap-1">
            {menuItems.map((item) => {
              const active = pathname === item.href || pathname.startsWith(`${item.href}/`)
              const Icon = item.icon
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  className={cn(
                    'text-label-md flex items-center gap-3 rounded px-3 py-2.5 font-medium transition-colors',
                    active
                      ? 'bg-secondary-container text-on-secondary'
                      : 'text-on-surface-variant hover:bg-surface-container-low'
                  )}
                >
                  <Icon className="h-5 w-5" aria-hidden />
                  {item.label}
                </Link>
              )
            })}
          </nav>

          <hr className="border-outline-variant" />

          <button
            type="button"
            onClick={onLogout}
            className="text-label-md text-error hover:bg-error-container flex items-center gap-3 rounded px-3 py-2.5 font-medium transition-colors"
          >
            <LogOut className="h-5 w-5" aria-hidden />
            로그아웃
          </button>
        </aside>

        <main className="min-w-0 flex-1">{children}</main>
      </div>
    </div>
  )
}
