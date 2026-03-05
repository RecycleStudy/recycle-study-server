# 스케줄러 다중 인스턴스 동시성 문제 분석

## 서론

`ReviewEmailSender`와 `EmailRetryScheduler`는 `@Scheduled`로 주기적으로 실행된다.
현재 구조는 단일 인스턴스를 가정하고 있어, 다중 인스턴스 배포 시 동일한 이메일이 중복 발송될 수 있다.

### 문제 시나리오

```
인스턴스 A: findAllByScheduledAt() → cycleA (PENDING) 조회
인스턴스 B: findAllByScheduledAt() → cycleA (PENDING) 조회  ← 동시
→ 둘 다 이메일 발송 → 중복 이메일 수신
```

SELECT와 `UPDATE status = SENT` 사이의 타임 윈도우에서 경쟁 조건이 발생한다.
현재 단일 인스턴스로 운영 중이라면 이 문제는 발생하지 않는다.

---

## 1. 현재 아키텍처

```
ReviewEmailSender (@Scheduled, cron)
  └─ reviewCycleService.findTargetReviewCycle()    @Transactional(readOnly)
       └─ findAllByScheduledAt(scheduledAt, PENDING)
  └─ singleReviewEmailSender.sendOne()             @Async (VT)
       └─ updateStatus(ids, SENT / FAILED)         @Transactional

EmailRetryScheduler (@Scheduled, 60초)
  └─ emailRetryService.retryFailedEmails()
```

관련 파일:
- `src/main/java/com/recyclestudy/email/ReviewEmailSender.java`
- `src/main/java/com/recyclestudy/email/EmailRetryScheduler.java`
- `src/main/java/com/recyclestudy/review/repository/ReviewCycleRepository.java` — `findAllByScheduledAt` 쿼리

---

## 2. 해결 방법 비교

### 2-1. ShedLock (DB 기반 분산 락)

**메커니즘**

```
인스턴스 A: shedlock 테이블에 락 레코드 INSERT → 성공 → 스케줄러 실행
인스턴스 B: shedlock 테이블에 락 레코드 INSERT → 실패 → 해당 틱 스킵
```

**변경 범위**

- `build.gradle`: `net.javacrumbs.shedlock:shedlock-spring`, `shedlock-provider-jdbc-template` 의존성 추가
- `ReviewEmailSender`: `@SchedulerLock(name = "reviewEmailSender", lockAtLeastFor = "PT50S", lockAtMostFor = "PT55S")` 추가
- `EmailRetryScheduler`: 동일하게 `@SchedulerLock` 추가
- DB 마이그레이션: `shedlock` 테이블 DDL 추가

**트레이드오프**

| 항목 | 내용 |
|------|------|
| 인프라 추가 | 없음 (MySQL 사용) |
| 구현 복잡도 | 낮음 |
| 보장 수준 | 한 인스턴스만 실행 |
| 한계 | 락 획득 실패 인스턴스는 해당 틱 완전 스킵 → 최대 1분 발송 지연 가능 |
| 발송 자체의 멱등성 | 없음 (락으로만 보호) |

---

### 2-2. DB 상태 기반 멱등성 (PROCESSING 상태 전이)

**메커니즘**

```sql
-- 조회 전 원자적 상태 전이
UPDATE notification_history SET status = 'PROCESSING'
WHERE status = 'PENDING' AND scheduled_at <= :now
-- UPDATE 성공 건수만큼만 발송 진행
```

UPDATE는 MySQL 기준 행 단위 잠금이므로 두 인스턴스가 동시에 실행해도 각기 다른 행만 처리한다.

**변경 범위**

- `NotificationStatus` enum: `PROCESSING` 추가
- `NotificationHistoryRepository`: 상태 전이 쿼리 추가
- `ReviewEmailSender`: 조회 방식 변경 (PENDING → PROCESSING 전환 후 처리)
- DB 마이그레이션: 없음 (상태 값만 추가)
- 추가 필요: stuck PROCESSING 레코드 처리 스케줄러 (타임아웃 후 FAILED 전환)

**트레이드오프**

| 항목 | 내용 |
|------|------|
| 인프라 추가 | 없음 |
| 구현 복잡도 | 높음 (PROCESSING stuck 처리 로직 포함) |
| 보장 수준 | 완전한 멱등성 (DB 원자적 UPDATE) |
| 한계 | stuck 레코드 처리 로직이 없으면 발송 영구 중단 가능 |
| 발송 자체의 멱등성 | 있음 |

