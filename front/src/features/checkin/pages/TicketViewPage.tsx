'use client'

import { QRCodeSVG } from 'qrcode.react'
import { useSearchParams } from 'next/navigation'

import { TicketViewError, type TicketViewTicket } from '../api'
import { useTicketView } from '../hooks'

/**
 * SMS 로 받은 링크가 도착하는 화면. **로그인이 필요 없다.**
 *
 * 발권 문자의 `QR 확인 https://.../tickets?token=...` 이 여기로 온다.
 * 토큰이 곧 인증이라 URL 말고는 아무것도 요구하지 않는다.
 *
 * QR 은 **브라우저에서 그린다.** 백엔드가 이미지를 만들어 주지 않고 원문 문자열(`qrPayload`)만
 * 준다. 이미지를 서버에서 만들면 엔드포인트가 하나 늘고 그 엔드포인트도 토큰을 다시 검증해야
 * 하는데, 화면은 어차피 필요하므로 아끼는 것이 없다.
 */
const TicketViewPage = () => {
  const token = useSearchParams().get('token')
  const { data, isPending, error } = useTicketView(token)

  if (!token) {
    return <Message title="링크가 올바르지 않습니다" detail="주소에 토큰이 없습니다." />
  }

  if (isPending) {
    return <Message title="티켓을 불러오는 중입니다" />
  }

  if (error) {
    return <ErrorMessage error={error} />
  }

  return (
    <main className="mx-auto max-w-md p-6">
      <header className="mb-6">
        <h1 className="text-xl font-bold">{data.tickets[0]?.expoTitle ?? '입장권'}</h1>
        <p className="mt-1 text-sm text-gray-500">
          주문번호 {data.orderNumber} · {data.ticketCount}매
        </p>
      </header>

      <ul className="space-y-6">
        {data.tickets.map((ticket) => (
          <li key={ticket.issuedTicketId}>
            <TicketCard ticket={ticket} />
          </li>
        ))}
      </ul>

      <p className="mt-8 text-xs text-gray-400">
        입장 시 이 화면의 QR 을 보여 주십시오. QR 이 읽히지 않으면 아래 티켓 코드를 직원에게 알려
        주시면 됩니다.
      </p>
    </main>
  )
}

/**
 * 입장권 한 장.
 *
 * 이미 입장한 티켓은 QR 을 흐리게 하고 입장 시각을 얹는다. QR 을 아예 감추지는 않는다 —
 * 재입장 정책이 아직 없어서, 화면이 먼저 정하는 것은 옳지 않다.
 */
const TicketCard = ({ ticket }: { ticket: TicketViewTicket }) => {
  const usable = ticket.status === 'ISSUED'

  return (
    <article className="rounded-lg border border-gray-200 p-5">
      <div className="flex justify-center">
        <div className={usable ? '' : 'opacity-30'}>
          <QRCodeSVG value={ticket.qrPayload} size={200} level="M" />
        </div>
      </div>

      <p className="mt-4 text-center font-mono text-sm tracking-wider">{ticket.ticketCode}</p>

      <p className="mt-2 text-center text-sm">
        <StatusLabel ticket={ticket} />
      </p>
    </article>
  )
}

const StatusLabel = ({ ticket }: { ticket: TicketViewTicket }) => {
  switch (ticket.status) {
    case 'ISSUED':
      return <span className="text-green-600">입장 가능</span>
    case 'CHECKED_IN':
      return (
        <span className="text-gray-500">
          입장 완료
          {ticket.checkedInAt ? ` · ${formatDateTime(ticket.checkedInAt)}` : ''}
        </span>
      )
    case 'CANCELED':
      return <span className="text-gray-500">취소된 티켓</span>
    case 'INVALIDATED':
      return <span className="text-red-600">사용할 수 없는 티켓</span>
  }
}

/**
 * 실패를 넷으로 갈라 보여 준다.
 *
 * **폐기(`REVOKED`)에는 재발급을 안내하지 않는다.** 폐기는 무언가 잘못돼서 끊은 링크라,
 * "다시 받으세요" 라고 안내하면 안 되는 경우다. 만료는 반대로 안내해야 한다.
 */
const ErrorMessage = ({ error }: { error: Error }) => {
  const kind = error instanceof TicketViewError ? error.kind : 'UNKNOWN'

  switch (kind) {
    case 'NOT_FOUND':
      return (
        <Message
          title="유효하지 않은 링크입니다"
          detail="주소가 잘린 것은 아닌지 확인해 주십시오."
        />
      )
    case 'EXPIRED':
      return (
        <Message
          title="링크 유효기간이 지났습니다"
          detail="주최사에 문의하시면 링크를 다시 받으실 수 있습니다."
        />
      )
    case 'REVOKED':
      return <Message title="사용할 수 없는 링크입니다" detail="주최사에 문의해 주십시오." />
    default:
      return <Message title="티켓을 불러오지 못했습니다" detail="잠시 후 다시 시도해 주십시오." />
  }
}

const Message = ({ title, detail }: { title: string; detail?: string }) => (
  <main className="mx-auto max-w-md p-6 text-center">
    <h1 className="text-lg font-bold">{title}</h1>
    {detail ? <p className="mt-2 text-sm text-gray-500">{detail}</p> : null}
  </main>
)

/** 서버는 `Instant`(UTC) 를 주고, 보는 사람은 자기 시간대로 읽는다. */
const formatDateTime = (isoInstant: string) =>
  new Date(isoInstant).toLocaleString('ko-KR', {
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })

export default TicketViewPage
