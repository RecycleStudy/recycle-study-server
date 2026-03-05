# Recycle Study Server - 시나리오별 시퀀스 다이어그램

## 공통 구조

- **인증 방식**: `X-device-Id` 헤더로 디바이스 식별자 전달 → `DeviceAuthArgumentResolver`가 DB 조회 후 활성화 여부 검증
- **이메일 발송**: `@Async` 비동기 처리
- **입출력 레이어**: Controller ↔ Service 간 Input/Output DTO 분리

---

## 1. 회원/디바이스 등록

`POST /api/v1/members`

```mermaid
sequenceDiagram
    actor Client
    participant MemberController
    participant MemberService
    participant MemberRepository
    participant DeviceRepository
    participant DeviceAuthEmailSender
    participant EmailSender
    participant SMTP

    Client->>MemberController: POST /api/v1/members {email}
    MemberController->>MemberService: saveDevice(MemberSaveInput)

    MemberService->>MemberRepository: findByEmail(email)
    alt 신규 멤버
        MemberRepository-->>MemberService: empty
        MemberService->>MemberRepository: save(Member)
        MemberRepository-->>MemberService: savedMember
    else 기존 멤버
        MemberRepository-->>MemberService: existingMember
    end

    MemberService->>MemberService: DeviceIdentifier.create() (UUID 생성)
    MemberService->>MemberService: ActivationExpiredDateTime.create() (만료시간 설정)
    MemberService->>DeviceRepository: save(Device{isActive=false})
    DeviceRepository-->>MemberService: savedDevice

    MemberService-->>MemberController: MemberSaveOutput{email, identifier}
    MemberController->>DeviceAuthEmailSender: sendDeviceAuthMail(email, identifier) [비동기]
    MemberController-->>Client: 201 Created {identifier}

    Note over DeviceAuthEmailSender,SMTP: 비동기 처리 (@Async)
    DeviceAuthEmailSender->>DeviceAuthEmailSender: authUrl 생성 (Thymeleaf 템플릿 렌더링)
    DeviceAuthEmailSender->>EmailSender: send(email, subject, htmlContent)
    EmailSender->>SMTP: MimeMessage 전송
    SMTP-->>EmailSender: 전송 완료
```

---

## 2. 디바이스 인증 (이메일 링크 클릭)

`GET /api/v1/device/auth?email={email}&identifier={identifier}`

```mermaid
sequenceDiagram
    actor User
    participant DeviceController
    participant MemberService
    participant MemberRepository
    participant DeviceRepository
    participant Device

    User->>DeviceController: GET /api/v1/device/auth?email=&identifier=
    DeviceController->>MemberService: authenticateDevice(email, deviceIdentifier)

    MemberService->>MemberRepository: existsByEmail(email)
    alt 존재하지 않는 이메일
        MemberRepository-->>MemberService: false
        MemberService-->>DeviceController: NotFoundException
        DeviceController-->>User: 404 Not Found
    end

    MemberService->>DeviceRepository: findByIdentifier(deviceIdentifier)
    alt 존재하지 않는 디바이스
        DeviceRepository-->>MemberService: empty
        MemberService-->>DeviceController: NotFoundException
    end

    DeviceRepository-->>MemberService: device

    alt 이미 인증된 디바이스
        MemberService-->>DeviceController: BadRequestException
        DeviceController-->>User: 400 Bad Request
    end

    MemberService->>Device: verifyOwner(email) - 소유자 검증
    MemberService->>Device: activate(currentTime) - 만료 여부 확인 후 isActive=true

    MemberService-->>DeviceController: 완료
    DeviceController-->>User: View "auth_success" (HTML 페이지)
```

---

## 3. 디바이스 목록 조회

`GET /api/v1/members`

```mermaid
sequenceDiagram
    actor Client
    participant DeviceAuthArgumentResolver
    participant DeviceRepository
    participant MemberController
    participant MemberService
    participant MemberRepository

    Client->>DeviceAuthArgumentResolver: X-device-Id 헤더 검증
    DeviceAuthArgumentResolver->>DeviceRepository: findByIdentifier(identifier)
    DeviceRepository-->>DeviceAuthArgumentResolver: device
    DeviceAuthArgumentResolver->>DeviceAuthArgumentResolver: isActive 확인
    DeviceAuthArgumentResolver-->>MemberController: DeviceIdentifier

    MemberController->>MemberService: findAllMemberDevices(MemberFindInput)
    MemberService->>MemberRepository: findByIdentifier(identifier)
    MemberRepository-->>MemberService: member

    MemberService->>DeviceRepository: findAllByMemberEmail(email)
    DeviceRepository-->>MemberService: List<Device>

    MemberService-->>MemberController: MemberFindOutput{email, devices}
    MemberController-->>Client: 200 OK {email, devices[]}
```