---

### 2-3. Redis 분산 락 (Redisson / SETNX)

**메커니즘**

Redis `SET NX EX`로 TTL이 있는 락을 구현한다. 락을 획득한 인스턴스만 스케줄러를 실행하고, TTL 초과 시 락이 자동 해제된다.

**트레이드오프**

| 항목 | 내용 |
|------|------|
| 인프라 추가 | Redis 서버 필요 |
| 구현 복잡도 | 중간 |
| 보장 수준 | 한 인스턴스만 실행, TTL로 데드락 방지 |
| 한계 | Redis 장애 시 락 기능 중단 |
| 발송 자체의 멱등성 | 없음 (락으로만 보호) |

---

### 2-4. 메시지 큐 (SQS / RabbitMQ)

**메커니즘**

스케줄러가 발송 작업을 큐에 발행하고, 여러 인스턴스가 경쟁 소비한다. 큐의 visibility timeout으로 중복 처리를 방지한다.

**트레이드오프**

| 항목 | 내용 |
|------|------|
| 인프라 추가 | 큐 서버 필요 |
| 구현 복잡도 | 높음 |
| 보장 수준 | 자연스러운 at-least-once |
| 한계 | 아키텍처 전면 변경 필요 |
| 현 규모 적합성 | DAU 500 기준 오버 엔지니어링 |

---

### 2-5. FOR UPDATE SKIP LOCKED (DB 비관적 잠금)

**메커니즘**

MySQL 8.0+에서 지원. 이미 다른 트랜잭션이 잠금 중인 행을 **대기 없이 결과에서 제외(skip)**한다.

```sql
SELECT id, review_cycle_id
FROM notification_history
WHERE status = 'PENDING' AND scheduled_at <= :now
FOR UPDATE SKIP LOCKED
```

인스턴스 A가 행 X를 잠금 중이면 인스턴스 B의 같은 쿼리 결과에서 행 X가 빠진다.
→ 여러 인스턴스가 서로 다른 행을 병렬로 처리 가능 (분산 처리 친화적)

**현재 아키텍처와의 호환성 문제**

현재 흐름:
```
T1: findTargetReviewCycle()  @Transactional(readOnly) → 반환 시 트랜잭션/잠금 해제
T2: sendOne()                @Async (VT), 별도 트랜잭션
```

`FOR UPDATE SKIP LOCKED`로 획득한 잠금은 **T1 커밋 시점에 해제**된다.
T2는 별도 비동기 스레드에서 실행되므로, T1이 끝난 뒤 잠금이 이미 풀려 있다.
→ 다른 인스턴스가 T1 종료 직후 같은 행을 조회 가능 → **경쟁 조건 미해소**

**올바른 적용 패턴**

SKIP LOCKED를 실효성 있게 사용하려면 **같은 트랜잭션 내에서 SELECT + 상태 전이**를 묶어야 한다:

```
BEGIN
  SELECT ... FOR UPDATE SKIP LOCKED  -- 행 잠금 획득 + 다른 인스턴스 skip
  UPDATE status = 'PROCESSING' WHERE id IN (:ids)
COMMIT                                -- 잠금 해제, 이미 PROCESSING이므로 다른 인스턴스가 재조회해도 안전
이후 비동기 발송
```

이 패턴은 사실상 **2-2(PROCESSING 상태 전이)와 동일한 결과**를 낸다.
차이점: SKIP LOCKED를 쓰면 SELECT와 UPDATE 사이의 짧은 타임 윈도우도 DB 잠금으로 명시적 보호.
SKIP LOCKED 없이 `UPDATE WHERE PENDING`만 해도 MySQL 행 잠금으로 동일 효과를 낼 수 있다.

SKIP LOCKED의 진짜 강점은 job queue 패턴에서 나타난다:
```sql
-- 인스턴스마다 N개씩 분할 처리
SELECT * FROM notification_history
WHERE status = 'PENDING' ORDER BY scheduled_at LIMIT 10
FOR UPDATE SKIP LOCKED
```
여러 워커가 동시에 실행해도 자동으로 서로 다른 행을 가져감 → 확장성 우수.
단, 현재 구조(전체 배치 조회 → 모두 비동기 발송)에서는 이 장점을 활용하기 어렵다.

