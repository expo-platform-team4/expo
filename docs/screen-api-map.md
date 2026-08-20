# 화면 · 모듈 · API 대응표 — 작업 지시서

Stitch 디자인의 화면과 프론트 모듈, 백엔드 API 를 잇는 표다.
**1~5절은 조사 결과, 7절은 그 결과를 바탕으로 확정된 결정이다. 실제 작업은 7절 기준으로 한다.**

| | |
|-|-|
| 디자인 | Stitch 프로젝트 `엑스포티켓(ExpoTicket)` · `projects/9433856227347358698` |
| 화면 | **33개** (캔버스 인스턴스는 238개지만 재생성 이력이고, 최종 화면은 33개다) |
| 백엔드 | 엔드포인트 **133개** 구현 완료 |
| 프론트 | 라우트 12개 존재 / 30개 계획 · `features` 모듈 8개 중 **1개만 실구현** |

> 화면 ID 는 앞 8자만 적었다. 전체는 `projects/9433856227347358698/screens/{id}` 다.

---

## 1. 일반 사용자 (비로그인 · 회원 · 비회원)

| 화면 | ID | 라우트 | 모듈 | 백엔드 API |
|-|-|-|-|-|
| 에스포틱 홈 | `6e6d886e` | `/` ✅ | `expo` | 배너·추천 조회 **없음** |
| 박람회 목록 | `a1fc1acc` | `/expos` ✅ | `expo` | **없음** (공개 목록 API 미구현) |
| 박람회 상세 정보 | `5f9fb15e` | `/expos/{id}` ❌ | `expo` | `GET /api/expos/{expoId}/ticket-products/purchasable` |
| 로그인 | `8d6a731b` | `/login` ✅ | `auth` | `POST /api/auth/login` · `/reissue` |
| 회원가입 — 유형 선택 | `e0548499` | ❌ | `auth` | — (화면 분기만) |
| 일반회원 가입 | `7eb82549` | ❌ | `auth` | `POST /api/auth/signup` · `email-availability` · `nickname-availability` · `phone-verifications` |
| 기업회원 가입 | `14b50137` | ❌ | `auth` | `POST /api/auth/client-signup` · `business-number-availability` |
| 소셜 로그인 추가 정보 | `2e17f073` | `/auth/callback` ✅ | `auth` | OAuth2 콜백 후 추가 정보 |
| Auth Flow (영문) | `a6bf4b3a` | — | — | **정체 불명.** 아래 3절 참고 |
| 티켓 예매·결제 (회원) | `d3c2aaf3` | `/orders` ✅ | `ticket` | `POST /api/orders/member` |
| 티켓 예매·결제 (비회원) | `2fd025bd` | `/orders/guest` ❌ | `ticket` | `POST /api/orders/guest` |
| 예매 내역 (마이페이지) | `50b7de2e` | `/mypage/orders` ❌ | `ticket` | **없음** (회원 주문 목록 API 미구현) |
| 나의 티켓 (티켓별 QR) | `66513a65` | `/mypage/tickets` ❌ | `checkin` | **없음** (회원용 티켓 목록 API 미구현) |
| 비회원 주문 조회 | `119d7f0f` | `/orders/guest` ❌ | `ticket` | `POST /api/orders/search/guest` |
| 비회원 주문 상세 (환불·QR) | `d25ce9bc` | `/orders/{id}` ❌ | `ticket` | `POST /api/orders/search/guest` |
| 공고 모집 목록 | `b516ae29` | `/recruitment-notices` ✅ | `recruitment` | `GET /api/recruitment-notices` |
| 기업 모집 공고 참여 신청 | `1921eb04` | `/recruitment-notices/{id}` ❌ | `recruitment` | `GET /api/recruitment-notices/{noticeId}` · `POST /api/client/participation-applications` |
| 배너 신청 및 등록 | `3a1c55b3` | `/client/banners/new` ❌ | `booth`? | **없음** (배너 API 미구현) |

---

## 2. 주최사 포털 (`CLIENT`)

