# 피부 이미지 분석 연동 (Spring ↔ analyze FastAPI) — v2

> **이 문서의 목적**
> Claude Code가 이 문서 하나만 보고 **API 4개 구현 → 통합 테스트**까지 진행할 수 있도록 정리한 작업 지시서.
> 담당 범위는 **피부 이미지 분석 연동**뿐이며, 플랜 생성 / 리포트 / 기여도 계산은 다른 담당 영역이라 건드리지 않는다.
>
> **v2 변경점**: 팀(윤환) 확정 사항 반영 + 실제 소스(`skin_metrics/api`)로 스펙 재확인. STEP 1(도커 실행/스펙 확인)은 이미 완료됨.

---

## 0. 한 줄 요약

Spring(8080)에서 이미 완성된 **analyze FastAPI 서버(localhost:8000)** 를 호출해 얼굴 사진 URL을 분석하고,
그 결과를 **팀 공용 클라우드 Redis에서 직접 읽어** 프론트에 반환한다.
이미지 분석은 3~7초 걸리므로 **비동기 + Redis 폴링** 구조다.

---

## 1. 팀 확정 사항 (★ 반드시 이대로 구현)

| 항목 | 확정 내용 |
|---|---|
| **Redis** | **하나로 통합.** 로컬 Redis 버리고 **팀 클라우드 Redis(redislabs, db 0) 하나만** 사용. 카카오 로그인 토큰/블랙리스트도 이 클라우드 Redis에 저장. 키가 안 겹쳐서 충돌 없음 (로그인=email/`blacklist:` 키, 분석=`{request_id}:analyze` / `:diary`). |
| **담당 범위** | **값만 받아 넘기기까지.** result/diary 테이블 DB insert는 **다른 담당**. 나는 분석 결과를 Redis에서 읽어 프론트에 반환만. (DB 저장 X, 필드 매핑·소수 캐스팅 고민 없음) |
| **조회 결과 없을 때** | **404 반환.** `done`이면 결과값, 그 외(키 없음/`processing`/`failed`)는 404. 프론트가 로딩 화면에서 200 올 때까지 폴링. |
| **kind 구분** | **엔드포인트로 구분** (body 파라미터 아님). `/analyze` vs `/analyze/diary`. |
| **포트** | analyze 서버 = **8000** 확정. |

---

## 2. 전체 구조

```
[프론트] ←→ [Spring : 8080] ─(REST 호출)→ [analyze FastAPI : 8000]
                  │                               │
                  │                               ▼ (결과 저장)
                  └──────(직접 읽기)──────→ [클라우드 Redis (팀 공용, db 0)]
```

- **Spring (8080)**: 내가 짜는 백엔드. analyze 호출 + Redis 결과 조회.
- **analyze FastAPI (8000)**: 얼굴 분석 모델. `proof-face` 레포. **이미 완성됨. 도커로 로컬 실행만.**
- **클라우드 Redis**: 분석 결과 임시 저장(TTL 3600초 = 1시간). Spring이 여기서 직접 읽음.
- **S3**: 사진 → URL 변환. **내 담당 아님.** 완성된 `image_url`만 받아 넘긴다.

---

## 3. 요청/조회 흐름

### 요청 (분석 시작)
```
① (프론트) 얼굴 사진 S3 업로드 → image_url 확보   ← 내 담당 아님
② Spring → analyze 서버:  POST http://localhost:8000/analyze
                          body: { "image_url": "https://.../face.jpg" }
③ analyze 서버 즉시 202 응답: { request_id, redis_key }
④ analyze 서버가 백그라운드로 분석 → 완료 시 결과를 Redis({request_id}:analyze)에 저장
⑤ Spring → 프론트: request_id 반환
```

### 조회 (결과 확인 - 폴링)
```
① 프론트가 request_id 들고 주기적으로 조회
② Spring이 {request_id}:analyze 키로 클라우드 Redis 직접 읽기
③-a 키 없음 / status=processing / status=failed → 404
③-b status=done → result 반환
```

- **id 두 개**: `request_id`(프론트 몫, 사용자 식별자) / `redis_key`(백엔드 몫, `{request_id}:{kind}`)
- 분석 완료까지 대략 3~7초.

---

## 4. 실제 API 스펙 (소스로 확정)

### analyze 서버 요청 (Spring이 호출)

`POST http://localhost:8000/analyze` (또는 `/analyze/diary`), `Content-Type: application/json`

**Body**

| Key | Type | 필수 | 설명 |
|---|---|---|---|
| image_url | String(http/https) | O | 공개 접근 가능한 얼굴 이미지 URL |
| reference_bbox | int[4] | X | 그레이패치 `[x,y,w,h]`. 실제 패치 있을 때만. 보통 생략 |

```json
{ "image_url": "https://example.com/face.jpg" }
```

### 202 즉시 응답 (⚠️ 필드 2개뿐 — status/version 없음)

```json
{
  "request_id": "470b634e92bd44b9abeb12accb0f0b70",
  "redis_key":  "470b634e92bd44b9abeb12accb0f0b70:analyze"
}
```