---

## 4. 알림 시간 조회

`GET /api/v1/members/notification-time`

```mermaid
sequenceDiagram
    actor Client
    participant DeviceAuthArgumentResolver
    participant MemberController
    participant MemberService
    participant MemberRepository

    Client->>DeviceAuthArgumentResolver: X-device-Id 헤더 검증
    DeviceAuthArgumentResolver-->>MemberController: DeviceIdentifier

    MemberController->>MemberService: findNotificationTime(identifier)
    MemberService->>MemberRepository: findByIdentifier(identifier)
    MemberRepository-->>MemberService: member
    MemberService-->>MemberController: MemberNotificationTimeFindOutput{notificationTime}
    MemberController-->>Client: 200 OK {notificationTime}
```

---

## 5. 알림 시간 수정

`PATCH /api/v1/members/notification-time`

```mermaid
sequenceDiagram
    actor Client
    participant DeviceAuthArgumentResolver
    participant MemberController
    participant MemberService
    participant MemberRepository
    participant Member

    Client->>DeviceAuthArgumentResolver: X-device-Id 헤더 검증
    DeviceAuthArgumentResolver-->>MemberController: DeviceIdentifier

    MemberController->>MemberService: updateNotificationTime(MemberNotificationTimeUpdateInput)
    MemberService->>MemberRepository: findByIdentifier(identifier)
    MemberRepository-->>MemberService: member

    MemberService->>Member: updateNotificationTime(newTime)
    Note over Member: Dirty Checking으로 자동 반영

    MemberService-->>MemberController: 완료
    MemberController-->>Client: 200 OK
```

---

## 6. 디바이스 삭제

`DELETE /api/v1/device`

```mermaid
sequenceDiagram
    actor Client
    participant DeviceAuthArgumentResolver
    participant DeviceController
    participant MemberService
    participant MemberRepository
    participant DeviceRepository

    Client->>DeviceAuthArgumentResolver: X-device-Id 헤더 검증
    DeviceAuthArgumentResolver-->>DeviceController: DeviceIdentifier (요청자)

    DeviceController->>MemberService: deleteDevice(DeviceDeleteInput{요청자 identifier, 삭제 대상 identifier})

    MemberService->>MemberRepository: findByIdentifier(요청자 identifier)
    MemberRepository-->>MemberService: requestMember

    MemberService->>DeviceRepository: findByIdentifier(대상 identifier)
    alt 존재하지 않는 디바이스
        DeviceRepository-->>MemberService: empty
        MemberService-->>DeviceController: NotFoundException
        DeviceController-->>Client: 404 Not Found
    end
    DeviceRepository-->>MemberService: targetDevice

    MemberService->>MemberService: targetDevice.member 이메일 == requestMember 이메일 검증
    alt 소유자 불일치
        MemberService-->>DeviceController: NotFoundException
        DeviceController-->>Client: 404 Not Found
    end

    MemberService->>DeviceRepository: delete(targetDevice)
    MemberService-->>DeviceController: 완료
    DeviceController-->>Client: 204 No Content
```

---

## 7. 커스텀 복습 주기 목록 조회

`GET /api/v1/cycles/custom`

```mermaid
sequenceDiagram
    actor Client
    participant DeviceAuthArgumentResolver
    participant CycleOptionController
    participant CycleOptionService
    participant MemberRepository
    participant CycleOptionRepository
    participant DefaultCycleOption

    Client->>DeviceAuthArgumentResolver: X-device-Id 헤더 검증
    DeviceAuthArgumentResolver-->>CycleOptionController: DeviceIdentifier

    CycleOptionController->>CycleOptionService: findAllCycleOptions(identifier)
    CycleOptionService->>MemberRepository: findByIdentifier(identifier)
    MemberRepository-->>CycleOptionService: member

    CycleOptionService->>DefaultCycleOption: getAll() - 에빙하우스 등 기본 옵션
    DefaultCycleOption-->>CycleOptionService: List<DefaultCycleOption>

    CycleOptionService->>CycleOptionRepository: findAllByMember(member)
    CycleOptionRepository-->>CycleOptionService: List<CycleOption>

    CycleOptionService-->>CycleOptionController: CycleOptionFindOutput{defaultOptions, customOptions}
    CycleOptionController-->>Client: 200 OK {defaultOptions[], customOptions[]}
```