| 화면 | ID | 라우트 | 모듈 | 백엔드 API |
|-|-|-|-|-|
| 클라이언트 대시보드 | `0ae4be64` | `/client/dashboard` ✅ | `client` | `GET /api/client/me/dashboard` |
| 내 박람회 | `f83a3d68` | `/client/expos` ❌ | `expo` | `GET /api/client/me/expos` · `/expos/{expoId}/sales-summary` |
| 박람회 개최 신청 | `13484d75` | ❌ | `expo` | **없음** (개최 신청 API 미구현) |
| 모집공고 요청 관리 | `fe000d47` | `/client/recruitment-notice-requests` ❌ | `recruitment` | `GET /api/client/recruitment-notice-requests` · `/{requestId}` |
| 공고 생성 요청 | `533717af` | `/client/recruitment-notice-requests/new` ❌ | `recruitment` | `POST /api/client/recruitment-notice-requests` |
| 참여 신청 내역 | `79d848ba` | `/client/participations` ❌ | `client` | `GET /api/client/me/participations` · `/participation-applications/{id}` |
| 광고 배너 관리 | `f6d38be5` | `/client/banners` ❌ | `booth`? | **없음** (배너 API 미구현) |
| 정산 리포트 | `3ad4a13d` | `/client/settlements` ❌ | **신설 필요** | `GET /api/client/settlements` · `/{settlementId}` |

---

## 3. 관리자 (`ADMIN`)

| 화면 | ID | 라우트 | 모듈 | 백엔드 API |
|-|-|-|-|-|
| 관리자 대시보드 | `ecafe136` | ❌ | **신설 필요** | `GET /api/admin/dashboard/summary` · `/pending-tasks` |
| 박람회 개최 승인 관리 | `262c25c5` | ❌ | `expo` | **없음** (개최 승인 API 미구현) |
| 공고 신청 관리 | `0f675bf6` | ❌ | `recruitment` | `GET /api/admin/recruitment-notice-requests` · `PATCH .../venue-decision` |
| 공고 모집 관리 | `a3059378` | ❌ | `recruitment` | `GET/POST /api/admin/recruitment-notices` · `publish` · `close` · `cancel` |
| 카테고리 관리 | `b1690b49` | ❌ | **신설 필요** | `GET/POST/PATCH/DELETE /api/admin/categories` |
| 광고 배너 관리 | `5605ae51` | ❌ | `booth`? | **없음** (배너 API 미구현) |
| 정산 관리 | `fa7c036f` | ❌ | **신설 필요** | `GET /api/admin/settlements` · `calculate` · `confirm` · `transfers` |

---

## 4. 어긋나는 지점

### 4-1. API 는 있는데 화면이 없다

| 기능 | API | 왜 문제인가 |
|-|-|-|
| **현장 체크인** | `GET /api/client/expos/{id}/check-in`<br>`POST .../check-ins/qr` · `/code`<br>`GET .../check-ins/history` | D-API-002~005. **화면이 통째로 없다.** 라우트(`/client/check-in`)만 껍데기로 존재한다 |
| **알림 이력·재발송** | `GET /api/admin/notifications`<br>`POST .../{id}/retry` | D-API-009·010. 관리자 화면 목록에 없다 |
| **SMS 링크 QR 조회** | `GET /api/public/tickets?token=` | D-API-001. `/tickets` 화면은 **이미 구현했지만** Stitch 디자인에는 대응 화면이 없다 |
| 부스 콘텐츠 관리 | `/api/client/booth-contents/**` (13개) | 주최사·관리자 화면 어디에도 없다 |
| 장소·부스 카탈로그 | `/api/admin/virtual-venues/**` 등 (14개) | 관리자 화면 없음 |

**체크인이 가장 크다.** 현장에서 QR 을 스캔하는 화면인데 데스크톱 디자인에서 빠졌다 — 원래 모바일이어야 하는 화면이라 누락됐을 가능성이 높다.

### 4-2. 화면은 있는데 API 가 없다

| 화면 | 없는 API |
|-|-|
| 박람회 목록 · 홈 | 공개 박람회 목록·배너 조회 |
| 예매 내역 · 나의 티켓 | 회원 주문·티켓 목록 (`/api/member/**` 자체가 없다) |
| 박람회 개최 신청 · 승인 관리 | 개최 신청 생성·심사 |
| 광고 배너 (3개 화면) | 배너 도메인 전체 |