### Redis에 저장되는 문서 (JSON **문자열**)

```json
// 진행 중
{"status":"processing","request_id":"...","kind":"analyze","submitted_at":"..."}
// 완료
{"status":"done","request_id":"...","kind":"analyze","submitted_at":"...","completed_at":"...","result":{ ... }}
// 실패
{"status":"failed","request_id":"...","kind":"analyze","submitted_at":"...","completed_at":"...","error":{"code":"...","message":"..."}}
```

**result — kind별 2종류**

`analyze` (0~100):
```json
{ "pigmentation": 38.39, "erythema": 55.52, "hydration": 70.80,
  "confidence": { "pigmentation": 0.6, "erythema": 0.6, "hydration": 0.6 } }
```

`diary` (0~10):
```json
{ "skin_tone": 8.8, "dryness": 3.1, "redness": 9.0,
  "confidence": { "skin_tone": 0.6, "dryness": 0.36, "redness": 0.6 } }
```

---

## 5. 구현할 함수 4개

DB 저장 없음. Redis에서 읽어 그대로 반환.

| # | 기능 | Method | analyze 서버 호출 | 결과 조회 키 |
|---|---|---|---|---|
| 1 | 피부 이미지 분석 요청 | POST | `POST /analyze` | — (request_id 반환) |
| 2 | 피부 이미지 분석 결과 조회 | GET | — | `{request_id}:analyze` |
| 3 | 체험 피부 이미지 분석 요청 | POST | `POST /analyze/diary` | — (request_id 반환) |
| 4 | 체험 피부 이미지 분석 결과 조회 | GET | — | `{request_id}:diary` |

---

## 6. STEP 0 — GitHub 이슈 + 브랜치 (반드시 코딩 전에)

> 팀 규칙: `develop`/`main`에 바로 짜지 않는다. 이슈 → 브랜치 → 작업 → PR.

**이슈 2개로 분리** (요청+조회는 짝이라 한 이슈로):
- **이슈 1**: `[Feature] 피부 이미지 분석 API 연동 (analyze)`
- **이슈 2**: `[Feature] 체험 피부 이미지 분석 API 연동 (diary)`

**브랜치 네이밍 = 팀 컨벤션 `feat/#<이슈번호>`** (예: `feat/#42`). ⚠️ `feature/skin-analysis-...` 아님.

```bash
git checkout develop
git pull
git checkout -b "feat/#<이슈번호>"   # PowerShell에선 따옴표 필수 (# 때문)
git branch --show-current            # feat/#N 확인 후 작업
```
analyze 브랜치 먼저 끝내고, diary는 별도 브랜치로.

---

## 7. STEP 1 — analyze 서버 도커 실행 (✅ 완료됨)

이미 검증 완료: 도커로 서버 기동(`/healthz` 200 = Redis 연결 OK), `/docs`에서 엔드포인트 4개 확인, 실제 얼굴 분석 작동 확인(팀원).

**다시 띄울 때**:
```bash
cd <proof-face 경로>
docker compose up          # 최초 1회만 --build (빌드 완료됨)
# http://localhost:8000/docs 로 확인
```
`.env`의 Redis 값은 팀 클라우드(redislabs)로 이미 설정됨. **비밀번호는 카톡 참고 — 이 문서에 평문으로 넣지 말 것.**
※ 포트 8000이 다른 프로세스에 점유되면(`netstat -ano | findstr :8000`), 그 프로세스 종료하거나 compose 포트를 8001로.

---

## 8. STEP 2 — Spring 연동 구현

### (1) Redis 설정 — 클라우드로 통합

`application-local.yml`의 redis를 **팀 클라우드**로 교체 (로컬 localhost:6379 → redislabs). 로그인 토큰도 여기 저장됨.
```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:redis-12829.c340.ap-northeast-2-1.ec2.cloud.redislabs.com}
      port: ${REDIS_PORT:12829}
      username: ${REDIS_USERNAME:default}
      password: ${REDIS_PASSWORD:}   # 실제 값은 env/카톡. 평문 커밋 금지
      ssl:
        enabled: false               # analyze 서버 TLS=0 과 동일
```

### (2) ⚠️ Redis 직렬화 — 제일 실수하기 쉬운 곳

analyze 서버는 결과를 **평문 JSON 문자열**로 저장한다 (`redis.set(key, json.dumps(...))`, `decode_responses=True`).
Spring의 `RedisTemplate` 기본 직렬화(JDK)로는 **이 문자열을 못 읽는다**.
→ **`StringRedisTemplate`(또는 `StringRedisSerializer`)로 값을 String으로 읽고, `ObjectMapper.readTree/readValue`로 파싱**해야 한다.
(기존 JWT용 RedisService/RedisTemplate 설정을 덮어쓰지 말 것. 문자열 읽기 전용 접근을 별도로 쓰거나, 조회 시 String으로 get.)