---

## 8. 커스텀 복습 주기 생성

`POST /api/v1/cycles/custom`

```mermaid
sequenceDiagram
    actor Client
    participant DeviceAuthArgumentResolver
    participant CycleOptionController
    participant CycleOptionService
    participant MemberRepository
    participant CycleOptionRepository

    Client->>DeviceAuthArgumentResolver: X-device-Id 헤더 검증
    DeviceAuthArgumentResolver-->>CycleOptionController: DeviceIdentifier

    CycleOptionController->>CycleOptionService: saveCycleOption(identifier, CycleOptionSaveInput{title, durations})
    CycleOptionService->>MemberRepository: findByIdentifier(identifier)
    MemberRepository-->>CycleOptionService: member

    CycleOptionService->>CycleOptionRepository: countByMember(member)
    alt 커스텀 주기 5개 초과
        CycleOptionRepository-->>CycleOptionService: count >= 5
        CycleOptionService-->>CycleOptionController: BadRequestException
        CycleOptionController-->>Client: 400 Bad Request
    end

    CycleOptionService->>CycleOptionRepository: save(CycleOption{member, title, durations})
    CycleOptionRepository-->>CycleOptionService: savedCycleOption

    CycleOptionService-->>CycleOptionController: CycleOptionSaveOutput{id, title, durations}
    CycleOptionController-->>Client: 201 Created {id, title, durations}
```

---

## 9. 커스텀 복습 주기 수정

`PUT /api/v1/cycles/custom/{id}`

```mermaid
sequenceDiagram
    actor Client
    participant DeviceAuthArgumentResolver
    participant CycleOptionController
    participant CycleOptionService
    participant MemberRepository
    participant CycleOptionRepository
    participant CycleOption

    Client->>DeviceAuthArgumentResolver: X-device-Id 헤더 검증
    DeviceAuthArgumentResolver-->>CycleOptionController: DeviceIdentifier

    CycleOptionController->>CycleOptionService: updateCycleOption(identifier, cycleOptionId, CycleOptionUpdateInput)
    CycleOptionService->>MemberRepository: findByIdentifier(identifier)
    MemberRepository-->>CycleOptionService: member

    CycleOptionService->>CycleOptionRepository: findByIdWithDurations(cycleOptionId)
    alt 존재하지 않는 주기
        CycleOptionRepository-->>CycleOptionService: empty
        CycleOptionService-->>CycleOptionController: NotFoundException
        CycleOptionController-->>Client: 404 Not Found
    end
    CycleOptionRepository-->>CycleOptionService: cycleOption

    CycleOptionService->>CycleOption: isOwner(member) 검증
    alt 소유자 불일치
        CycleOptionService-->>CycleOptionController: NotFoundException
        CycleOptionController-->>Client: 404 Not Found
    end

    CycleOptionService->>CycleOption: update(newTitle, newDurations)
    Note over CycleOption: CycleDurations.replace()로 기존 durations 교체

    CycleOptionService-->>CycleOptionController: CycleOptionSaveOutput
    CycleOptionController-->>Client: 200 OK {id, title, durations}
```

---

## 10. 커스텀 복습 주기 삭제

`DELETE /api/v1/cycles/custom/{id}`

```mermaid
sequenceDiagram
    actor Client
    participant DeviceAuthArgumentResolver
    participant CycleOptionController
    participant CycleOptionService
    participant MemberRepository
    participant CycleOptionRepository

    Client->>DeviceAuthArgumentResolver: X-device-Id 헤더 검증
    DeviceAuthArgumentResolver-->>CycleOptionController: DeviceIdentifier

    CycleOptionController->>CycleOptionService: deleteCycleOption(identifier, cycleOptionId)
    CycleOptionService->>MemberRepository: findByIdentifier(identifier)
    MemberRepository-->>CycleOptionService: member

    CycleOptionService->>CycleOptionRepository: findById(cycleOptionId)
    alt 존재하지 않는 주기
        CycleOptionRepository-->>CycleOptionService: empty
        CycleOptionService-->>CycleOptionController: NotFoundException
        CycleOptionController-->>Client: 404 Not Found
    end
    CycleOptionRepository-->>CycleOptionService: cycleOption

    CycleOptionService->>CycleOptionService: isOwner(member) 검증
    alt 소유자 불일치
        CycleOptionService-->>CycleOptionController: NotFoundException
    end

    CycleOptionService->>CycleOptionRepository: delete(cycleOption)
    CycleOptionService-->>CycleOptionController: 완료
    CycleOptionController-->>Client: 204 No Content
```