### 4-3. 모듈이 없는 화면

`features/` 에 대응 모듈이 없어 **새로 만들어야 하는 것**이 셋이다.

```
settlement   정산 리포트(주최사) · 정산 관리(관리자)
admin        관리자 대시보드 · 카테고리 관리
banner       배너 신청·관리 3개 화면   ← 7-1 결정으로 이번 범위 제외 (이슈 #103)
```

### 4-4. 정체가 불분명한 화면

`Expo Exhibition Service Auth Flow` (`a6bf4b3a`) 하나만 **영문 제목**이고 크기도 다르다
(1280×1024, 나머지는 2560 폭). 다른 화면과 달리 스크린샷도 비어 있다.
**초기 실험 산출물일 가능성이 높다** — 확인 후 제외 여부를 정해야 한다.

---

## 5. 프론트 모듈 현황

```
features/
  auth          스텁   ← 화면 4개 대응
  booth         스텁   ← 배너 화면들이 여기 들어갈지 미정
  checkin       구현   ← api.ts 94줄. /tickets 화면만 완료
  client        스텁   ← 대시보드·참여 신청 내역
  expo          스텁   ← 홈·목록·상세·내 박람회·개최 신청
  recruitment   스텁   ← 공고 5개 화면
  ticket        스텁   ← 예매·결제·주문 조회 5개 화면
  venue         스텁   ← 대응 화면 없음
```

모듈마다 `api.ts` · `queryKeys.ts` · `hooks.ts` · `pages/` 네 파일 규칙이 이미 잡혀 있다.
`checkin` 이 유일한 실구현이므로 **다른 모듈을 채울 때 그 모양을 따르면 된다.**

공통 컴포넌트는 `components/auth/RequireAuth.tsx` 하나뿐이다 — **레이아웃·헤더·사이드바를
사실상 처음 만들게 된다.** Stitch 화면 제목에 "레이아웃 통합" "헤더 통일" 이 반복해 붙어 있는 것을
보면 디자인 쪽에서도 그 통일이 늦게 이뤄진 것으로 보이므로, **공통 레이아웃을 먼저 뽑는 것이 낫다.**

---

## 6. 결정 근거 (조사 시점 메모)

5절까지는 2026-08-19 조사 결과다. 아래는 그 뒤 스크린샷을 직접 열어 확인하며 정리한
근거이고, 최종 결정은 7절이다.

**레이아웃** — 클라이언트 대시보드·관리자 대시보드·예매 내역(마이페이지) 스크린샷을 대조했다.

```
마이페이지 · 클라이언트   헤더(로고+글로벌 내비+Profile) 동일
                        사이드바: 프로필 위 / 메뉴 / 로그아웃 아래  ← 구조 동일, 내용만 다름
관리자                   로고가 사이드바 안에 있음 (헤더 없음)
                        글로벌 내비 없음
                        프로필이 사이드바 "하단"에 있음 (다른 둘은 위)
                        "핵심 관리" / "시스템 관리" 섹션 구분
```

→ 관리자는 색만 다른 변형이 아니라 **별도 셸**이다. 마이페이지·클라이언트는 **하나의 셸**을
공유하고 메뉴·프로필 데이터만 주입하면 된다.

**배포·카메라** — 체크인 스캔은 브라우저 카메라(`getUserMedia`)를 쓰는데, 이는 보안 컨텍스트
(HTTPS 또는 `localhost`)에서만 열린다. OCI + namecheap 도메인이어도 **HTTP 로만 서빙하면
스마트폰에서 카메라가 안 열린다.** Caddy 를 리버스 프록시로 쓰면 Let's Encrypt 인증서 발급·갱신이
자동이라 이 문제가 사실상 해소된다. 원리는 [expo-docs 09-DEPLOY-HTTPS.md](~/Downloads/expo-docs/09-DEPLOY-HTTPS.md)
에 정리했다. 배포 시 OCI 보안 목록과 인스턴스 내부 방화벽을 **둘 다** 열어야 하는 점만 유의한다.

---

## 7. 결정 사항 — 이번 프론트 작업 범위

### 7-1. 배너 (3개 화면) — **제외한다**

