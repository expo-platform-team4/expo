# Style — 어떻게 보이는가

디자인 토큰, 컴포넌트 규칙, 공통 레이아웃 셸. **짐작으로 쓰지 않았다** — Stitch MCP 로
프로젝트(`projects/9433856227347358698`)의 **실제 적용된** 디자인 시스템을 읽고, 화면
스크린샷 셋(클라이언트 대시보드·관리자 대시보드·마이페이지 예매 내역)을 직접 열어 구조를
대조한 값이다.

> **주의.** 이 프로젝트에는 이름이 같은 디자인 시스템 초안이 두 벌 있었다(`ExpoTick`).
> 프로젝트에 실제로 걸린 테마 값(`secondary #4b41e1`, `spacing.lg 40px`)과 대조해
> **첫 번째 것이 적용본**임을 확인했다. 아래 토큰은 그 적용본만 옮긴 것이다.

---

## 1. 브랜드 톤

원문 스타일 가이드: **"Corporate Modernism"** — 일반 방문객(대량)과 업체 고객(꼼꼼함)이라는
이중 독자를 함께 겨냥한다. 장식을 피하고 구조적 정확함과 여백으로 신뢰감을 준다. 그림자는
무겁지 않고 옅게, "종이가 겹쳐 있는" 느낌으로 깊이를 표현한다.

폰트는 **Inter 하나만** 쓴다. 제목은 자간을 좁혀 무게감을, 본문은 줄간격을 넉넉히 해 긴
약관·설명도 읽기 편하게 한다.

**다크 모드는 없다.** 디자인 시스템이 `colorMode: LIGHT` 로 고정돼 있다. 라이트 전용으로
만들고, `prefers-color-scheme` 분기를 넣지 않는다.

---

## 2. 색 토큰

`src/app/globals.css` 의 `@theme` 블록에 아래를 그대로 채운다. 지금 있는 `--background`
/`--foreground`/Geist 폰트 변수, 그리고 다크모드 미디어쿼리는 **전부 지운다** — 보일러플레이트다.