---

## 11. 복습 저장

`POST /api/v1/reviews`

```mermaid
sequenceDiagram
    actor Client
    participant DeviceAuthArgumentResolver
    participant ReviewController
    participant ReviewService
    participant MemberRepository
    participant CycleOptionRepository
    participant CycleSelectionResolverRegistry
    participant ReviewRepository
    participant ReviewCycleRepository
    participant NotificationHistoryRepository

    Client->>DeviceAuthArgumentResolver: X-device-Id 헤더 검증
    DeviceAuthArgumentResolver-->>ReviewController: DeviceIdentifier

    ReviewController->>ReviewService: saveReview(ReviewSaveInput{identifier, url, cycle})
    ReviewService->>MemberRepository: findByIdentifier(identifier)
    MemberRepository-->>ReviewService: member

    ReviewService->>ReviewRepository: save(Review{member, url})
    ReviewRepository-->>ReviewService: savedReview

    alt cycle = CustomCycleSelection
        ReviewService->>CycleOptionRepository: findById(cycleOptionId)
        CycleOptionRepository-->>ReviewService: cycleOption
        ReviewService->>ReviewService: isOwner(member) 소유권 검증
    end

    ReviewService->>CycleSelectionResolverRegistry: resolve(cycleSelection)
    Note over CycleSelectionResolverRegistry: DefaultCycleSelectionResolver 또는<br/>CustomCycleSelectionResolver 위임
    CycleSelectionResolverRegistry-->>ReviewService: List<Duration>

    ReviewService->>ReviewService: 각 Duration → scheduledAt 계산
    Note over ReviewService: duration < 1일: 현재시간 + duration<br/>duration >= 1일 & notificationTime 설정: notificationTime으로 시간 조정

    ReviewService->>ReviewCycleRepository: saveAll(List<ReviewCycle>)
    ReviewCycleRepository-->>ReviewService: savedReviewCycles

    ReviewService->>NotificationHistoryRepository: saveAll(List<NotificationHistory{status=PENDING}>)
    NotificationHistoryRepository-->>ReviewService: 완료

    ReviewService-->>ReviewController: ReviewSaveOutput{url, scheduledAts}
    ReviewController-->>Client: 201 Created {url, scheduledAts[]}
```

---

## 12. 복습 메일 발송 스케줄러

매 분 cron 실행 (`${schedule.review-mail.cron}`, Asia/Seoul 기준)

```mermaid
sequenceDiagram
    participant Scheduler
    participant ReviewEmailSender
    participant ReviewCycleService
    participant ReviewCycleRepository
    participant SingleReviewEmailSender
    participant NotificationHistoryService
    participant NotificationHistoryRepository
    participant EmailSender
    participant SMTP

    Scheduler->>ReviewEmailSender: sendReviewMail() [cron 트리거]
    ReviewEmailSender->>ReviewEmailSender: targetDateTime = now().truncatedTo(MINUTES)

    ReviewEmailSender->>ReviewCycleService: findTargetReviewCycle(ReviewSendInput)
    ReviewCycleService->>ReviewCycleRepository: findAllByScheduledAt(targetDateTime)
    Note over ReviewCycleRepository: scheduledAt = targetDateTime인<br/>ReviewCycle 조회 (멤버별 그룹화)
    ReviewCycleRepository-->>ReviewCycleService: List<ReviewCycle>
    ReviewCycleService-->>ReviewEmailSender: ReviewSendOutput{elements[]}

    loop 각 ReviewSendElement (멤버별)
        ReviewEmailSender->>SingleReviewEmailSender: sendOne(element) [비동기 @Async]
    end

    Note over SingleReviewEmailSender,SMTP: 비동기 처리 (@Async)
    SingleReviewEmailSender->>SingleReviewEmailSender: Thymeleaf 템플릿으로 HTML 생성

    SingleReviewEmailSender->>EmailSender: send(email, subject, htmlContent)
    EmailSender->>SMTP: MimeMessage 전송

    alt 발송 성공
        SMTP-->>EmailSender: 전송 완료
        EmailSender-->>SingleReviewEmailSender: 완료
        SingleReviewEmailSender->>NotificationHistoryService: saveAll(cycleIds, SENT)
        NotificationHistoryService->>NotificationHistoryRepository: saveAll(List<NotificationHistory{SENT}>)
    else 발송 실패
        SMTP-->>EmailSender: 예외 발생
        EmailSender-->>SingleReviewEmailSender: EmailSendException
        SingleReviewEmailSender->>NotificationHistoryService: saveAll(cycleIds, FAILED)
        NotificationHistoryService->>NotificationHistoryRepository: saveAll(List<NotificationHistory{FAILED}>)
    end
```

