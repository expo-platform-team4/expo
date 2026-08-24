# QA 보고서 — 모집공고·참가신청·부스·개최신청 도메인 (2026-08-24)

`local_dev_snapshot_20260824.sql`(setval 보정판) 로드 후 주최사(CLIENT)·참가기업(CLIENT)·관리자
(ADMIN) 계정으로 브라우저에서 실제 조작하며 확인했다.

## 사용한 테스트 계정

| 계정 | role | 비밀번호 |
|---|---|---|
| e2e-client@example.com (e2e_client) | CLIENT — 박람회 7·8·9·10 소유(주최사) | `Test1234!` |
| e2e-exhibitor@example.com (e2e_exhibitor) | CLIENT — 참가기업, 부스 A-101 배정됨 | `Test1234!` |
| e2e-admin@example.com | ADMIN | `Test1234!` |
| seed-client@example.com | CLIENT | **불명**(로그인 401) — 박람회 id=1 소유주지만 못 씀 |

## 결과 요약

| 플로우 | 결과 |
|---|---|
| 5. 모집공고 요청→승인→공고 생성·게시 | ✅ |
| 6. 참가 신청→검토→결과 | ⚠️ 신청까지는 ✅, 심사·결과 생성은 **관리자 화면 없음** |
| 7. 부스 배정→콘텐츠→부스상품 주문·결제 | ⚠️ 화면 자체는 ✅, 콘텐츠 검수만 관리자 화면 없음 |

## 발견 사항

### 1. [중요] 관리자 운영 화면 4종 — 백엔드만 있고 프론트 없음

`docs/screen-api-map.md` 4절에 정리해 뒀다. 요약하면:

| 무엇 | API는 있음 | 문제 |
|---|---|---|
| 장소 예약 확정 | `POST /api/admin/venue-reservations` | 모집공고 생성 전 필수 단계인데 화면이 없어 관리자가 API 직접 호출해야 함 |
| 참여 신청 심사 | `AdminParticipationApplicationController` | 화면 없음. sidebar "공고 신청 관리"는 다른 기능(모집공고 **요청** 관리)이라 헷갈림 |
| 모집 결과 생성(합격/불합격) | `AdminRecruitmentResultService` | CLIENT 조회 화면만 있고 admin 생성 화면 없음 |
| 부스 콘텐츠 검수 | `AdminBoothContentController` | 부스 소개 콘텐츠가 "검수 중"에서 영원히 안 넘어감 |

**영향**: "기업이 공고에 신청 → 관리자가 심사 → 합격 → 부스 배정 → 콘텐츠 등록 → 관리자 검수"라는
정상 파이프라인이 admin 쪽 4곳에서 전부 막혀 있다. 지금 seed에 있는 "배정된 부스 A-101"·"확정된
정산" 등은 전부 이 화면들을 거치지 않고 SQL/API로 직접 만든 것 — 실제 운영에서도 관리자가 매번
Postman 등으로 API를 직접 호출해야 한다는 뜻이다. Function.md 원 설계(33개 화면)에 애초에 없던
화면들이라 "버그"는 아니지만, 운영 관점에서는 명백한 공백이다. 새 이슈로 등록할지, #107 처럼
별도 추적할지는 팀 판단이 필요하다.

### 2. [문서 오류, 수정 완료] `screen-api-map.md`의 개최신청·승인 상태 표기가 낡아 있었음
"박람회 개최 신청"·"박람회 개최 승인 관리" 두 화면이 본문 표엔 ⚠️(API 없음)로 남아있었지만,
실제로는 `ClientExpoOpeningRequestController`/`AdminExpoOpeningRequestController` +
프론트 페이지까지 전부 완성돼 있었다(문서 각주엔 "이슈#116/PR#117로 해소"라고 이미 적혀있었는데
본문 표만 안 고쳐진 상태). → 본문 표 수정 완료, 4절 "준비 중 화면" 요약과 최상단 상태 줄도
같이 갱신하고 관리자 운영 API 4종 공백을 새 항목으로 추가.

### 3. [백엔드 버그, 수정 완료] 참가 기업이 자기가 개최 요청한 공고에 자기가 신청 가능했던 문제
`ParticipationApplicationService.create()`에 호스트-신청자 동일인 검증이 없었음. 신규
`ErrorCode.CANNOT_APPLY_TO_OWN_NOTICE`(403) 추가, 검증 로직 추가, 테스트 케이스 추가(9개 전부
통과), 실제 계정(e2e_client → 자기 공고 신청 시도)으로 403 확인. `e2e_exhibitor`(다른 회사)의
정상 신청은 그대로 통과하는 것도 재확인.

## 테스트 데이터 상태
- 모집공고 `#2 테스트 공고 제목` → 게시됨(모집중), `e2e_exhibitor`가 정상 신청 1건 접수 상태.

## 검증 커맨드
```
cd backend && ./gradlew compileJava compileTestJava
cd backend && ./gradlew test --tests "com.expo.participation.service.ParticipationApplicationServiceTest"
```
