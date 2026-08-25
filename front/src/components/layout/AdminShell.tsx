'use client'

import { LogOut } from 'lucide-react'
import Link from 'next/link'
import { usePathname } from 'next/navigation'
import type { ComponentType, ReactNode } from 'react'

import { cn } from '@/lib/cn'

export type AdminMenuItem = {
  label: string
  href: string
  icon: ComponentType<{ className?: string }>
}

export type AdminMenuSection = {
  title?: string
  items: AdminMenuItem[]
}

/**
 * 관리자 전용 셸. Style.md 5-3 절 — "색만 바꾼 변형이 아니다. 구조 자체가 다르다."
 *
 * `SidebarShell` 과 억지로 합치지 않는다: 로고가 사이드바 안에 있고, 상단 글로벌 내비가
 * 없고, 프로필이 사이드바 "하단"에 있다 — 세 가지 모두 `SidebarShell` 과 반대라 props 로
 * 갈라 쓰면 조건문이 더 늘어난다.
 */
export const AdminShell = ({
  sections,
  adminName,
  onLogout,
  children,
}: {
  sections: AdminMenuSection[]
  adminName: string
  onLogout: () => void
  children: ReactNode
}) => {
  const pathname = usePathname()

  return (
    <div className="bg-surface flex min-h-screen">
      <aside className="flex w-64 shrink-0 flex-col gap-6 bg-[#031635] p-6 text-white">
        <Link href="/admin" className="text-title-lg font-bold text-white">
          엑스포티켓 관리자
        </Link>

        <nav className="flex flex-1 flex-col gap-6">
          {sections.map((section, index) => (
            <div key={section.title ?? index} className="flex flex-col gap-1">
              {section.title && (
                <p className="text-label-sm px-3 font-semibold text-white/50">{section.title}</p>
              )}
              {section.items.map((item) => {
                const active = pathname === item.href || pathname.startsWith(`${item.href}/`)
                const Icon = item.icon
                return (
                  <Link
                    key={item.href}
                    href={item.href}
                    className={cn(
                      'text-label-md flex items-center gap-3 rounded px-3 py-2.5 font-medium transition-colors',
                      active
                        ? 'bg-white/15 text-white'
                        : 'text-white/70 hover:bg-white/10 hover:text-white'
                    )}
                  >
                    <Icon className="h-5 w-5" aria-hidden />
                    {item.label}
                  </Link>
                )
              })}
            </div>
          ))}
        </nav>

        <div className="flex flex-col gap-3 border-t border-white/15 pt-4">
          <div>
            <p className="text-label-md font-semibold text-white">{adminName}</p>
            <p className="text-label-sm text-white/50">관리자 계정</p>
          </div>
          <button
            type="button"
            onClick={onLogout}
            className="text-label-md flex items-center gap-3 rounded px-3 py-2 font-medium text-white/70 transition-colors hover:bg-white/10 hover:text-white"
          >
            <LogOut className="h-5 w-5" aria-hidden />
            로그아웃
          </button>
        </div>
      </aside>

      <main className="min-w-0 flex-1 p-8">{children}</main>
    </div>
  )
}