`banner` 도메인은 DB·일부 엔티티는 있지만 컨트롤러가 없다. 이번 범위에서 **완전히 뺀다.**
정산까지 끝낸 뒤 D 갈래 담당이 마무리 짓는다 —
[이슈 #103](https://github.com/expo-platform-team4/expo/issues/103).

제외 화면: `배너 신청 및 등록`(`3a1c55b3`) · `광고 배너 관리`(주최사, `f6d38be5`) ·
`광고 배너 관리`(관리자, `5605ae51`)

### 7-2. `Expo Exhibition Service Auth Flow` — **제외한다**

휴대폰 본인인증(SMS)이 현재 목(mock)이라 실제 발송 없이 DEBUG 로그로만 확인된다
(`PhoneVerificationService.java:78`). 이 화면은 그 미완성 인증 흐름과 관련된 것으로 보이며,
실연동은 D 갈래가 이미 검증해 둔 `SmsSender`(Solapi)를 나중에 한 줄로 연결하면 된다 —
[이슈 #104](https://github.com/expo-platform-team4/expo/issues/104).

### 7-3. 체크인 (디자인 없음) — **자유 생성**

디자인이 없으므로 아래 라우트로 새로 만든다. 백엔드는 "스캐너 = 박람회 개최자(CLIENT)"
전제로 이미 구현돼 있어 화면만 만들면 된다.

```
/client/check-in            현황 (이미 라우트만 존재, 화면 신설)
/client/check-in/scan       QR 스캐너 — 카메라, getUserMedia
/client/check-in/history    체크인 이력
```

카메라가 안 되는 환경(HTTP 로컬 배포 등)을 대비해 **코드 수동 입력(D-API-004)을 스캔과
동등한 대체 경로로** 화면에 노출한다 — 이미 API 가 있고, 시연 중 카메라 문제의 안전판이 된다.

### 7-4. 공통 레이아웃 — 셸 3종

```
Public          홈 · 목록 · 상세 · 로그인 · 가입 · 예매        헤더만
Sidebar(light)  마이페이지 + 클라이언트 포털                  헤더 + 밝은 사이드바 (공유 컴포넌트)
Admin(dark)     관리자 전체                                  다크 사이드바, 헤더 없음, 프로필 하단
```

`Sidebar(light)` 하나를 만들고 메뉴 항목·프로필 데이터만 역할별로 주입한다. `components/`
에 `RequireAuth.tsx` 뿐이므로 이 셋을 이번에 신설한다.

### 7-5. 라우트 확정 — 계획 30개 + 추가분

기존 `HomePage.tsx` 계획 30개는 유지하고 아래를 더한다.

```
/tickets?token=                      ✅ 이미 구현 (SMS 링크 착지점)

/client/check-in                     신설 (7-3)
/client/check-in/scan                신설 (7-3)
/client/check-in/history             신설 (7-3)

/admin                               관리자 대시보드        ecafe136
/admin/expos                         박람회 개최 승인 관리   262c25c5
/admin/recruitment-notice-requests   공고 신청 관리         0f675bf6
/admin/recruitment-notices           공고 모집 관리         a3059378
/admin/categories                    카테고리 관리          b1690b49
/admin/settlements                   정산 관리             fa7c036f
/admin/notifications                 알림 이력·재발송 (디자인 없음, D-API-009·010)
```

`/admin/**` 은 계획에 통째로 빠져 있었다. `/api/admin/**` 이 `ROLE_ADMIN` 전용이므로
`/api/client/**`(`CLIENT|ADMIN`)와 같은 층위로 두면 `RequireAuth` 규칙이 단순해진다.

`/admin/banners` 는 7-1 에 따라 만들지 않는다.

---

## 8. 다음 단계

이 문서 기준으로 브랜치를 만들어 PR 로 올리고, 프론트 구현을 시작한다.
공통 레이아웃 3종(7-4)을 가장 먼저 뽑는 것을 권한다 — 개별 화면부터 옮기면 헤더·사이드바를
반복해서 복사하게 된다.

---

*이 문서는 Stitch MCP 로 읽은 2026-08-19 기준 화면 목록과 `dev` 브랜치의 백엔드를 대조하고,
스크린샷 확인과 논의를 거쳐 같은 날 결정을 확정한 것이다.*
