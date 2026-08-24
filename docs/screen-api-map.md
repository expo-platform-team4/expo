# 화면 · 모듈 · API 대응표

Stitch 디자인의 화면과 프론트 라우트, `features` 모듈, 백엔드 API 를 잇는 표다.

> **이 문서는 작업 지시서로 시작해 현행 대조표가 되었다.** 1~3절 표가 지금 상태이고,
> 6~7절은 그때 왜 그렇게 정했는지를 남긴 기록이다(결정 근거라 지우지 않는다).

| | |
|-|-|
| 디자인 | Stitch 프로젝트 `엑스포티켓(ExpoTicket)` · `projects/9433856227347358698` |
| 화면 | **33개** (캔버스 인스턴스는 238개지만 재생성 이력이고, 최종 화면은 33개다) |
| 프론트 | 라우트 **38개** · `features` 모듈 11개 (`venue` 만 스텁) |
| 상태 | Function.md 가 정의한 화면 **전부 연결 완료.** 남은 것은 백엔드 API 가 없어 "준비 중" 인 6개 화면뿐 |

**상태 표기** — ✅ 실제 API 연결 완료 · ⚠️ 화면은 있으나 API 가 없어 "준비 중" 표시
(전부 [이슈 #107](https://github.com/expo-platform-team4/expo/issues/107) 로 추적)

> 화면 ID 는 앞 8자만 적었다. 전체는 `projects/9433856227347358698/screens/{id}` 다.

---

## 1. 일반 사용자 (비로그인 · 회원 · 비회원)

| 화면 | ID | 라우트 | 모듈 | 백엔드 API | 상태 |
|-|-|-|-|-|-|
| 에스포틱 홈 | `6e6d886e` | `/` | `expo` | `GET /api/expos?sort=POPULAR` (추천) | 추천 ✅ / 배너 ⚠️ |
| 박람회 목록 | `a1fc1acc` | `/expos` | `expo` | `GET /api/expos` (지역·검색어·정렬) | ✅ |
| 박람회 상세 | `5f9fb15e` | `/expos/[expoId]` | `expo` | `GET /api/expos/{expoId}` + `/ticket-products/purchasable` | ✅ |
| 로그인 | `8d6a731b` | `/login` | `auth` | `POST /api/auth/login` · `/reissue` | ✅ |
| 회원가입 유형 선택 | `e0548499` | `/signup` | `auth` | — (화면 분기만) | ✅ |
| 일반회원 가입 | `7eb82549` | `/signup/member` | `auth` | `POST /api/auth/signup` · `email-availability` · `nickname-availability` · `phone-verifications` | ✅ |
| 기업회원 가입 | `14b50137` | `/signup/client` | `auth` | `POST /api/auth/client-signup` · `business-number-availability` | ✅ |
| 소셜 로그인 콜백 | `2e17f073` | `/auth/callback` | `auth` | OAuth2 리다이렉트 | ✅ (계약 미확정, 아래 주) |
| 티켓 예매 (회원) | `d3c2aaf3` | `/orders?expoId=` | `ticket` | `POST /api/orders/member` + `payment` 도메인 | ✅ |
| 티켓 예매 (비회원) | `2fd025bd` | `/orders/guest?expoId=` | `ticket` | `POST /api/orders/guest` + `payment` 도메인 | ✅ |
| 비회원 주문 조회 | `119d7f0f` | `/orders/guest/search` | `ticket` | `POST /api/orders/search/guest` | ✅ |
| 비회원 주문 상세 | `d25ce9bc` | `/orders/guest/[orderNumber]` | `ticket` | 위 검색 API 재사용 + `refund` 도메인 | ✅ |
| 예매 내역 | `50b7de2e` | `/mypage/orders` | `ticket` | `GET /api/users/me/orders` | ✅ |
| 나의 티켓 | `66513a65` | `/mypage/tickets` | `ticket` | `GET /api/users/me/tickets` | ✅ |
| 프로필 수정 | 대응 없음 | `/mypage/profile` | `auth` | 닉네임·비밀번호 변경, 회원 탈퇴 | ✅ |
| 공고 모집 목록 | `b516ae29` | `/recruitment-notices` | `recruitment` | `GET /api/recruitment-notices` | ✅ |
| 공고 참여 신청 | `1921eb04` | `/recruitment-notices/[noticeId]` | `recruitment` · `participation` | `GET /api/recruitment-notices/{id}` · `POST /api/client/participation-applications` | ✅ |
| SMS 링크 QR 확인 | 대응 없음 | `/tickets?token=` | `checkin` | `GET /api/public/tickets?token=` | ✅ |
| 배너 신청 및 등록 | `3a1c55b3` | — | — | 배너 도메인 **없음** | 제외 (7-1) |

> **소셜 로그인 콜백** — 백엔드에 OAuth2 `SuccessHandler` 가 아직 없어(선택 프로필
> `application-oauth2.yml` 만 존재) 프론트로 무엇을 실어 보낼지 계약이 정해지지 않았다.
> 가장 흔한 관례(토큰·역할·닉네임을 쿼리로 전달)를 가정해 만들어 두었고,
> 실제 핸들러가 붙으면 `AuthCallbackPage.tsx` 의 가정을 다시 확인해야 한다.

---

## 2. 주최사 포털 (`CLIENT`)

전부 Sidebar(light) 셸을 쓴다.

| 화면 | ID | 라우트 | 모듈 | 백엔드 API | 상태 |
|-|-|-|-|-|-|
| 클라이언트 홈 | 대응 없음 | `/client` | `client` | — (포털 진입 허브) | ✅ |
| 클라이언트 대시보드 | `0ae4be64` | `/client/dashboard` | `client` | `GET /api/client/me/dashboard` · `/me/expos` · `/me/booths` · `/me/recruitment-results` | ✅ |
| 내 박람회 | `f83a3d68` | `/client/expos` | `client` | `GET /api/client/me/expos` + `/api/client/settlements` (매출 요약 조인) | ✅ |
| 박람회 이미지·자료 관리 | 대응 없음 | `/client/expos/[expoId]/content` | `client` | `POST /api/files` + `/api/client/expos/{id}/images` · `/files` | ✅ |
| 박람회 개최 신청 | `13484d75` | `/client/expos/new` | `client` | 개최 신청 **없음** | ⚠️ |
| 부스 관리 | 대응 없음 | `/client/booths` | `booth` | `GET /api/client/me/booths` · `/booth-allocations/{id}` · `/booth-contents/**` | ✅ |
| 모집공고 요청 관리 | `fe000d47` | `/client/recruitment-notice-requests`<br>`/[requestId]` | `recruitment` | `GET /api/client/recruitment-notice-requests` · `/{id}` | ✅ |
| 공고 생성 요청 | `533717af` | `/client/recruitment-notice-requests/new` | `recruitment` | `POST /api/client/recruitment-notice-requests`<br>+ `GET /api/virtual-venues` · `/{id}/halls` · `/api/venue-halls/{id}/zones` | ✅ |
| 참여 신청 내역 | `79d848ba` | `/client/participations` | `participation` | `GET /api/client/me/participations` | ✅ (주 참고) |
| 정산 리포트 | `3ad4a13d` | `/client/settlements`<br>`/[id]` | `settlement` | `GET /api/client/settlements` · `/{settlementId}` | ✅ |
| 체크인 현황 | 대응 없음 | `/client/check-in` | `checkin` | `GET /api/client/expos/{expoId}/check-in` | ✅ |
| QR 스캔 | 대응 없음 | `/client/check-in/scan` | `checkin` | `POST .../check-ins/qr` · `/code` | ✅ |
| 체크인 이력 | 대응 없음 | `/client/check-in/history` | `checkin` | `GET .../check-ins/history` | ✅ |
| 광고 배너 관리 | `f6d38be5` | — | — | 배너 도메인 **없음** | 제외 (7-1) |

> **참여 신청 내역** — 응답이 `recruitmentNoticeId` 만 주고 공고 제목이 없어 화면에
> "공고 #1" 로만 표시된다. 목록에 제목을 조인해 주면 해소된다(이슈 #107 코멘트).

---

## 3. 관리자 (`ADMIN`)

전부 Admin(dark) 셸을 쓴다.

| 화면 | ID | 라우트 | 모듈 | 백엔드 API | 상태 |
|-|-|-|-|-|-|
| 관리자 대시보드 | `ecafe136` | `/admin` | `admin` | `GET /api/admin/dashboard/summary` · `/pending-tasks` | ✅ |
| 박람회 개최 승인 관리 | `262c25c5` | `/admin/expos` | `admin` | 개최 승인 **없음** | ⚠️ |
| 공고 신청 관리 | `0f675bf6` | `/admin/recruitment-notice-requests` | `admin` | `GET /api/admin/recruitment-notice-requests` · `PATCH .../venue-decision` | ✅ |
| 공고 모집 관리 | `a3059378` | `/admin/recruitment-notices`<br>`/new` | `admin` | `GET/POST /api/admin/recruitment-notices` · `publish` · `close` · `cancel` | ✅ |
| 카테고리 관리 | `b1690b49` | `/admin/categories` | `admin` | `GET/POST/PATCH/DELETE /api/admin/categories` | ✅ |
| 정산 관리 | `fa7c036f` | `/admin/settlements` | `admin` | `GET /api/admin/settlements` · `calculate` · `confirm` · `transfers` | ✅ |
| 알림 이력·재발송 | 대응 없음 | `/admin/notifications` | `admin` | `GET /api/admin/notifications` · `POST .../{id}/retry` | ✅ |
| 광고 배너 관리 | `5605ae51` | — | — | 배너 도메인 **없음** | 제외 (7-1) |

> **장소 예약 확정 화면이 없다.** 모집공고 요청을 승인(`venue-decision`)한 뒤 공고를 만들려면
> 그 사이에 `POST /api/admin/venue-reservations` 로 장소 예약을 확정해야 한다
> (안 하면 공고 생성이 `VENUE_RESERVATION_NOT_FOUND` 로 막힌다). Function.md 4절의 7개 화면에
> 없어서 만들지 않았고, 지금은 API 를 직접 호출해야 한다.

---

## 4. 아직 API 가 없어 "준비 중" 인 화면

전부 [이슈 #107](https://github.com/expo-platform-team4/expo/issues/107) 로 추적한다.
화면은 만들어 두고 `EmptyState notReady` 로 "준비 중" 을 명시한다 — 조용히 빈 목록을
보여주지 않는다(Function.md 7절).

> **이슈 #107 은 대부분 해소됐다. 남은 것은 박람회 개최 신청·승인 관리뿐이다** (2·3절 참고).
>
> | 무엇 | 어떻게 |
> |-|-|
> | 홈 · 박람회 목록 · 박람회 상세(헤더) | `GET /api/expos`, `GET /api/expos/{expoId}` (2026-08-20) |
> | 예매 내역 · 나의 티켓 | `GET /api/users/me/orders`, `/tickets` (PR #118) |
> | 결제 완료 · 환불 | `payment`·`refund` 도메인 (2026-08-23~24, 프론트 연동 + `GuestTicketRefundEligibilityService` 읽기전용 트랜잭션 버그 수정 포함) |
>
> 공개 박람회 목록은 예상대로 `v_public_expo_cards` 뷰 위에 컨트롤러·매퍼만 얹으면 되는
> 일이었다.

### 배너 (3개 화면)

`banner` 도메인은 DB·일부 엔티티만 있고 컨트롤러가 없다. 이번 범위에서 완전히 뺐다 —
[이슈 #103](https://github.com/expo-platform-team4/expo/issues/103).

---

## 5. 프론트 모듈 현황

```
features/
  admin         ✅  관리자 7개 화면 (도메인별 api/hooks 분리)
  auth          ✅  로그인·가입·마이페이지·콜백
  booth         ✅  부스 관리 + 부스 소개 콘텐츠
  checkin       ✅  QR 확인(공개) + 체크인 현황·스캔·이력
  client        ✅  홈·대시보드·내 박람회·개최 신청
  expo          ✅  홈·목록·상세 (대부분 준비 중 상태)
  participation ✅  참여 신청 폼·내역
  recruitment   ✅  공고 목록·상세·요청 관리
  settlement    ✅  정산 리포트 목록·상세
  ticket        ✅  예매(회원·비회원)·주문 조회·마이페이지
  venue         스텁  대응 화면 없음 (장소 조회는 recruitment 에서 직접 쓴다)
```

모듈마다 `api.ts` · `queryKeys.ts` · `hooks.ts` · `pages/` 네 파일 규칙을 지킨다.

공통 컴포넌트는 `components/ui`(공유 UI 킷 12종)와 `components/layout`(셸 3종 + 역할별 조립)에
있다. **화면 코드는 `components/ui` 배럴에서만 가져온다** — 개별 파일 직접 import 금지.

---

## 6. 결정 근거 (2026-08-19 조사 시점 기록)

**레이아웃** — 클라이언트 대시보드·관리자 대시보드·예매 내역 스크린샷을 대조했다.

```
마이페이지 · 클라이언트   헤더(로고+글로벌 내비+Profile) 동일
                        사이드바: 프로필 위 / 메뉴 / 로그아웃 아래  ← 구조 동일, 내용만 다름
관리자                   로고가 사이드바 안에 있음 (헤더 없음)
                        글로벌 내비 없음
                        프로필이 사이드바 "하단"에 있음 (다른 둘은 위)
                        "핵심 관리" / "시스템 관리" 섹션 구분
```

→ 관리자는 색만 다른 변형이 아니라 **별도 셸**이다. 마이페이지·클라이언트는 **하나의 셸**을
공유하고 메뉴·프로필 데이터만 주입한다. 구현도 이 결론을 그대로 따랐다
(`SidebarShell` 공유 + `AdminShell` 분리).

**배포·카메라** — 체크인 스캔은 브라우저 카메라(`getUserMedia`)를 쓰는데, 이는 보안 컨텍스트
(HTTPS 또는 `localhost`)에서만 열린다. HTTP 로만 서빙하면 스마트폰에서 카메라가 안 열린다.
Caddy 를 리버스 프록시로 쓰면 Let's Encrypt 발급·갱신이 자동이라 해소된다. 원리는
`~/Downloads/expo-docs/09-DEPLOY-HTTPS.md` 에 정리했다.

---

## 7. 범위 결정 (그대로 유효)

### 7-1. 배너 3개 화면 — 제외
`banner` 도메인에 컨트롤러가 없다. 정산까지 끝낸 뒤 D 갈래 담당이 마무리한다 — [이슈 #103](https://github.com/expo-platform-team4/expo/issues/103).

### 7-2. `Expo Exhibition Service Auth Flow` (`a6bf4b3a`) — 제외
영문 제목·다른 캔버스 크기·빈 스크린샷으로 초기 실험 산출물로 판단했다. 휴대폰 본인인증 SMS 실연동은 [이슈 #104](https://github.com/expo-platform-team4/expo/issues/104).

### 7-3. 체크인 3개 화면 — 디자인 없이 자유 생성
"스캐너 = 박람회 개최자(CLIENT)" 전제는 백엔드 `ExpoHostVerifier` 가 이미 강제한다.
카메라가 안 되는 환경을 대비해 **코드 수동 입력을 스캔과 동등한 탭으로** 노출했다 —
카메라 실패 시 대체가 아니라 항상 고를 수 있다.

### 7-4. 공통 레이아웃 셸 3종
```
Public          홈 · 목록 · 상세 · 로그인 · 가입 · 예매        헤더만
Sidebar(light)  마이페이지 + 클라이언트 포털                  헤더 + 밝은 사이드바 (공유)
Admin(dark)     관리자 전체                                  다크 사이드바, 헤더 없음, 프로필 하단
```

---

## 8. 남은 일

1. **결제 완료 경로** — 이슈 #107 에서 유일하게 남은 항목이다. 주문을 `PAID` 로 만드는
   길이 없어서 발권·정산·체크인이 여전히 SQL 시드에 의존한다. `payment`·`refund` 패키지가
   `package-info.java` 뿐이다.
2. **장소 예약 확정 화면** — 3절 주 참고. 지금은 API 직접 호출이 필요하다.
3. **OAuth2 콜백 계약 확정** — 1절 주 참고.
4. **`venue` 모듈** — 대응 화면이 없어 스텁으로 둔다. 장소 카탈로그 관리 화면이 생기면 채운다.

로컬에서 화면을 실제 데이터로 보려면
`backend/src/main/resources/db/seed/local_full_flow_expo7.sql` 을 쓴다 — 판매부터 정산까지
한 번에 채운다(파일 상단 주석에 실행법).

---

*2026-08-19 Stitch MCP 조사와 결정으로 시작해, 2026-08-20 구현 완료 시점 기준으로 갱신했다.*
