# 복습 주기별 이메일 발송 로직 재고려 및 재설계

## 서론
현재 주기적으로 사용자가 저장했던 복습 자료를 이메일로 전송하는 기능이 핵심인 서비스를 운용하고 있음.
현재 구조에서 이메일을 영구적으로 보내지 못하는 케이스가 존재하는 것을 확인하였고, 이번 기회에
확장성 있고 데이터 안정성을 높일 수 있는 방안을 고려하고자 함.

---

## 용어 및 개념 정의

| 용어 | 정의 |
|---|---|
| 단기 주기 | 1일 미만의 복습 주기 (예: 10분, 1시간). 재시도 정책 미적용 |
| 장기 주기 | 1일 이상의 복습 주기 (예: 1일, 7일, 30일). 재시도 정책 적용 대상 |
| ReviewCycle | 복습 예정 시각 단위 레코드. review_id + scheduled_at으로 구성 |
| NotificationHistory | 발송 이력 레코드. review_cycle_id + status(PENDING/SENT/FAILED)로 구성 |

---

## 현재 구조

### 데이터 모델

```
review_cycle
  - review_id
  - scheduled_at

notification_history
  - review_cycle_id
  - status: PENDING | SENT | FAILED
```

### 발송 흐름

**1) 리뷰 저장 시**
- ReviewCycle 생성
- NotificationHistory(PENDING) INSERT (사전 기록)

**2) 매분 스케줄러 (ReviewEmailSender)**
- `scheduled_at = 현재시간(분 단위 절삭)` 정확 일치로 ReviewCycle 조회
- 사용자별 그룹화 후 @Async 비동기 발송
- 발송 성공: NotificationHistory(SENT) INSERT (새 레코드 추가)
- 발송 실패: NotificationHistory(FAILED) INSERT (새 레코드 추가)

**3) 60초 재시도 스케줄러 (EmailRetryScheduler)**
- 조건: SENT=0 AND FAILED>=1 AND FAILED<3 인 ReviewCycle 조회
- 대상 재발송 → NotificationHistory(SENT/FAILED) INSERT

---

## 이 구조를 선택한 이유

| 설계 결정 | 선택 이유 |
|---|---|
| `notification_history` append-only 방식 (상태 UPDATE 대신 신규 INSERT) | "n번 이하로 실패 시 재전송 처리" 로직을 FAILED 레코드 개수로 단순 구현하기 위함 |
| PENDING 사전 기록 | 이력 추적 목적 — "발송 예정 데이터는 존재하지만 이메일 전송 후 결과 저장에 실패한 케이스"를 식별하기 위함 |
| 매분 스케줄러 + `scheduled_at` 정확 일치 조회 | 단순 구현 우선 |

---

## 문제점

### 1. PENDING이 이력 추적 목적을 달성하지 못함
PENDING 상태가 남아있는 원인이 세 가지인데 구분이 불가능하다.

| 케이스 | 상태 |
|---|---|
| A) scheduled_at이 아직 도래하지 않음 (정상) | PENDING |
| B) scheduled_at이 지났으나 스케줄러 미실행 (서버 다운 등) | PENDING |
| C) 발송은 됐으나 결과 저장 실패 (원래 감지 목표) | PENDING |

→ C를 감지하기 위해 PENDING을 사전 기록했으나, A·B와 구별할 수단이 없다.

### 2. 서버 다운 시 PENDING 영구 누락
스케줄러가 특정 분에 실행되지 않으면 해당 ReviewCycle은:
- PENDING 상태로 영구히 남음
- 재시도 대상(`findAllRetryableCycles`)은 FAILED 기준이라 PENDING은 제외
- 결과적으로 해당 메일은 영원히 발송되지 않음

### 3. 중복 발송 위험 (원자성 부재)
```
sendOne() @Async
  ├─ 이메일 발송 성공
  └─ notificationHistoryService.saveAll(SENT) ← 여기서 예외 발생 시
       → DB는 여전히 PENDING
       → 재시도 로직이 이를 감지하지 못함 (FAILED 없음)
       → 재시도 없이 누락 or 다음 로직에서 중복 발송 위험
```