```css
@theme {
  /* Primary — 헤더, 핵심 브랜드 요소, 관리자 사이드바 배경 */
  --color-primary: #031635;
  --color-on-primary: #ffffff;
  --color-primary-container: #1a2b4b;
  --color-on-primary-container: #8293b8;

  /* Secondary — 주요 액션(버튼, 활성 상태, 포커스) */
  --color-secondary: #4b41e1;
  --color-on-secondary: #ffffff;
  --color-secondary-container: #645efb;

  /* Tertiary — 성공/확정/체크인 완료 배지 ("Soft Mint") */
  --color-tertiary: #001c10;
  --color-on-tertiary: #ffffff;
  --color-tertiary-container: #003320;
  --color-on-tertiary-container: #00a774;
  --color-tertiary-fixed: #6ffbbe;

  /* Error */
  --color-error: #ba1a1a;
  --color-on-error: #ffffff;
  --color-error-container: #ffdad6;
  --color-on-error-container: #93000a;

  /* Surface — 배경·카드 계층 (밝은 순서: lowest → highest) */
  --color-background: #f8f9ff;
  --color-on-background: #0d1c2f;
  --color-surface: #f8f9ff;
  --color-surface-container-lowest: #ffffff;
  --color-surface-container-low: #eff4ff;
  --color-surface-container: #e6eeff;
  --color-surface-container-high: #dde9ff;
  --color-surface-container-highest: #d5e3fd;
  --color-on-surface: #0d1c2f;
  --color-on-surface-variant: #44474e;

  /* Outline */
  --color-outline: #75777f;
  --color-outline-variant: #c5c6cf;

  /* Inverse (관리자 다크 사이드바에서 쓸 값) */
  --color-inverse-surface: #233144;
  --color-inverse-on-surface: #ebf1ff;

  /* Typography */
  --font-sans: 'Inter', sans-serif;

  --text-display-lg: 48px;
  --text-display-lg--line-height: 56px;
  --text-display-lg--font-weight: 700;
  --text-display-lg--letter-spacing: -0.02em;

  --text-display-lg-mobile: 36px;
  --text-display-lg-mobile--line-height: 44px;
  --text-display-lg-mobile--font-weight: 700;

  --text-headline-md: 30px;
  --text-headline-md--line-height: 38px;
  --text-headline-md--font-weight: 600;

  --text-headline-sm: 24px;
  --text-headline-sm--line-height: 32px;
  --text-headline-sm--font-weight: 600;

  --text-title-lg: 20px;
  --text-title-lg--line-height: 28px;
  --text-title-lg--font-weight: 600;

  --text-body-lg: 18px;
  --text-body-lg--line-height: 28px;

  --text-body-md: 16px;
  --text-body-md--line-height: 24px;

  --text-label-md: 14px;
  --text-label-md--line-height: 20px;
  --text-label-md--font-weight: 500;

  --text-label-sm: 12px;
  --text-label-sm--line-height: 16px;
  --text-label-sm--font-weight: 600;

  /*
   * Spacing (8px 그리드) — 이름 있는 토큰을 두지 않는다.
   *
   * Tailwind v4 는 `--spacing-*` 네임스페이스 하나를 p/m/gap/w/h/max-w/min-w/inset 등
   * 모든 크기 유틸리티가 공유한다. 여기에 `--spacing-md: 24px` 처럼 이름을 얹으면
   * `max-w-md`(기본값 28rem)까지 24px 로 덮어써서 화면 전체의 `max-w-*`·`w-*` 가
   * 조용히 깨진다 — 실제로 로그인 카드가 24px 로 찌그러지는 걸로 발견했다(구현 단계에서
   * 처음 시도했던 `--spacing-xs/sm/md/lg/xl` 블록이 이 문제였다).
   *
   * 기본 숫자 스케일(`--spacing: 0.25rem` 기반, 건드리지 않음)이 이미 8px 그리드와
   * 맞아떨어진다 — xs=2(8px) sm=4(16px) md=6(24px) lg=10(40px) xl=16(64px).
   * 화면에서는 `gap-6`·`p-10`처럼 숫자 유틸리티를 쓴다.
   */
  --container-max: 1280px;

  /* Radius */
  --radius-sm: 0.25rem;
  --radius: 0.5rem;
  --radius-md: 0.75rem;
  --radius-lg: 1rem;
  --radius-xl: 1.5rem;
}

body {
  background: var(--color-background);
  color: var(--color-on-background);
  font-family: var(--font-sans);
}
```

**그림자는 유틸리티가 아니라 인라인으로 쓴다** (Tailwind v4 의 임의값 문법).

```
Level 1 (input, 보조 블록)   테두리만: border border-outline-variant, 그림자 없음
Level 2 (카드, 정산 요약)     shadow-[0_4px_6px_-1px_rgba(26,43,75,0.05),0_2px_4px_-1px_rgba(26,43,75,0.03)]
Level 3 (모달, 드롭다운)      shadow-[0_20px_25px_-5px_rgba(26,43,75,0.1),0_10px_10px_-5px_rgba(26,43,75,0.04)]
```

---

## 3. 반응형 기준

```
모바일   ~767px    4컬럼, 16px 거터, 16px 여백
태블릿   768~1439px 8컬럼, 20px 거터
데스크톱 1440px+    12컬럼, 24px 거터, 1280px 최대 컨테이너
```

Stitch 화면은 2560px 너비(레티나)로 뽑혀 있다 — **그대로 베끼지 않는다.** 실제 콘텐츠는
`max-w-[1280px] mx-auto` 안에 넣고, 그 안에서 12컬럼 그리드를 쓴다.

---

## 4. 컴포넌트 규칙

원문 가이드에서 이 프로젝트에 실제로 쓰이는 것만 옮긴다.

