/* eslint-disable @next/next/no-img-element -- 이미지 경로가 /api/files/{id}/content 라
   Next 이미지 최적화 서버를 한 번 더 태울 이유가 없다. 저장소의 다른 이미지(SidebarShell 의
   아바타)도 같은 이유로 <img> 를 쓴다. */
import Link from 'next/link'

import { Badge, Card } from '@/components/ui'
import type { BadgeVariant } from '@/components/ui'
import { formatCurrency } from '@/lib/currency'
import { formatDate } from '@/lib/date'

import { type DisplaySalesStatus, type ExpoCard, SALES_STATUS_LABEL } from '../api'

const STATUS_VARIANT: Record<DisplaySalesStatus, BadgeVariant> = {
  SCHEDULED: 'info',
  ON_SALE: 'success',
  SOLD_OUT: 'neutral',
  SALE_ENDED: 'neutral',
  CANCELED: 'error',
}

/**
 * 목록·홈에서 쓰는 박람회 카드 한 장.
 *
 * 대표 이미지가 없는 박람회가 많아서(주최사가 아직 안 올렸거나 승인 직후) 이미지 자리를
 * 비워 두지 않고 제목 첫 글자를 넣는다. 빈 회색 상자만 늘어놓으면 목록이 무엇인지 모르게 된다.
 *
 * `next/image` 대신 `<img>` 를 쓴다 — 경로가 `/api/files/{id}/content` 라 Next 이미지
 * 최적화 서버를 한 번 더 태울 이유가 없고, 저장소의 다른 이미지도 같은 방식이다.
 */
export const ExpoCardItem = ({ expo }: { expo: ExpoCard }) => (
  <Link href={`/expos/${expo.expoId}`} className="block h-full">
    <Card className="flex h-full flex-col gap-3 p-0 transition-shadow hover:shadow-md">
      <div className="bg-surface-variant relative aspect-[3/2] w-full overflow-hidden rounded-t">
        {expo.thumbnailUrl ? (
          <img
            src={expo.thumbnailUrl}
            alt=""
            className="h-full w-full object-cover"
            loading="lazy"
          />
        ) : (
          <div className="text-display-sm text-on-surface-variant flex h-full w-full items-center justify-center font-semibold opacity-40">
            {expo.title.slice(0, 1)}
          </div>
        )}
        <Badge variant={STATUS_VARIANT[expo.displaySalesStatus]} className="absolute top-2 right-2">
          {SALES_STATUS_LABEL[expo.displaySalesStatus]}
        </Badge>
      </div>

      <div className="flex min-w-0 flex-1 flex-col gap-1 p-4 pt-0">
        <p className="text-title-lg text-on-surface truncate font-semibold">{expo.title}</p>
        <p className="text-body-sm text-on-surface-variant">
          {formatDate(expo.eventStartAt)} ~ {formatDate(expo.eventEndAt)}
        </p>
        <p className="text-body-sm text-on-surface-variant">{expo.regionCode}</p>
        <p className="text-title-md text-on-surface mt-auto pt-2 font-semibold">
          {expo.minimumPrice === undefined
            ? '티켓 준비 중'
            : `${formatCurrency(expo.minimumPrice)}~`}
        </p>
      </div>
    </Card>
  </Link>
)
