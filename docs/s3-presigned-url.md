# S3 presigned URL — 로컬에서 잡히지 않는 버그

## 요약

로컬 S3 에뮬레이터(Adobe S3Mock)는 presigned URL 을 **받아주기만 하고 검증하지 않는다.**
서명·만료시간·HTTP 메서드 중 무엇이 틀려도 로컬에서는 정상 동작한다.
같은 코드가 운영의 실제 S3 에서는 `403 SignatureDoesNotMatch` 나 `AccessDenied` 로 실패한다.

**이 영역은 로컬 테스트가 통과해도 아무것도 보장하지 않는다.**

## 언제 해당되는가

presigned URL 을 쓸 때만 해당된다. 업로드 방식에 따라 갈린다.

| 방식 | presigned URL | 이 문서 |
|-|-|-|
| 서버 경유 — 브라우저 → Spring → S3 | 안 씀 | 해당 없음 |
| 직접 업로드 — 브라우저 → S3, Spring 은 URL 만 발급 | 씀 | **해당** |
| 비공개 이미지 조회 — presigned GET | 씀 | **해당** |

박람회 포스터나 부스 이미지처럼 파일이 커지면 서버 경유는 Spring 이 트래픽을 전부 받아내야 해서
직접 업로드로 가게 될 가능성이 높다. 그 설계를 택하는 순간 이 문서가 유효해진다.

## S3Mock 이 검증하지 않는 것

Adobe S3Mock 문서 기준으로 presigned URL 은 "accepted but not validated" 다.
구체적으로 아래 셋을 확인하지 않는다.

- **서명(signature)** — 서명값이 깨져 있어도 통과한다
- **만료시간(expiration)** — 이미 만료된 URL 도 통과한다
- **HTTP 메서드(verb)** — PUT 용으로 발급한 URL 로 GET 을 해도 통과한다

자격증명도 검증하지 않는다. 그래서 `test/test` 같은 더미 값으로 동작하는 것이다.

## 로컬을 통과하지만 운영에서 터지는 것들

1. **만료시간 단위 실수** — `Duration.ofMinutes(10)` 을 쓸 자리에 `Duration.ofSeconds(10)` 을 썼다.
   로컬은 만료를 안 보므로 며칠이 지나도 잘 된다. 운영에서는 10초 뒤부터 전부 실패한다.

2. **만료시간이 상한을 넘음** — SigV4 presigned URL 의 최대 유효기간은 7일(604800초)이다.
   "넉넉하게 30일" 같은 값을 넣으면 실제 S3 는 URL 생성 시점이나 사용 시점에 거부한다.

3. **서명에 포함되지 않은 헤더를 클라이언트가 붙임** — 서버는 `Content-Type` 없이 서명했는데
   프론트에서 `axios.put(url, file)` 이 `Content-Type` 을 자동으로 붙이는 경우가 대표적이다.
   서명 대상이 달라져 실제 S3 는 `SignatureDoesNotMatch` 를 낸다. 로컬은 그냥 받아준다.

4. **메서드 불일치** — 업로드용(PUT)으로 발급하고 조회(GET)에 쓰거나 그 반대.

5. **서버 시계 오차** — 컨테이너·서버 시계가 15분 이상 틀어지면 실제 S3 는 거부한다.
   로컬에서는 드러나지 않는다.

6. **리전·엔드포인트 불일치** — 서명은 리전을 포함한다.
   설정상 `ap-northeast-2` 로 서명해놓고 다른 리전 엔드포인트로 요청하면 실패한다.
   S3Mock 은 리전 자체를 검증하지 않아 이 불일치가 로컬에서 보이지 않는다.

## 어떻게 잡을 것인가

### 1) 코드 리뷰 체크리스트

presigned URL 을 다루는 PR 에서는 아래를 눈으로 확인한다. 테스트는 근거가 되지 못한다.

- [ ] 만료시간의 **단위**가 의도한 것인가 (`ofMinutes` / `ofSeconds` / `ofHours`)
- [ ] 만료시간이 **7일 이하**인가
- [ ] 발급한 **HTTP 메서드**와 프론트에서 실제로 쓰는 메서드가 같은가
- [ ] 서명에 포함한 헤더 집합과 프론트가 보내는 헤더가 정확히 일치하는가
      (특히 `Content-Type`, `Content-Length`, 커스텀 `x-amz-*`)
- [ ] 만료시간이 하드코딩이 아니라 설정값(`application.yml`)으로 빠져 있는가
- [ ] 만료된 URL 을 받았을 때 프론트가 **재발급을 요청**하는 경로가 있는가
      (운영에서는 반드시 만료가 발생한다. 로컬에서는 한 번도 안 겪는다)

### 2) S3Mock 이 정말 검증하지 않는지 직접 확인

말로만 믿지 말고 한 번 눈으로 보는 편이 낫다. 만료 1초짜리 URL 을 만들어 한참 뒤에 써본다.

```bash
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=ap-northeast-2

# 1초 뒤 만료되는 조회용 URL 발급
URL=$(aws --endpoint-url http://localhost:9090 \
  s3 presign s3://expo-local/probe.txt --expires-in 1)

sleep 5
curl -s -o /dev/null -w '%{http_code}\n' "$URL"
```

**200 이 나오면** S3Mock 이 만료를 무시한다는 뜻이다(예상되는 결과).
실제 S3 라면 같은 요청이 `403 AccessDenied` 와 함께 `Request has expired` 를 돌려준다.

서명 검증도 같은 방식으로 확인할 수 있다. 위 URL 끝의 `X-Amz-Signature` 값 한 글자를 바꾼 뒤
다시 요청해서 여전히 200 이 나오는지 본다.

### 3) 배포 전 실제 S3 로 한 번은 확인

presigned URL 기능은 **운영 환경에 처음 올릴 때 반드시 손으로 한 번 확인한다.**
로컬에서 아무리 돌려도 검증된 것이 없기 때문이다. 최소한 아래 두 가지를 본다.

- 정상 URL 로 업로드·조회가 되는가
- 만료시간이 지난 뒤 실제로 거부되는가 (만료가 동작하지 않으면 비공개 파일이 영구 공개된다)

## 왜 S3Mock 을 쓰는가

이 제약을 알고도 S3Mock 을 쓰는 이유는 대안이 마땅치 않기 때문이다.

- **LocalStack** — 2026-03-23 부터 `localstack/localstack:latest` 가 인증 토큰을 요구한다.
  팀원 각자 가입해야 하고, 데이터 영속성(`PERSISTENCE=1`)은 유료 플랜 기능이다.
- **MinIO** — 2025-10 부터 Docker 이미지 배포가 중단됐고 2026-04 저장소가 아카이브됐다.

S3Mock 은 Apache 2.0 이고 계정이 필요 없으며 영속성이 무료로 동작한다.
presigned URL 검증만 포기하면 나머지는 충분하다. 그 포기한 부분을 이 문서로 메운다.

## 참고

- [Adobe S3Mock](https://github.com/adobe/S3Mock) — presigned URL 미검증 서술
- [AWS — Presigned URL 제약](https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-presigned-url.html) — SigV4 최대 7일
- 로컬 인프라 구성: 저장소 루트 `docker-compose.yml`
- S3 동작 방식 전반: `README.md` 의 "Object Storage 동작 방식" 절
