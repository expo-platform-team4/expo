-- 모집공고 이력의 처리 유형에 ACTIVATE 를 허용한다.
--
-- 무엇이 깨져 있었나
--   RecruitmentNoticeActionType 에는 ACTIVATE 가 있는데 이 테이블의 CHECK 에는 없었다.
--   ACTIVATE 는 "예정된 신청 시작일이 도래해 시스템이 SCHEDULED 를 OPEN 으로 바꿨다" 는 기록으로,
--   RecruitmentNoticeService.processSchedule() 이 남긴다.
--
--   그래서 SCHEDULED -> OPEN 전환이 **항상 실패했다.** 게다가 그 메서드는 한 트랜잭션이라
--   같은 호출에서 처리하던 신청 마감(CLOSE)까지 통째로 롤백됐다 - 한 건이 막히면 전부 막힌다.
--
--   증상은 조용하다. 관리자는 "게시" 를 눌렀고 화면에도 게시된 것으로 보인다. 그런데 공개 목록은
--   OPEN 만 보여주므로, 신청 시작일을 미래로 잡은 공고는 **영영 공개되지 않는다.**
--
-- 왜 안 잡혔나
--   ddl-auto: validate 는 컬럼과 타입만 대조한다. CHECK 제약의 내용은 보지 않는다.
--   엔티티와 스키마가 이런 식으로 어긋나면 기동도 통과하고, 그 값을 실제로 INSERT 하는 경로가
--   실행될 때까지 아무 일도 일어나지 않는다.

ALTER TABLE recruitment_notice_histories
    DROP CONSTRAINT IF EXISTS recruitment_notice_histories_action_type_check;

ALTER TABLE recruitment_notice_histories
    ADD CONSTRAINT recruitment_notice_histories_action_type_check
    CHECK (action_type IN ('CREATE', 'PUBLISH', 'ACTIVATE', 'UPDATE', 'CLOSE', 'CANCEL', 'ARCHIVE'));
