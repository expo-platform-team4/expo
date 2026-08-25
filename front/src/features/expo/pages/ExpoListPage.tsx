'use client'

import { useState } from 'react'

import {
  Button,
  EmptyState,
  ErrorState,
  Input,
  LoadingBlock,
  PageHeader,
  Select,
} from '@/components/ui'

import type { ExpoCardQuery } from '../api'
import { ExpoCardItem } from '../components/ExpoCardItem'
import { useExpoCards, useExpoCategories } from '../hooks'

/**
 * 지역 필터 선택지. `expos.region_code` 에 들어가는 값이고 별도 코드 테이블이 없어서
 * 여기에 적는다. 값이 늘어나면 백엔드에 지역 목록 API 를 두고 그걸 부른다.
 */
const REGIONS = [
  'SEOUL',
  'GYEONGGI',
  'INCHEON',
  'BUSAN',
  'DAEGU',
  'DAEJEON',
  'GWANGJU',
  'ULSAN',
  'SEJONG',
  'GANGWON',
  'CHUNGBUK',
  'CHUNGNAM',
  'JEONBUK',
  'JEONNAM',
  'GYEONGBUK',
  'GYEONGNAM',
  'JEJU',
] as const

/**
 * `/expos` — 박람회 목록. Function.md 2절.
 *
 * `GET /api/expos` 가 `v_public_expo_cards` 뷰를 그대로 내보낸다. 뷰가 이미
 * `visibility_status = 'PUBLIC'` 으로 걸러 주므로 비공개 박람회는 여기 오지 않는다.
 *
 * 검색어는 **입력할 때마다 요청하지 않는다** — 폼을 제출해야 조회한다. 목록 조회가
 * 뷰 하나를 통째로 훑는 쿼리라 글자마다 부르면 부담이 크고, 필터가 캐시 키라 매 글자가
 * 캐시 항목을 하나씩 만든다.
 */
const ExpoListPage = () => {
  const [draftKeyword, setDraftKeyword] = useState('')
  const [query, setQuery] = useState<ExpoCardQuery>({})
  const { data, isPending, isError, error, refetch } = useExpoCards(query)
  const { data: categories } = useExpoCategories()

  const apply = (patch: Partial<ExpoCardQuery>) =>
    setQuery((previous) => ({ ...previous, ...patch }))

  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="박람회 목록" description="전체 박람회를 둘러볼 수 있는 공간입니다." />

      <form
        className="flex flex-wrap items-end gap-3"
        onSubmit={(event) => {
          event.preventDefault()
          apply({ keyword: draftKeyword.trim() || undefined })
        }}
      >
        <div className="min-w-56 flex-1">
          <Input
            label="검색"
            placeholder="박람회명으로 검색"
            value={draftKeyword}
            onChange={(event) => setDraftKeyword(event.target.value)}
          />
        </div>
        <div className="w-40">
          <Select
            label="지역"
            value={query.regionCode ?? ''}
            onChange={(event) => apply({ regionCode: event.target.value || undefined })}
          >
            <option value="">전체</option>
            {REGIONS.map((region) => (
              <option key={region} value={region}>
                {region}
              </option>
            ))}
          </Select>
        </div>
        <div className="w-40">
          <Select
            label="카테고리"
            value={query.categoryId ? String(query.categoryId) : ''}
            onChange={(event) =>
              apply({ categoryId: event.target.value ? Number(event.target.value) : undefined })
            }
          >
            <option value="">전체</option>
            {categories?.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </Select>
        </div>
        <div className="w-36">
          <Select
            label="정렬"
            value={query.sort ?? ''}
            onChange={(event) =>
              apply({ sort: event.target.value === 'POPULAR' ? 'POPULAR' : undefined })
            }
          >
            <option value="">행사일순</option>
            <option value="POPULAR">인기순</option>
          </Select>
        </div>
        <Button type="submit">검색</Button>
      </form>

      {isError ? (
        <ErrorState error={error} onRetry={() => refetch()} />
      ) : isPending ? (
        <LoadingBlock label="박람회를 불러오는 중입니다" />
      ) : data.length === 0 ? (
        <EmptyState
          title="박람회가 없습니다"
          description="조건에 맞는 박람회를 찾지 못했습니다. 검색어나 지역을 바꿔 보세요."
        />
      ) : (
        <ul className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {data.map((expo) => (
            <li key={expo.expoId}>
              <ExpoCardItem expo={expo} />
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

export default ExpoListPage
