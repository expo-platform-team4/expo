/* eslint-disable @next/next/no-img-element -- 이미지 경로가 /api/files/{id}/content 라
   Next 이미지 최적화 서버를 한 번 더 태울 이유가 없다. 저장소의 다른 이미지(SidebarShell 의
   아바타)도 같은 이유로 <img> 를 쓴다. */
'use client'

import Link from 'next/link'
import { useParams } from 'next/navigation'

import {
  Badge,
  Button,
  Card,
  CardTitle,
  EmptyState,
  ErrorState,
  LoadingBlock,
  PageHeader,
} from '@/components/ui'
import { useAuthStore } from '@/lib/auth'
import { formatCurrency } from '@/lib/currency'
import { formatDate, formatDateTime } from '@/lib/date'

import {
  EXPO_FILE_PURPOSE_LABEL,
  type ExpoDetail,
  type ExpoFile,
  type PurchasableTicketProduct,
  SALES_STATUS_LABEL,
} from '../api'
import { useExpoDetail, usePurchasableTicketProducts } from '../hooks'

/**
 * `/expos/{expoId}` — 박람회 상세. Function.md 2절 "박람회 정보 + 구매 가능 티켓 상품 목록".
 *
 * 박람회 정보는 `GET /api/expos/{expoId}`, 티켓은
 * `GET /api/expos/{expoId}/ticket-products/purchasable` 로 각각 받는다. 둘을 따로 두는
 * 이유는 티켓 재고가 훨씬 자주 바뀌어 캐시 수명이 다르기 때문이다.
 */
