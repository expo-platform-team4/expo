'use client'

import { ChevronLeft, ChevronRight } from 'lucide-react'
import Link from 'next/link'
import { useCallback, useEffect, useState } from 'react'

import { bannerImageUrl } from '../api'
import { useActiveBanners } from '../hooks'

/** 자동 전환 간격. */
const ROTATE_INTERVAL_MS = 5000

/**
 * 메인 홈 상단 배너. 슬롯 코드가 `MAIN_TOP` 인 자리다.
 *
 * <h2>배너는 보조 요소다</h2>
 *
 * 로딩 중이거나 실패했거나 노출할 배너가 없으면 <b>아무것도 그리지 않는다.</b> 자리만 잡아 두면
 * 배너가 없는 날 홈 상단이 빈 상자로 남는다. 광고가 없는 것이 정상 상태이기도 하다.
 *
 * <h2>자동 전환을 멈추는 조건 셋</h2>
 *
 * 자동으로 움직이는 UI 는 읽는 사람을 방해할 수 있어 멈출 수단이 필요하다.
 *
 * <ul>
 *   <li>마우스를 올리거나 키보드 포커스가 들어오면 — 지금 보고 있다는 뜻이다
 *   <li>사용자가 점을 눌러 직접 고르면 — 자동 전환이 그 선택을 덮어쓰면 안 된다
 *   <li>OS 에서 "동작 줄이기" 를 켜 두었으면 — 애초에 돌리지 않는다
 * </ul>
 *
 * <p>배너가 하나뿐이면 전환할 것이 없으므로 타이머도 점도 화살표도 만들지 않는다.
 *
 * <h2>화살표는 올렸을 때만 보인다</h2>
 *
 * 배너는 그림이 주인공이라 조작 버튼이 항상 떠 있으면 그림을 가린다. 그래서 마우스를 올렸을 때만
 * 드러낸다. 다만 <b>손가락으로 쓰는 기기에는 hover 가 없다</b> — 그런 기기에서는 처음부터 보인다.
 * 기본값을 "보임" 으로 두고 {@code hover:hover} 인 기기에서만 숨기는 이유가 그것이다. 반대로
 * 짜면(기본 숨김 + hover 시 표시) 터치 기기에서 <b>영영 안 보이는 버튼</b>이 된다.
 *
 * <p>키보드 포커스가 들어와도 보인다. 안 그러면 탭으로 옮겨 갔는데 어디에 있는지 알 수 없다.
 */