**변경 범위**

- `NotificationStatus` enum: `PROCESSING` 추가 (2-2와 동일)
- `NotificationHistoryRepository`: native query로 SKIP LOCKED 쿼리 작성
  - JPQL은 `FOR UPDATE SKIP LOCKED` 미지원 → `@Query(value = "...", nativeQuery = true)` 필수
- `ReviewEmailSender`: SELECT + UPDATE PROCESSING을 같은 트랜잭션으로 묶도록 재설계
- MySQL 8.0+ 버전 확인 필요

**트레이드오프**

| 항목 | 내용 |
|------|------|
| 인프라 추가 | 없음 (MySQL 8.0+ 필요) |
| 구현 복잡도 | 중간~높음 (native query + 트랜잭션 재설계) |
| 보장 수준 | SELECT-UPDATE 타임 윈도우를 DB 잠금으로 명시적 보호 |
| 한계 | JPA JPQL 미지원, 트랜잭션 범위 재설계 필요 |
| stuck 처리 필요 | 있음 (2-2와 동일) |
| 발송 자체의 멱등성 | 있음 (PROCESSING 전이와 결합 시) |

**2-2와의 관계**

| 항목 | 2-2 (PROCESSING 전이만) | 2-5 (SKIP LOCKED + PROCESSING) |
|------|----------------------|-------------------------------|
| SELECT-UPDATE 타임 윈도우 보호 | UPDATE 원자성으로 충분 | DB 잠금으로 명시적 보호 |
| 병렬 처리 (인스턴스 분산) | 가능 | 가능 (더 명시적) |
| JPA 지원 | JPQL 가능 | Native query 필요 |
| 복잡도 | 중간 | 중간~높음 |

실질적으로 SKIP LOCKED는 2-2의 보완 기법이며 독립된 대안이 아니다.
MySQL의 행 잠금으로 `UPDATE WHERE PENDING`만으로도 동일한 원자성을 보장할 수 있기 때문에,
SKIP LOCKED를 추가해도 실질적인 안전성 향상은 크지 않다.

---

## 3. 방법별 비교 요약

| 항목 | ShedLock | 상태 기반 | Redis 락 | 메시지 큐 | SKIP LOCKED |
|------|---------|---------|---------|---------|------------|
| 인프라 추가 | 없음 | 없음 | Redis 필요 | 큐 서버 필요 | 없음 (MySQL 8.0+) |
| 구현 복잡도 | 낮음 | 높음 | 중간 | 높음 | 중간~높음 |
| 발송 멱등성 | 없음 (락으로만 보호) | 있음 | 없음 (락으로만 보호) | 있음 | 있음 (PROCESSING 결합 시) |
| 발송 지연 위험 | 최대 1분 | 없음 | 최대 1분 | 없음 | 없음 |
| stuck 처리 필요 | 없음 | 있음 | 없음 | 없음 (DLQ) | 있음 |
| 현 규모 적합성 | 적합 | 적합 | 과함 | 과함 | 적합 (2-2 대비 이점 미미) |

---

## 4. 선택 기준 정리

**ShedLock을 선택하는 경우**
- 1분 이내 발송을 엄격하게 보장하지 않아도 되는 경우
- 구현 단순성을 우선할 때
- 락 획득 실패로 인한 틱 스킵이 허용 가능한 경우

**상태 기반 멱등성을 선택하는 경우**
- 발송 멱등성이 비즈니스 요구사항일 때
- stuck 레코드 처리 로직을 포함한 완전한 구현을 원할 때
- 인스턴스 간 작업을 분산 처리하고 싶을 때 (한 틱에 모든 인스턴스가 기여 가능)

**SKIP LOCKED를 선택하는 경우**
- 2-2(PROCESSING 전이)를 적용하면서 SELECT-UPDATE 타임 윈도우를 DB 잠금으로 명시적으로 보호하고 싶을 때
- 향후 LIMIT N으로 워커별 처리량을 나누는 job queue 패턴으로 확장할 계획이 있을 때
- JPQL 대신 native query를 허용하는 프로젝트 정책일 때
- 단, 현재 구조에서는 2-2만으로도 동일한 안전성을 확보할 수 있으므로 단독 선택 이유는 약하다