### (3) analyze 서버 URL — yml 분리 (하드코딩 금지)

`application.yml`:
```yaml
services:
  analyze:
    url: ${SERVICES_ANALYZE_URL:http://localhost:8000}
```

### (4) DTO (record) — snake_case 매핑 주의

FastAPI는 snake_case(`image_url`, `request_id`, `skin_tone` …). Java record는 camelCase 권장이므로 **`@JsonProperty`로 매핑**.
```java
// 요청 (Spring → analyze). 보낼 때 image_url 로 나가야 함
public record AnalyzeRequestDto(
    @JsonProperty("image_url") String imageUrl,
    @JsonProperty("reference_bbox") int[] referenceBbox  // 보통 null
) {}

// 202 응답 (필드 2개뿐)
public record AnalyzeAcceptedDto(
    @JsonProperty("request_id") String requestId,
    @JsonProperty("redis_key")  String redisKey
) {}

// analyze 결과
public record AnalyzeResultDto(
    double pigmentation, double erythema, double hydration,
    Map<String, Double> confidence
) {}

// diary 결과
public record DiaryResultDto(
    @JsonProperty("skin_tone") double skinTone,
    double dryness, double redness,
    Map<String, Double> confidence
) {}
```

### (5) analyze 서버 호출 클라이언트 (RestClient)

- `RestClient`로 `POST {services.analyze.url}/analyze` (diary는 `/analyze/diary`) 호출 → `AnalyzeAcceptedDto` 수신.
- 프론트엔 `request_id`만 반환.
- 참고: `KakaoOAuthClient`의 RestClient 사용 패턴을 참고해 **전용 `AnalyzeClient`로 새로 작성** (KakaoOAuthClient엔 analyze 샘플 없음).

### (6) 결과 조회 (Redis 직접 읽기)

- key = `{request_id}:analyze` (또는 `:diary`).
- String으로 get → null이면 **404**.
- JSON 파싱해서 `status` 확인: `done`이면 `result` 반환, 그 외(`processing`/`failed`)면 **404**.
- 404는 프로젝트 공통 `ErrorCode.DATA_NOT_FOUND`(C404) 재사용, 응답은 `GlobalResponse` 엔벨로프.

### (7) 컨트롤러 4개 (엔드포인트 경로 예시 — 팀과 최종 조율)

```
POST /api/v1/skin-analysis            body:{imageUrl}  → {requestId}
GET  /api/v1/skin-analysis/{requestId}                 → analyze 결과 or 404
POST /api/v1/skin-analysis/diary      body:{imageUrl}  → {requestId}
GET  /api/v1/skin-analysis/diary/{requestId}           → diary 결과 or 404
```
(컨트롤러는 `GlobalResponse.ok(...)`로 래핑, 서비스는 순수 DTO 반환 — 팀 컨벤션.)

### 구현 순서
1. `application-local.yml` redis → 클라우드 / `application.yml` analyze URL 추가
2. DTO(record) 4~5개 정의
3. `AnalyzeClient` (RestClient, POST /analyze·/analyze/diary)
4. Redis String 조회 + JSON 파싱 리더
5. 서비스: kind(analyze/diary) 분기, done 아니면 404
6. 컨트롤러 4개
7. **STEP 1 도커 서버 띄운 채로 통합 테스트** (실제 얼굴 URL로 요청→폴링→결과)

---

## 9. 남은 확인 (거의 없음)

- 조회 시 `processing`/`failed`도 전부 404로 통일할지(현재 방향) vs `failed`는 에러 메시지로 구분할지 → 프론트와 합의(기본: 전부 404).
- 컨트롤러 경로(`/api/v1/skin-analysis...`)는 위 예시 기준, 프론트/팀 API 명세와 최종 일치시킬 것.

---

## 10. 주의 함정 모음 (꼭 읽기)

1. **Redis 직렬화**: analyze가 넣은 평문 JSON을 JDK 직렬화로 읽으면 깨짐 → String으로 읽고 Jackson 파싱. (JWT용 Redis 설정 덮어쓰지 말 것)
2. **202 응답은 `{request_id, redis_key}` 2개뿐** — status/version 없음.
3. **kind = 엔드포인트**(`/analyze`, `/analyze/diary`), body 파라미터 아님.
4. **DB 저장 안 함** — 값만 반환.
5. **결과 없거나 done 아니면 404**.
6. **snake_case ↔ camelCase**: DTO에 `@JsonProperty` 필수 (`image_url`, `request_id`, `redis_key`, `skin_tone`).
7. **PowerShell 브랜치명**: `git checkout -b "feat/#42"` — `#` 때문에 따옴표 필수.
8. **Redis 비밀번호 평문 금지**: yml 기본값 비우고 env/카톡. `[Sensitive]` 페이지 외 노출 주의.

---

## 참고 (하류 — 내가 만들지 않음)
- analyze 결과 → result 테이블 → 플랜 생성 입력 (다른 담당)
- diary 결과 → diary 테이블 → 기여도 계산 입력 (다른 담당)