const MainBannerCarousel = () => {
  const { data: banners } = useActiveBanners()
  const [index, setIndex] = useState(0)
  const [paused, setPaused] = useState(false)
  /**
   * 사용자가 직접 고른 뒤에는 자동 전환을 멈춘다.
   *
   * <p>`useRef` 가 아니라 상태다. 렌더 중에 ref 를 읽으면 안 되고(React 규칙), 이 값은 실제로
   * <b>무엇을 그릴지를 바꾸는</b> 값이라 상태가 맞다.
   */
  const [pickedByUser, setPickedByUser] = useState(false)

  const count = banners?.length ?? 0
  const rotating = count > 1 && !paused && !pickedByUser

  useEffect(() => {
    if (!rotating) {
      return
    }
    // OS 의 "동작 줄이기" 설정을 존중한다.
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      return
    }
    const timer = window.setInterval(() => {
      setIndex((current) => (current + 1) % count)
    }, ROTATE_INTERVAL_MS)
    return () => window.clearInterval(timer)
  }, [rotating, count])

  const pick = useCallback((next: number) => {
    setPickedByUser(true)
    setIndex(next)
  }, [])

  /**
   * 앞뒤로 한 칸 옮긴다. 끝에서는 반대쪽 끝으로 돌아간다.
   *
   * <p>화살표도 "사용자가 직접 골랐다" 로 친다. 점을 누른 것과 다를 이유가 없다 — 내가 방금 옮긴
   * 배너를 5초 뒤 타이머가 밀어내면 조작이 무시당한 것으로 보인다.
   */
  const step = useCallback(
    (delta: number) => {
      pick((index + delta + count) % count)
    },
    [pick, index, count]
  )

  // 배너가 없으면 자리도 만들지 않는다. 위 주석 참고.
  if (!banners || banners.length === 0) {
    return null
  }

  const current = banners[Math.min(index, banners.length - 1)]

  return (
    <section
      aria-label="추천 박람회 광고"
      aria-roledescription="carousel"
      className="group relative overflow-hidden rounded-lg"
      onMouseEnter={() => setPaused(true)}
      onMouseLeave={() => setPaused(false)}
      onFocus={() => setPaused(true)}
      onBlur={() => setPaused(false)}
    >
      <Link href={`/expos/${current.expoId}`} className="block">
        {/* eslint-disable-next-line @next/next/no-img-element -- 백엔드 프록시 경로라 Next 이미지 최적화 대상이 아니다. */}
        <img
          src={bannerImageUrl(current.imageFileId)}
          alt={current.headline ?? '박람회 광고 배너'}
          className="h-48 w-full object-cover sm:h-64"
        />
        {current.headline && (
          <p className="bg-surface-container-high text-on-surface text-title-md px-4 py-3">
            {current.headline}
          </p>
        )}
      </Link>

      {banners.length > 1 && (
        <>
          <ArrowButton direction="prev" onClick={() => step(-1)} />
          <ArrowButton direction="next" onClick={() => step(1)} />
        </>
      )}

      {banners.length > 1 && (
        <div className="absolute bottom-14 left-1/2 flex -translate-x-1/2 sm:bottom-18">
          {banners.map((banner, position) => (
            <button
              key={banner.id}
              type="button"
              onClick={() => pick(position)}
              aria-label={`${position + 1}번째 배너 보기`}
              aria-current={position === index}
              // 점은 8px 로 보이지만 누르는 자리는 패딩까지 포함해 24px 이다. 점 크기 그대로
              // 두면 빗나간 손가락이 아래 링크를 눌러 박람회 상세로 넘어가 버린다 - 실제로
              // 브라우저에서 그렇게 됐다.
              className="grid size-6 place-items-center"
            >
              <span
                className={`block size-2 rounded-full transition-opacity ${
                  position === index ? 'bg-on-surface' : 'bg-on-surface/40'
                }`}
              />
            </button>
          ))}
        </div>
      )}
    </section>
  )
}

/**
 * 좌우 전환 화살표. 이미지 높이의 한가운데에 놓는다.
 *
 * <p>세로 기준을 <b>section 전체가 아니라 이미지</b>로 잡았다. 아래 문구 막대까지 포함해 가운데를
 * 잡으면 화살표가 그림과 문구 경계에 걸친다. 그림 위에 얹히는 조작이므로 그림을 기준으로 삼는다.
 * 이미지 높이가 {@code h-48 sm:h-64} 라 절반인 {@code top-24 sm:top-32} 에 놓고 자기 높이의
 * 절반만큼 끌어올린다.
 *
 * <p>배경을 반투명 검정으로 깐다. 배너 이미지가 어떤 색일지 알 수 없어 아이콘만 얹으면 밝은
 * 그림에서 흰 화살표가 사라진다.
 */
const ArrowButton = ({
  direction,
  onClick,
}: {
  direction: 'prev' | 'next'
  onClick: () => void
}) => {
  const isPrev = direction === 'prev'
  const Icon = isPrev ? ChevronLeft : ChevronRight

  return (
    <button
      type="button"
      onClick={onClick}
      aria-label={isPrev ? '이전 배너 보기' : '다음 배너 보기'}
      className={`absolute top-24 z-10 grid size-10 -translate-y-1/2 place-items-center rounded-full bg-black/40 text-white transition-opacity hover:bg-black/60 focus-visible:opacity-100 sm:top-32 [@media(hover:hover)]:opacity-0 [@media(hover:hover)]:group-hover:opacity-100 ${
        isPrev ? 'left-2 sm:left-4' : 'right-2 sm:right-4'
      }`}
    >
      <Icon className="size-6" aria-hidden />
    </button>
  )
}

export default MainBannerCarousel
