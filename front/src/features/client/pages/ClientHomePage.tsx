'use client'

import { Building2, LayoutDashboard, Megaphone, Receipt, ScanLine, Users } from 'lucide-react'
import Link from 'next/link'

import { Card, CardTitle, PageHeader } from '@/components/ui'

import { useClientDashboardProfile } from '../hooks'

const QUICK_LINKS = [
  {
    href: '/client/dashboard',
    icon: LayoutDashboard,
    title: '대시보드',
    description: '등록 박람회·부스·모집공고 요약 지표를 확인합니다',
  },
  {
    href: '/client/expos',
    icon: Building2,
    title: '내 박람회',
    description: '내가 연 박람회 목록과 매출 요약을 확인합니다',
  },
  {
    href: '/client/recruitment-notice-requests',
    icon: Megaphone,
    title: '모집공고 요청 현황',
    description: '내 요청 목록과 진행 상태를 확인합니다',
  },
  {
    href: '/client/participations',
    icon: Users,
    title: '참여 신청 내역',
    description: '내가 신청한 부스 참여 내역을 확인합니다',
  },
  {
    href: '/client/settlements',
    icon: Receipt,
    title: '정산 리포트',
    description: '티켓·부스 매출 정산 리포트를 확인합니다',
  },
  {
    href: '/client/check-in',
    icon: ScanLine,
    title: '체크인 현황',
    description: '오늘 체크인 현황을 확인하고 QR 을 스캔합니다',
  },
] as const

/**
 * `/client`. 주최사 포털 진입점.
 *
 * `/client/dashboard` 는 실제 요약 "지표"(숫자)를 보여주는 화면이라 따로 둔다. 여기는
 * `/mypage`(회원 포털 홈)가 이미 쓰고 있는 "가벼운 랜딩 + 바로가기 카드" 패턴을 그대로
 * 재사용한다 — 두 포털이 같은 Sidebar 셸을 쓰는 구조적 짝이라 구분을 다르게 둘 이유가 없다.
 */
const ClientHomePage = () => {
  const { data: profile } = useClientDashboardProfile()

  return (
    <div>
      <PageHeader
        title="클라이언트 홈"
        description={profile ? `${profile.companyName}님, 환영합니다.` : undefined}
      />
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {QUICK_LINKS.map(({ href, icon: Icon, title, description }) => (
          <Link key={href} href={href}>
            <Card className="hover:border-secondary flex h-full items-start gap-4 border border-transparent transition-colors">
              <Icon className="text-secondary h-8 w-8 shrink-0" aria-hidden />
              <div>
                <CardTitle className="mb-1">{title}</CardTitle>
                <p className="text-body-md text-on-surface-variant">{description}</p>
              </div>
            </Card>
          </Link>
        ))}
      </div>
    </div>
  )
}

export default ClientHomePage