const ExpoDetailPage = () => {
  const params = useParams<{ expoId: string }>()
  const parsedExpoId = Number(params.expoId)
  const expoId = Number.isInteger(parsedExpoId) && parsedExpoId > 0 ? parsedExpoId : null

  const detailQuery = useExpoDetail(expoId)
  const ticketsQuery = usePurchasableTicketProducts(expoId)
  const accessToken = useAuthStore((state) => state.accessToken)

  if (expoId === null) {
    return (
      <div>
        <PageHeader title="박람회 상세" />
        <EmptyState title="잘못된 접근입니다" description="박람회 주소가 올바르지 않습니다." />
      </div>
    )
  }

  if (detailQuery.isError) {
    return (
      <div>
        <PageHeader title="박람회 상세" />
        <ErrorState error={detailQuery.error} onRetry={() => detailQuery.refetch()} />
      </div>
    )
  }

  if (detailQuery.isPending) {
    return <LoadingBlock label="박람회 정보를 불러오는 중입니다" />
  }

  const expo = detailQuery.data

  return (
    <div className="flex flex-col gap-8">
      <ExpoHero expo={expo} />

      <section aria-label="박람회 소개" className="flex flex-col gap-4">
        <h2 className="text-title-lg text-on-background font-semibold">박람회 소개</h2>
        <Card>
          <p className="text-body-md text-on-surface whitespace-pre-line">{expo.description}</p>
        </Card>
        <ExpoGallery expo={expo} />
        <ExpoFileList files={expo.files} />
      </section>

      <section aria-label="구매 가능한 티켓" className="flex flex-col gap-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-title-lg text-on-background font-semibold">구매 가능한 티켓</h2>
          <div className="flex gap-2">
            <Link href={`/orders?expoId=${expoId}`}>
              <Button size="sm">{accessToken ? '예매하기' : '회원 예매하기'}</Button>
            </Link>
            <Link href={`/orders/guest?expoId=${expoId}`}>
              <Button variant="secondary" size="sm">
                비회원 예매하기
              </Button>
            </Link>
          </div>
        </div>

        {ticketsQuery.isPending ? (
          <LoadingBlock label="티켓 정보를 불러오는 중입니다" />
        ) : ticketsQuery.error ? (
          <ErrorState error={ticketsQuery.error} onRetry={ticketsQuery.refetch} />
        ) : ticketsQuery.data.length === 0 ? (
          <EmptyState
            title="구매 가능한 티켓이 없습니다"
            description="현재 판매 중인 티켓 상품이 없습니다."
          />
        ) : (
          <ul className="grid gap-4 sm:grid-cols-2">
            {ticketsQuery.data.map((product) => (
              <li key={product.ticketProductId}>
                <TicketProductCard product={product} />
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}

/** 제목 위에 대표 이미지를 크게 깐다. 없으면 이미지 영역 자체를 넣지 않는다. */
const ExpoHero = ({ expo }: { expo: ExpoDetail }) => {
  const thumbnail = expo.images.find((image) => image.imageType === 'THUMBNAIL')

  return (
    <div className="flex flex-col gap-4">
      {thumbnail && (
        <div className="bg-surface-variant aspect-[16/6] w-full overflow-hidden rounded-md">
          <img
            src={thumbnail.url}
            alt={thumbnail.altText ?? ''}
            className="h-full w-full object-cover"
          />
        </div>
      )}

      <PageHeader
        title={expo.title}
        description={`${formatDate(expo.eventStartAt)} ~ ${formatDate(expo.eventEndAt)} · ${expo.regionCode}`}
      />

      <div className="flex flex-wrap items-center gap-2">
        <Badge variant={expo.displaySalesStatus === 'ON_SALE' ? 'success' : 'neutral'}>
          {SALES_STATUS_LABEL[expo.displaySalesStatus]}
        </Badge>
        {expo.minimumPrice !== undefined && (
          <span className="text-title-md text-on-surface font-semibold">
            {formatCurrency(expo.minimumPrice)}~
          </span>
        )}
        <span className="text-body-sm text-on-surface-variant">
          잔여 {expo.availableQuantity}매
        </span>
      </div>
    </div>
  )
}

/** 대표 이미지는 위에 이미 크게 나오므로 갤러리에서는 뺀다. */
const ExpoGallery = ({ expo }: { expo: ExpoDetail }) => {
  const gallery = expo.images.filter((image) => image.imageType !== 'THUMBNAIL')
  if (gallery.length === 0) {
    return null
  }

  return (
    <ul className="grid grid-cols-2 gap-3 sm:grid-cols-3">
      {gallery.map((image) => (
        <li key={image.id} className="bg-surface-variant aspect-[3/2] overflow-hidden rounded">
          <img
            src={image.url}
            alt={image.altText ?? ''}
            className="h-full w-full object-cover"
            loading="lazy"
          />
        </li>
      ))}
    </ul>
  )
}

/**
 * 소개 자료·팜플렛.
 *
 * 새 탭으로 연다 — PDF 는 브라우저가 그대로 그려 주고, 내려받기를 원하면 그 화면에서
 * 저장하면 된다. 백엔드가 이미지가 아닌 파일에 `Content-Disposition: attachment` 를
 * 붙이므로 실제로는 저장 대화상자가 뜬다.
 */
const ExpoFileList = ({ files }: { files: ExpoFile[] }) => {
  if (files.length === 0) {
    return null
  }

  return (
    <Card className="flex flex-col gap-2">
      <CardTitle>소개 자료</CardTitle>
      <ul className="flex flex-col gap-2">
        {files.map((file) => (
          <li key={file.id} className="flex flex-wrap items-center gap-2">
            <Badge variant="neutral">{EXPO_FILE_PURPOSE_LABEL[file.filePurpose]}</Badge>
            <a
              href={file.url}
              target="_blank"
              rel="noreferrer"
              className="text-secondary text-body-md font-medium underline"
            >
              {file.title ?? '자료 내려받기'}
            </a>
          </li>
        ))}
      </ul>
    </Card>
  )
}

const TicketProductCard = ({ product }: { product: PurchasableTicketProduct }) => {
  const soldOut = product.availableQuantity <= 0

  return (
    <Card className="flex flex-col gap-2">
      <div className="flex items-start justify-between gap-2">
        <CardTitle className="mb-0">{product.name}</CardTitle>
        {soldOut ? <Badge variant="neutral">품절</Badge> : <Badge variant="success">판매중</Badge>}
      </div>
      <p className="text-body-md text-on-surface-variant">{product.description}</p>
      <p className="text-title-lg text-on-surface font-semibold">{formatCurrency(product.price)}</p>
      <p className="text-label-md text-on-surface-variant">
        판매기간 {formatDateTime(product.salesStartAt)} ~ {formatDateTime(product.salesEndAt)}
      </p>
      <p className="text-label-sm text-on-surface-variant">
        잔여 {product.availableQuantity}매 · 1회 최대 {product.maxQuantityPerOrder}매
      </p>
    </Card>
  )
}

export default ExpoDetailPage