---

## 13. 실패 메일 재시도 스케줄러

60초마다 실행 (`fixedDelay = 60_000`)

```mermaid
sequenceDiagram
    participant Scheduler
    participant EmailRetryScheduler
    participant EmailRetryService
    participant ReviewCycleRepository
    participant SingleReviewEmailSender
    participant NotificationHistoryService
    participant EmailSender
    participant SMTP

    Scheduler->>EmailRetryScheduler: runRetry() [60초마다]
    EmailRetryScheduler->>EmailRetryService: retryFailedEmails()

    EmailRetryService->>ReviewCycleRepository: findAllRetryableCycles(MAX_RETRY_COUNT=3)
    Note over ReviewCycleRepository: 최신 상태가 FAILED이고<br/>재시도 횟수 < 3인 ReviewCycle 조회
    ReviewCycleRepository-->>EmailRetryService: List<ReviewCycle>

    alt 재시도 대상 없음
        EmailRetryService-->>EmailRetryScheduler: 종료
    end

    EmailRetryService->>EmailRetryService: 멤버별 GroupingBy

    loop 멤버별
        EmailRetryService->>SingleReviewEmailSender: sendOne(ReviewSendElement{email, cycleIds, urls})
        Note over SingleReviewEmailSender,SMTP: 비동기 처리 (@Async)

        SingleReviewEmailSender->>EmailSender: send(email, subject, htmlContent)
        EmailSender->>SMTP: MimeMessage 전송

        alt 발송 성공
            SingleReviewEmailSender->>NotificationHistoryService: saveAll(cycleIds, SENT)
        else 발송 실패
            SingleReviewEmailSender->>NotificationHistoryService: saveAll(cycleIds, FAILED)
        end
    end
```

---

## 공통: 디바이스 인증 필터 (`@AuthDevice`)

모든 인증이 필요한 API에서 공통으로 동작하는 `DeviceAuthArgumentResolver` 흐름:

```mermaid
sequenceDiagram
    actor Client
    participant Spring MVC
    participant DeviceAuthArgumentResolver
    participant DeviceRepository
    participant Controller

    Client->>Spring MVC: HTTP 요청 + X-device-Id 헤더

    Spring MVC->>DeviceAuthArgumentResolver: resolveArgument() [@AuthDevice 파라미터 감지]

    alt X-device-Id 헤더 없음
        DeviceAuthArgumentResolver-->>Spring MVC: UnauthorizedException
        Spring MVC-->>Client: 401 Unauthorized
    end

    DeviceAuthArgumentResolver->>DeviceRepository: findByIdentifier(identifier)

    alt 존재하지 않는 디바이스
        DeviceRepository-->>DeviceAuthArgumentResolver: empty
        DeviceAuthArgumentResolver-->>Spring MVC: UnauthorizedException
        Spring MVC-->>Client: 401 Unauthorized
    end

    DeviceRepository-->>DeviceAuthArgumentResolver: device

    alt isActive = false
        DeviceAuthArgumentResolver-->>Spring MVC: UnauthorizedException("인증되지 않은 디바이스입니다")
        Spring MVC-->>Client: 401 Unauthorized
    end

    DeviceAuthArgumentResolver-->>Controller: DeviceIdentifier
    Controller->>Controller: 비즈니스 로직 처리
```

---

## 엔티티 관계 요약

```
Member (1) ──── (N) Device
Member (1) ──── (N) Review
Member (1) ──── (N) CycleOption

Review (1) ──── (N) ReviewCycle
ReviewCycle (1) ──── (N) NotificationHistory

CycleOption (1) ──── (N) CycleOptionDuration
```

## 알림 상태 흐름 (NotificationStatus)

```
PENDING → SENT    (발송 성공)
PENDING → FAILED  (발송 실패)
FAILED  → SENT    (재시도 성공)
FAILED  → FAILED  (재시도 실패, MAX 3회)
```