| 컴포넌트       | 규칙                                                                                                                                                                                   |
| -------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **버튼(주)**   | `secondary` 배경(#4b41e1) + 흰 글씨. 라운드 `--radius`(8px)                                                                                                                            |
| **버튼(보조)** | 1px 아웃라인(`primary` 또는 `outline`), 배경 없음                                                                                                                                      |
| **카드**       | 흰 배경(`surface-container-lowest`) + Level 2 그림자. 제목은 `primary` 색                                                                                                              |
| **입력 필드**  | 8px 라운드, 1px `#cbd5e1` 테두리. 포커스 시 `secondary` 테두리 + 2px 외곽 글로우                                                                                                       |
| **뱃지·칩**    | 성공/확정/체크인 완료 → `tertiary-container` 배경 + `on-tertiary-container` 글씨(Soft Mint). 카테고리 태그 → 연한 회색 배경                                                            |
| **목록**       | `#f1f5f9` 구분선. 드릴다운 가능한 행은 우측에 `chevron-right`                                                                                                                          |
| **아이콘**     | 내비게이션 24px 아웃라인, 상태 표시 20px 솔리드                                                                                                                                        |
| **티켓 카드**  | QR 과 상세 정보 사이에 **점선(perforated) 구분선.** `checkin` 모듈의 `TicketCard`(`TicketViewPage.tsx`)가 이미 이 패턴이다 — 새로 만들 때(마이페이지 "나의 티켓" 등) 그대로 재사용한다 |

---

## 5. 공통 레이아웃 셸 — 셋

`components/layout/` 에 신설한다. `docs/screen-api-map.md` 6절이 스크린샷 대조로 확인한
구조다.

### 5-1. Public — 공개 화면

```
헤더만.  로고 | 박람회·공고 신청·공고 모집·비회원 주문 조회 | [로그인] 또는 [Profile]
```

홈·목록·상세·로그인·가입·예매 화면이 쓴다.

### 5-2. Sidebar(light) — 마이페이지 · 클라이언트 포털

**둘이 완전히 같은 구조를 공유한다.** 내용(메뉴 항목, 프로필 데이터)만 주입한다.

```
상단 헤더    로고 | 글로벌 내비(Public 과 동일) | Profile
좌측 사이드바
  프로필 원형 이미지
  이름
  부가정보 (회원: 전화번호 / 클라이언트: 사업자번호)
  [프로필 수정] 버튼
  ─────────────
  메뉴 목록 (역할별로 다름)
  ─────────────
  로그아웃          ← 항상 맨 아래
```

컴포넌트 하나(`SidebarShell`)로 만들고 `menuItems`·`profile` 을 props 로 받는다.

```tsx
<SidebarShell
  profile={{ name, subtitle, avatarUrl }}
  menuItems={[...]}
>
  {children}
</SidebarShell>
```

### 5-3. Admin(dark) — 관리자 전체

**색만 바꾼 변형이 아니다.** 구조 자체가 다르다.

```
로고가 사이드바 안에 있다        (Public/Sidebar 는 헤더에 있다)
상단 글로벌 내비가 없다          (박람회·공고 신청 등 링크 없음)
프로필이 사이드바 "하단"에 있다   (다른 둘은 상단)

좌측 사이드바 (다크, primary #031635 계열 배경)
  로고
  ─────────────
  "핵심 관리" 섹션 헤더
    메뉴들
  "시스템 관리" 섹션 헤더
    메뉴들
  (빈 공간)
  ─────────────
  관리자 프로필 (이름 + "관리자 계정" 같은 라벨)
  로그아웃
```

별도 컴포넌트(`AdminShell`)로 만든다. `SidebarShell` 과 억지로 합치지 않는다 — 구조가
달라서 props 분기가 조건투성이가 된다.

---

## 6. 아이콘·이미지

원문 가이드가 아이콘 세트를 지정하지 않았다. **Lucide** 를 권장한다(Next/React 생태계에서
흔히 쓰이고 24px 아웃라인·20px 솔리드 스타일을 다 지원한다). 새 의존성이므로 추가할 때
`General.md` 의 스택 표에도 반영한다.

배너·박람회 썸네일처럼 원본 이미지가 없는 자리는 `bg-surface-container` 배경 + 중앙 정렬
플레이스홀더 아이콘으로 채운다. 회색 박스에 "img" 텍스트만 넣지 않는다(스크린샷에 그렇게
나온 것은 Stitch 목업용 표시다).

---

## 다음

화면 목록은 [Function.md](Function.md), API 계약·인증·에러 처리는 [Spec.md](Spec.md) 를 본다.