### 4. `findAllRetryableCycles`의 N+1 쿼리
JOIN FETCH 없이 ReviewCycle 목록을 조회한 후,
`reviewCycle.getReview().getMember()` 접근 시 N번 추가 쿼리 발생.

### 5. 재시도 로직이 단기/장기 주기를 구분하지 않음
`findAllRetryableCycles`는 `scheduled_at` 기준 없이 모든 FAILED를 재시도 대상으로 삼음.
의도적으로 재시도하지 않으려 했던 단기 주기(10분, 1시간)도 실패 시 재시도 대상에 포함됨.

### 6. append-only 구조로 인한 데이터 크기 증가
FAILED 기록을 notification_history 테이블에 계속 추가하는 구조이므로,
재시도가 반복될수록 동일 ReviewCycle에 대한 레코드 수가 누적됨.

---

## 대안

### 대안 A: 범위 조회 (scheduled_at <= now)

`findAllByScheduledAt`의 정확 일치 조건을 범위 조회로 변경.

- **해결**: 문제 2 (서버 다운 시 PENDING 누락)
- **미해결**: 문제 1 (PENDING 이력 추적 불가), 문제 3 (중복 발송 위험)
- **한계**: append-only 구조 유지로 데이터 증가 및 N+1 미해소

---

### 대안 B: fail_count 컬럼 + UPDATE 방식

`notification_history`에 `fail_count` 컬럼 추가 후 append-only 포기 → UPDATE 방식으로 전환.

- **해결**: 문제 6 (데이터 크기 증가)
- **미해결**: 문제 2 (범위 조회 미적용 시 서버 다운 누락)
- **한계**: 단독으로는 범위 조회와 결합해야 의미 있음

---

### 대안 C: notification_history를 outbox로 활용 (대안 A + B 통합)

`notification_history`를 review_cycle_id당 1개 레코드로 관리.
- `status` 필드를 INSERT 대신 UPDATE로 전환
- `fail_count`, `last_attempted_at` 컬럼 추가
- 범위 조회(`scheduled_at <= now`) + PENDING 필터로 서버 다운 누락 방지
- `fail_count < 3 AND scheduled_at <= now - 1일` 조건으로 재시도 단순화

**해결**: 문제 1·2·5·6
**수용**: 문제 3 — at-least-once 허용 (원자성 보장 포기, 중복 발송 가능성 수용)
**미해결**: 문제 4 — JOIN FETCH로 N+1 해소 (대안 C 내 포함)

---

### 대안 D: Kafka / SQS 도입

외부 메시지 큐로 이메일 발송을 이벤트 기반으로 처리.

- **해결**: 문제 3 (원자성), 문제 2 (재전송 보장)
- **한계**: 외부 인프라 필요, 현재 서비스 규모에 과도한 복잡도

---

## 최종 선택

**대안 C 채택**

### 선택 이유

| 기준 | 내용 |
|---|---|
| 인프라 추가 없음 | DB만으로 해결 가능 — Kafka/SQS 불필요 |
| 쿼리 단순화 | GROUP BY/HAVING 제거 → `status + fail_count` 단순 조건으로 대체 |
| 역할 명확화 | `notification_history`가 발송 예정 큐(outbox) + 이력 역할 겸임, append-only 구조 폐기 |
| N+1 해소 | `findAllRetryableCycles`에 JOIN FETCH 적용 (문제 4 해결 포함) |
| 단기 주기 분리 | `scheduled_at <= now - 1일` 조건으로 단기 주기 재시도 방지 (문제 5 해결) |

### 수용한 트레이드오프

- **원자성**: 이메일 발송 성공 후 DB UPDATE 실패 시 중복 발송 가능. at-least-once 의미론 수용.
- **이력 추적**: append-only 포기로 시도 이력 미보존 → 로그로 대체.
- **last_attempted_at**: 중복 발송 여부 판단 및 디버깅 보조 컬럼으로 추가.

---

## 구현

### 데이터 모델 변경

