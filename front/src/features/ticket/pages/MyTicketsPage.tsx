'use client'

import { ChevronLeft, ChevronRight } from 'lucide-react'
import Link from 'next/link'
import { QRCodeSVG } from 'qrcode.react'
import { useState } from 'react'

import { Badge, Card, EmptyState, ErrorState, LoadingBlock, PageHeader } from '@/components/ui'
import type { BadgeVariant } from '@/components/ui'
import { formatDate, formatDateTime } from '@/lib/date'

import type { MemberTicket, MemberTicketGroup } from '../api'
import { useMyTickets } from '../hooks'

const STATUS_LABEL: Record<MemberTicket['status'], string> = {
  ISSUED: '입장 가능',
  CHECKED_IN: '입장 완료',
  CANCELED: '취소된 티켓',
  INVALIDATED: '사용할 수 없는 티켓',
}

const STATUS_VARIANT: Record<MemberTicket['status'], BadgeVariant> = {
  ISSUED: 'success',
  CHECKED_IN: 'neutral',
  CANCELED: 'neutral',
  INVALIDATED: 'error',
}

/**
 * `/mypage/tickets`. A-API-021, A-API-022 — `GET /api/users/me/tickets`.
 *
 * 카드 1개 = 박람회 1개. 오른쪽 QR 영역은 그 박람회에서 산 티켓이 여러 장이면 좌우 화살표로
 * 넘긴다. 제목만 박람회 상세(`/expos/{expoId}`)로 가는 링크다 — QR·사진 영역은 눌러도
 * 아무 데도 가지 않는다(입장 직전에 QR 보려다 실수로 페이지를 벗어나면 안 된다).
 */
const MyTicketsPage = () => {
  const { data, isPending, error, refetch } = useMyTickets()

  return (
    <div>
      <PageHeader title="나의 티켓" description="발권된 티켓과 QR 을 확인합니다." />

      {isPending && <LoadingBlock label="나의 티켓을 불러오는 중입니다" />}
      {error && <ErrorState error={error} onRetry={refetch} />}
      {!isPending && !error && data && data.length === 0 && (
        <EmptyState title="발권된 티켓이 없습니다" description="아직 발권된 티켓이 없습니다." />
      )}
      {!isPending && !error && data && data.length > 0 && (
        <ul className="space-y-4">
          {data.map((group) => (
            <li key={group.expoId}>
              <ExpoTicketCard group={group} />
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

const ExpoTicketCard = ({ group }: { group: MemberTicketGroup }) => {
  const [index, setIndex] = useState(0)
  const ticketCount = group.tickets.length
  // 카드는 expoId 로 유지되는데, refetch 로 마지막으로 보던 티켓이 목록에서 빠질 수 있다
  // (예: 취소 처리). index 를 그대로 쓰면 undefined 를 가리켜 아래에서 터진다 — 항상
  // 현재 목록 범위 안으로 잘라서 쓴다.
  const activeIndex = Math.min(index, ticketCount - 1)
  const ticket = group.tickets[activeIndex]
  const hasMultiple = ticketCount > 1

  return (
    <Card>
      <div className="flex flex-col gap-6 sm:flex-row sm:items-center sm:justify-between">
        <div className="min-w-0">
          <Link href={`/expos/${group.expoId}`} className="hover:underline">
            <p className="text-title-lg text-primary font-semibold">{group.expoTitle}</p>
          </Link>
          <p className="text-body-sm text-on-surface-variant mt-1">
            {formatDate(group.eventStartAt)} ~ {formatDate(group.eventEndAt)}
          </p>
          {hasMultiple && (
            <p className="text-body-sm text-on-surface-variant mt-1">
              티켓 {activeIndex + 1} / {ticketCount}
            </p>
          )}
        </div>

        {/*
          화살표 버튼은 티켓이 1장이어도 자리(w-6)를 그대로 차지한다 — hasMultiple 일 때만
          렌더링하면 QR 블록이 카드마다 좌우로 밀려서 1장짜리 카드와 여러 장짜리 카드의
          정렬이 어긋나 보인다. 항상 같은 폭을 두고 안 쓸 때만 투명 처리한다.
        */}
        <div className="flex items-center gap-3 self-center">
          <button
            type="button"
            aria-label="이전 티켓"
            disabled={!hasMultiple}
            onClick={() => setIndex((activeIndex - 1 + ticketCount) % ticketCount)}
            className="text-on-surface-variant hover:text-on-surface w-6 shrink-0 disabled:invisible"
          >
            <ChevronLeft aria-hidden className="mx-auto" />
          </button>

          <TicketQr ticket={ticket} />

          <button
            type="button"
            aria-label="다음 티켓"
            disabled={!hasMultiple}
            onClick={() => setIndex((activeIndex + 1) % ticketCount)}
            className="text-on-surface-variant hover:text-on-surface w-6 shrink-0 disabled:invisible"
          >
            <ChevronRight aria-hidden className="mx-auto" />
          </button>
        </div>
      </div>
    </Card>
  )
}

/**
 * 너비를 고정한다(`w-40`). 입장 시각 텍스트("2026. 8. 20. 오후 08:25")가 QR 박스(120px)보다
 * 넓어서, 이 텍스트가 있는 티켓과 없는 티켓 사이에 블록 너비가 달라지면 옆의 화살표 버튼이
 * 좌우로 밀린다 — 폭을 고정해 어떤 상태든 화살표 위치가 흔들리지 않게 한다.
 */
const TicketQr = ({ ticket }: { ticket: MemberTicket }) => (
  <div className="flex w-40 shrink-0 flex-col items-center gap-2">
    {ticket.qrPayload ? (
      <QRCodeSVG value={ticket.qrPayload} size={120} level="M" />
    ) : (
      <div className="bg-surface-container-high text-on-surface-variant text-body-sm flex h-[120px] w-[120px] items-center justify-center rounded-md text-center">
        QR 없음
      </div>
    )}
    <p className="font-mono text-xs tracking-wider">{ticket.ticketCode}</p>
    <Badge variant={STATUS_VARIANT[ticket.status]}>{STATUS_LABEL[ticket.status]}</Badge>
    {/*
      입장 시각은 뱃지 밖, 항상 같은 자리에 고정 높이로 둔다. 뱃지 안에 붙이면
      "입장 완료 · 2026. 8. 20. 오후 08:25" 처럼 길어져서 티켓마다 카드 높이가
      달라지고, 좌우로 넘길 때 카드가 흔들리는 것처럼 보인다.
    */}
    <p className="text-body-sm text-on-surface-variant h-5 whitespace-nowrap">
      {ticket.status === 'CHECKED_IN' && ticket.checkedInAt
        ? formatDateTime(ticket.checkedInAt)
        : ''}
    </p>
  </div>
)

export default MyTicketsPage
