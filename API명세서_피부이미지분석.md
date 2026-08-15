# API 명세서 — 피부 이미지 분석 (analyze / diary)

> 담당: 신석훈 · 도메인: Analyze · 인증: JWT(USER)
> 공통 응답 엔벨로프: `{ "data": ..., "error": { "code": ..., "message": ... } }`
> 비동기 구조: 요청 시 즉시 `requestId` 반환 → 프론트가 결과를 폴링(진행 중이면 404, done이면 200)

---

# 1. 피부 이미지 분석 요청 (analyze)

## Summary

| 항목 | 내용 |
|---|---|
| Endpoint | `/api/v1/analyses` |
| Method | POST |
| 설명 | 얼굴 이미지 URL을 analyze 서버로 보내 분석을 시작하고, 폴링용 requestId를 반환 (분석은 비동기, 결과는 Redis 저장) |
| 최초 작성일 | 2026-08-12 |
| 최종 수정일 | 2026-08-12 |

## Request

### Headers

| Key | Type | Description | Example |
|---|---|---|---|
| Authorization | String | JWT Access Token | Bearer eyJhbGciOi... |
| Content-Type | String | 요청 본문 타입 | application/json |

### Request Body

| Key | Type | Description | 필수 | Example |
|---|---|---|---|---|
| imageUrl | String | 공개 접근 가능한 얼굴 이미지 URL (http/https) | O | https://.../face.jpg |

### Example

```json
{
  "imageUrl": "https://example.com/face.jpg"
}
```

## Response

### 요청 성공 (200 OK)

```json
{
  "data": {
    "requestId": "470b634e92bd44b9abeb12accb0f0b70"
  },
  "error": {
    "code": null,
    "message": null
  }
}
```

### Response Body

| Key | Type | Description | Example |
|---|---|---|---|
| requestId | String | 결과 조회(폴링)에 사용할 요청 식별자 | 470b634e92bd44b9abeb12accb0f0b70 |

### 요청 실패

```json
{
  "data": null,
  "error": {
    "code": "C002",
    "message": "유효하지 않은 파라미터입니다."
  }
}
```

### 발생할 수 있는 오류 코드

| HTTP Status | Code | Message | 발생 상황 |
|---|---|---|---|
| 400 | C002 | 유효하지 않은 파라미터입니다. | imageUrl 누락/빈 값 |
| 401 | C4011 | 인증 정보가 유효하지 않습니다. | 토큰 미첨부 / 만료 |
| 502 | C502 | 이미지 분석 서버 호출에 실패했습니다. | analyze 서버(:8000) 연결/응답 실패 |
| 500 | C500 | 오류가 발생하였습니다. | 서버 내부 오류 |

---

# 2. 피부 이미지 분석 결과 조회 (analyze)

## Summary

| 항목 | 내용 |
|---|---|
| Endpoint | `/api/v1/analyses/{requestId}` |
| Method | GET |
| 설명 | requestId로 Redis(`{requestId}:analyze`)를 조회. done이면 결과 반환, 그 외(없음/진행중/실패)는 404 |
| 최초 작성일 | 2026-08-12 |
| 최종 수정일 | 2026-08-12 |

## Request

### Headers

| Key | Type | Description | Example |
|---|---|---|---|
| Authorization | String | JWT Access Token | Bearer eyJhbGciOi... |

### Path Variable

| Key | Type | Description | 필수 | Example |
|---|---|---|---|---|
| requestId | String | 요청 시 받은 요청 식별자 | O | 470b634e92bd44b9abeb12accb0f0b70 |

### Request Body

없음

## Response

### 요청 성공 (200 OK)

```json
{
  "data": {
    "pigmentation": 29.01,
    "erythema": 13.82,
    "hydration": 99.29,
    "confidence": {
      "pigmentation": 0.6,
      "erythema": 0.6,
      "hydration": 0.6
    }
  },
  "error": {
    "code": null,
    "message": null
  }
}
```

### Response Body

| Key | Type | Description | Example |
|---|---|---|---|
| pigmentation | Double | 색소침착 정도 (0~100) | 29.01 |
| erythema | Double | 홍조 정도 (0~100) | 13.82 |
| hydration | Double | 수분 정도 (0~100) | 99.29 |
| confidence | Object | 지표별 신뢰도(0~1). pigmentation/erythema/hydration 키 | {"pigmentation":0.6,...} |

### 요청 실패

```json
{
  "data": null,
  "error": {
    "code": "C404",
    "message": "정보를 불러올 수 없습니다."
  }
}
```

### 발생할 수 있는 오류 코드