```
notification_history (변경 전)
  - review_cycle_id
  - status: PENDING | SENT | FAILED
  → review_cycle_id당 여러 레코드 존재 (append-only)

notification_history (변경 후)
  - review_cycle_id
  - status: PENDING | SENT | FAILED
  - fail_count: INT DEFAULT 0
  - last_attempted_at: DATETIME NULL
  → review_cycle_id당 레코드 1개 고정 (UPDATE 방식)
```

### 발송 흐름 변경

**변경 전**
```
리뷰 저장 → PENDING INSERT

스케줄러 → scheduled_at = now() 정확 일치 조회
  → 발송 → SENT/FAILED INSERT (append-only)

재시도 → SENT=0 AND FAILED>=1 AND FAILED<3 (GROUP BY/HAVING)
  → N+1 쿼리 발생
```

**변경 후**
```
리뷰 저장 → PENDING INSERT (fail_count=0)  ← 동일

스케줄러 → status=PENDING AND scheduled_at <= now() 범위 조회
  → 발송 성공: UPDATE SET status=SENT, last_attempted_at=now()
  → 발송 실패: UPDATE SET status=FAILED, fail_count=fail_count+1, last_attempted_at=now()

재시도 → status=FAILED AND fail_count < 3 AND scheduled_at <= now() - 1일
  → JOIN FETCH로 N+1 없이 단일 쿼리 조회
```

### cutoffDateTime 설계 근거

재시도 필터 조건 `scheduled_at <= now() - 1일`은 단기/장기 주기를 구분하기 위한 프록시.

`review_cycle` 테이블에 주기 길이(duration) 컬럼이 없으므로 `scheduled_at`의 경과 시간으로 역산.

| scheduled_at | 경과 시간 | 재시도 여부 | 이유 |
|---|---|---|---|
| now - 1시간 | 1시간 | ❌ 제외 | 단기 주기로 간주. 다음 발송 시점이 곧 도래 |
| now - 25시간 | 25시간 | ✅ 포함 | 장기 주기(1일+)로 간주. 다음 발송까지 오래 걸림 |
| now - 72시간 | 72시간 | ✅ 포함 | 장기 주기(7일, 30일 등) |

기준을 1일로 삼은 이유: 장기 주기의 최솟값이 `ReviewCycleDuration.DAY`(1일)이기 때문.

**한계**: 단기 주기가 서버 장애 등으로 2일 이상 묵은 경우 재시도 대상에 포함될 수 있음. at-least-once 수용 범위로 허용.

### DB 마이그레이션 (Flyway)

| 파일 | 내용 |
|---|---|
| `V20260304_1__add_fail_count_and_last_attempted_at_to_notification_history.sql` | DDL — `fail_count`, `last_attempted_at` 컬럼 추가 |
| `V20260304_2__migrate_notification_history_data.sql` | DML — 기존 append-only 레코드를 review_cycle_id당 1개로 통합 |

DML 변환 규칙:

| 기존 NH 레코드 구성 | 변환 후 status | fail_count |
|---|---|---|
| PENDING만 존재 | PENDING | 0 |
| PENDING + FAILED×n (SENT 없음) | FAILED | n |
| PENDING + FAILED×n + SENT | SENT | n |

### 코드 변경 요약

| 파일 | 변경 내용 |
|---|---|
| `ReviewCycleRepository` | `findAllByScheduledAt`: 정확 일치 → `<= :scheduledAt AND status=PENDING` 범위 조회 |
| `ReviewCycleRepository` | `findAllRetryableCycles`: GROUP BY/HAVING 제거 → `status=FAILED AND failCount<3 AND scheduledAt<=cutoff` + JOIN FETCH |
| `NotificationHistoryRepository` | `updateStatus`, `updateStatusWithIncrementFailCount` `@Modifying @Query` 추가 |
| `NotificationHistoryService` | `saveAll` (INSERT) → `updateStatus` (UPDATE) 방식으로 전환 |
| `SingleReviewEmailSender` | `saveAll` → `updateStatus` 호출로 변경 |
| `EmailRetryService` | `Clock` 주입, `cutoffDateTime = now - 1일` 계산 후 전달 |