| HTTP Status | Code | Message | 발생 상황 |
|---|---|---|---|
| 401 | C4011 | 인증 정보가 유효하지 않습니다. | 토큰 미첨부 / 만료 |
| 404 | C404 | 정보를 불러올 수 없습니다. | 결과 없음 / 분석 진행중(processing) / 실패(failed) |
| 500 | C500 | 오류가 발생하였습니다. | 서버 내부 오류 |

---

# 3. 체험 피부 이미지 분석 요청 (diary)

## Summary

| 항목 | 내용 |
|---|---|
| Endpoint | `/api/v1/analyses/diary` |
| Method | POST |
| 설명 | 얼굴 이미지 URL을 analyze 서버(`/analyze/diary`)로 보내 체험 분석을 시작하고, 폴링용 requestId를 반환 (비동기, 결과는 Redis 저장) |
| 최초 작성일 | 2026-08-12 |
| 최종 수정일 | 2026-08-12 |

## Request

### Headers

| Key | Type | Description | Example |
|---|---|---|---|
| Authorization | String | JWT Access Token | Bearer eyJhbGciOi... |
| Content-Type | String | 요청 본문 타입 | application/json |

### Request Body

| Key | Type | Description | 필수 | Example |
|---|---|---|---|---|
| imageUrl | String | 공개 접근 가능한 얼굴 이미지 URL (http/https) | O | https://.../face.jpg |

### Example

```json
{
  "imageUrl": "https://example.com/face.jpg"
}
```

## Response

### 요청 성공 (200 OK)

```json
{
  "data": {
    "requestId": "2567ef73135c488e9c5d6372442acd11"
  },
  "error": {
    "code": null,
    "message": null
  }
}
```

### Response Body

| Key | Type | Description | Example |
|---|---|---|---|
| requestId | String | 결과 조회(폴링)에 사용할 요청 식별자 | 2567ef73135c488e9c5d6372442acd11 |

### 요청 실패

```json
{
  "data": null,
  "error": {
    "code": "C002",
    "message": "유효하지 않은 파라미터입니다."
  }
}
```

### 발생할 수 있는 오류 코드

| HTTP Status | Code | Message | 발생 상황 |
|---|---|---|---|
| 400 | C002 | 유효하지 않은 파라미터입니다. | imageUrl 누락/빈 값 |
| 401 | C4011 | 인증 정보가 유효하지 않습니다. | 토큰 미첨부 / 만료 |
| 502 | C502 | 이미지 분석 서버 호출에 실패했습니다. | analyze 서버(:8000) 연결/응답 실패 |
| 500 | C500 | 오류가 발생하였습니다. | 서버 내부 오류 |

---

# 4. 체험 피부 이미지 분석 결과 조회 (diary)

## Summary

| 항목 | 내용 |
|---|---|
| Endpoint | `/api/v1/analyses/diary/{requestId}` |
| Method | GET |
| 설명 | requestId로 Redis(`{requestId}:diary`)를 조회. done이면 결과 반환, 그 외(없음/진행중/실패)는 404 |
| 최초 작성일 | 2026-08-12 |
| 최종 수정일 | 2026-08-12 |

## Request

### Headers

| Key | Type | Description | Example |
|---|---|---|---|
| Authorization | String | JWT Access Token | Bearer eyJhbGciOi... |

### Path Variable

| Key | Type | Description | 필수 | Example |
|---|---|---|---|---|
| requestId | String | 요청 시 받은 요청 식별자 | O | 2567ef73135c488e9c5d6372442acd11 |

### Request Body

없음

## Response

### 요청 성공 (200 OK)

```json
{
  "data": {
    "skin_tone": 10,
    "dryness": 0.1,
    "redness": 1.4,
    "confidence": {
      "skin_tone": 0.6,
      "dryness": 0.36,
      "redness": 0.6
    }
  },
  "error": {
    "code": null,
    "message": null
  }
}
```

### Response Body

| Key | Type | Description | Example |
|---|---|---|---|
| skin_tone | Double | 피부톤 정도 (0~10) | 10 |
| dryness | Double | 건조도 정도 (0~10) | 0.1 |
| redness | Double | 붉은기 정도 (0~10) | 1.4 |
| confidence | Object | 지표별 신뢰도(0~1). skin_tone/dryness/redness 키 | {"skin_tone":0.6,...} |

### 요청 실패

```json
{
  "data": null,
  "error": {
    "code": "C404",
    "message": "정보를 불러올 수 없습니다."
  }
}
```

### 발생할 수 있는 오류 코드

| HTTP Status | Code | Message | 발생 상황 |
|---|---|---|---|
| 401 | C4011 | 인증 정보가 유효하지 않습니다. | 토큰 미첨부 / 만료 |
| 404 | C404 | 정보를 불러올 수 없습니다. | 결과 없음 / 분석 진행중(processing) / 실패(failed) |
| 500 | C500 | 오류가 발생하였습니다. | 서버 내부 오류 |
