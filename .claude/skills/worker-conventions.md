---
name: worker-conventions
description: Worker 모듈 아키텍처 및 개발 규칙입니다. Listener, Handler, Publisher, MQ Message DTO 작성 시 이 규칙을 따릅니다.
allowed-tools: Read, Write, Edit, Glob
---

# Worker Module Conventions

`spring-api-rules.md`의 공통 규칙을 기반으로 하며, Worker 모듈에서 추가로 적용하는 규칙을 정의합니다.

- Root Package: `com.mailsangja.worker`
- 진입점: HTTP (Google Pub/Sub), RabbitMQ Listener, Cron Scheduler
- Exchange 타입: `DirectExchange` (main + DLX 분리)
- Queue 이름: `mailsangja.{taskName}` / Routing Key: `mail.{taskName}`

---

## 핵심 구조 원칙: RabbitMQ 앞단 / 뒷단 분리

```
[앞단 — Producer Side]
  HTTP 진입점 or Cron Scheduler
    → Facade → Classifier → Publisher (MQ 발행)

[RabbitMQ 경계]

[뒷단 — Consumer Side]
  Listener (비즈니스 Facade 역할)
    → Handler → *ApiService / *CommandService / *QueryService
```

**앞단:** 동기 처리만 (OIDC 검증, 토큰 갱신, 외부 API 조회, 이벤트 분류, MQ 발행). DB Write 없음. 발행 즉시 응답.

**뒷단:** Listener가 비즈니스 Facade 역할. 별도 Facade 클래스 없음. Listener가 Handler·ApiService·CommandService 직접 조율.

---

## 패키지 구조

```
com.mailsangja.worker
├── controller/
├── facade/                             # 앞단 HTTP 진입 흐름 조율 전용
├── handler/{domain}/
│   ├── *Classifier.java
│   └── *Handler.java
├── service/{domain}/
│   ├── {Domain}ApiService.java         # 외부 Gmail/Google API 호출 전용
│   ├── {Domain}CommandService.java     # DB Write
│   └── {Domain}QueryService.java       # DB Read
├── messaging/
│   ├── listener/                       # 뒷단 비즈니스 진입점 (Facade 역할)
│   └── publisher/                      # 앞단 MQ 발행 전용
├── scheduler/                          # Cron 트리거
├── config/
│   ├── rabbitmq/
│   │   ├── RabbitMqConfig.java         # Exchange, Converter, RabbitTemplate, 공통 유틸
│   │   └── {TaskName}RabbitConfig.java # 큐 1개당 파일 1개
│   └── properties/
└── dto/{domain}/
    ├── *Message.java / *Request.java / *Response.java / *Result.java / *Command.java
```

---

## 새 메시지 큐 등록

> **상세 절차 및 큐·ContainerFactory·Listener 연결 전체 현황은 `.claude/skills/rabbitmq-queue-registration.md`를 참조한다.**

---

## DTO / Message Record

**모든 DTO는 Java `record`.** compact constructor 검증 및 decode() 패턴은 `spring-api-rules.md`의 Record Self-Validation 섹션을 따른다.

### MQ Message 규칙

- 비즈니스 제약(지원 provider 목록 등)까지 compact constructor 안에 포함한다. Publisher에 검증을 흩뿌리지 않는다.
- Entity → Message 변환이 한 곳 이상에서 발생하면 `from()` 정적 팩토리를 Record 내부에 선언한다.
- HTTP 진입 DTO의 Base64 파싱·검증·변환은 `decode()` 메서드에 위임한다.

### DTO 접미사 구분

| 접미사 | 역할 |
|--------|------|
| `*Message` | RabbitMQ 메시지 payload |
| `*Request` | 외부 Gmail API 요청 파라미터 / HTTP 요청 입력 |
| `*Response` | 외부 Gmail API 원본 JSON 응답 (Jackson 역직렬화) |
| `*Result` | 내부 변환 결과 |
| `*Command` | 내부 명령 전달 |

---

## 앞단 규칙 (Producer Side)

### Facade

- 동기 흐름만 조율: OIDC 검증 → decode() → 토큰 갱신 → 외부 API 조회 → 이벤트 분류 → MQ 발행 → historyId 업데이트
- 저수준 파싱·null 체크·필드 검증을 Facade 메서드 안에 직접 작성하지 않는다. DTO의 compact constructor 또는 `decode()`에 위임한다.
- 도메인 로직은 엔티티 메서드에 위임한다.
- MQ 발행 완료 후 즉시 응답. DB Write 결과를 기다리지 않는다.
- `publishAll()`이 `AmqpException`을 전파하면 historyId 업데이트는 호출되지 않는다 — Pub/Sub NACK → 재전달로 이벤트 누락 없이 재처리된다.

### Publisher

- `RabbitTemplate`을 직접 주입하지 않고 반드시 `*Publisher`를 통해서만 발행한다.
- Message 필드 검증은 Record compact constructor 책임 — Publisher에서 중복 검증 금지
- `AmqpException`은 그대로 전파 — catch 후 삼키지 않는다
- `GmailHistoryEventType`에 `getRoutingKey()` 메서드를 두어 라우팅 키를 중앙화한다

---

## 뒷단 규칙 (Consumer Side)

### Handler 분리 기준

| 상황 | 선택 |
|------|------|
| 큐 1개, 이벤트 타입 1가지, 직선 흐름 | **Listener에 직접 작성** |
| 같은 ContainerFactory를 공유하는 여러 이벤트 타입 | **`GmailHistoryEventHandler` 전략 패턴** |
| 타입은 1가지지만 비즈니스 로직이 복잡 | **전용 Handler 분리** (`MessageAddedHistoryEventHandler` 패턴) |

### Listener — 비즈니스 Facade 역할

- Listener는 MQ 메시지 수신 진입점이자 뒷단 비즈니스 흐름의 Facade다. **별도 Facade 클래스 없음.**
- `@RabbitListener`에 선언된 `containerFactory`가 그 메서드에만 적용되는 concurrency · retry · DLQ 정책 전체다.
- `queues`는 반드시 `#{@{beanName}.name}` SpEL 참조 — 문자열 하드코딩 금지
- `containerFactory`에 해당 task의 팩토리 Bean 이름을 명시한다
- 같은 도메인의 관련 큐 여러 개는 하나의 Listener 클래스에 묶을 수 있다

**토큰 갱신 위치:**
- `message-added`: Listener에서 토큰 갱신 후 갱신된 `MailAccount`를 전용 Handler에 직접 전달
- `history-state`: Listener는 이벤트만 전달, 토큰 갱신은 Gmail API 호출이 필요한 각 Handler 내부에서 수행

---

## Google Access Token 갱신 규칙

모든 Google API 호출 전에 반드시 `GoogleAccessTokenEnsureService.ensureValidGoogleAccessToken()`으로 토큰을 선제 갱신한다. `mailAccount.getAccessToken()` 직접 사용 금지. 앞단 Facade, 뒷단 Listener / Handler 모두 동일하게 적용한다.

---

## Handler / Classifier 규칙

### Classifier

- History API 응답을 내부 이벤트 타입으로 변환하는 순수 해석 로직만 담당한다.
- 동일 메시지 ID에 대해 여러 이벤트가 겹칠 경우, `LinkedHashMap<messageId, event>` 구조로 최신 이벤트만 유지한다 (중복 제거).
- 외부 I/O, DB 조작, 상태 변경을 포함하지 않는다.

### Handler

- `supports()` 반환값과 클래스명 접두사를 반드시 일치시킨다 (`MESSAGE_ADDED` → `MessageAdded...`)
- `@Component`로만 등록하면 Listener의 `List<{Domain}EventHandler>`에 자동 주입된다
- Repository Port 또는 JPA Module을 Handler에서 직접 주입하지 않는다
- 외부 API 조회는 트랜잭션과 DB 락 바깥에서 수행한다
- 락을 획득한 뒤에는 대상 상태를 다시 검증한다

**Handler 두 가지 유형:**
- **전략 Handler**: `GmailHistoryEventHandler` 인터페이스 구현. `supports()` + `handle(event)` 시그니처.
- **전용 Handler**: 인터페이스 미구현. `handle(MailAccount, event)` 시그니처. Listener가 직접 지목.

### 새 이벤트 타입 추가

1. 이벤트 enum에 새 항목 추가 + `getRoutingKey()` 반환값 추가
2. `{EventType}{Domain}EventHandler` 구현체 작성 후 `@Component` 등록
3. Google API 응답에서 새 타입이 식별되어야 한다면 `*Classifier`에 분류 로직 추가
4. 새 큐가 필요하면 `.claude/skills/rabbitmq-queue-registration.md` 참조

---

## Gmail 동시성 규칙

- `Message`, `Thread` 상태를 변경하는 모든 비동기 write 경로는 `mailAccountId + gmailThreadId` 단위로 직렬화한다.
- 락 획득에는 `GmailThreadLockRepositoryPort.acquireThreadLock()`을 사용한다.
- 락을 획득한 뒤에만 Thread 집계 상태(`message_count`, `is_read` 등)를 계산하고 반영한다.
- 락 획득 이후 현재 DB 상태를 다시 확인한 뒤 필요한 최소 범위만 반영한다.

---

## Retry / DLQ 규칙

- `defaultRequeueRejected=false` 기본 설정 — poison message 무한 재적재 방지
- 재시도 소진 시 `AmqpRejectAndDontRequeueException`으로 DLQ 라우팅
- DLQ 적재 시 routing key와 핵심 식별자를 로그에 남긴다
- 소비 로직은 중복 수신에도 안전하도록 idempotent하게 작성한다

---

## 레이어별 책임 요약

| 레이어 | 클래스 | 위치 | 책임 |
|--------|--------|------|------|
| Controller | `*Controller` | 앞단 | HTTP 요청 수신, Facade 위임 |
| Facade | `*Facade` | 앞단 | 동기 흐름 조율 (토큰 갱신, 외부 API, 분류, 발행) |
| Classifier | `*Classifier` | 앞단 | 외부 이벤트 → 내부 이벤트 타입 변환, 중복 제거 |
| Publisher | `*Publisher` | 앞단 | `RabbitTemplate` 래핑, MQ 발행 전용 |
| Scheduler | `*Scheduler` | 앞단 | Cron 트리거, Facade 또는 Publisher 위임 |
| **Listener** | `*Listener` | **뒷단** | **비즈니스 Facade 역할, ContainerFactory 경계, 흐름 조율** |
| Handler (전략) | `*Handler` implements `*EventHandler` | 뒷단 | 전략 풀 멤버, `supports()` 기준 이벤트 타입별 처리 |
| Handler (전용) | `*Handler` (인터페이스 미구현) | 뒷단 | Listener 직접 지목, `(MailAccount, Event)` 시그니처 |
| ApiService | `*ApiService` | 뒷단 | 외부 Gmail/Google API 호출 + Result 변환 |
| CommandService | `*CommandService` | 뒷단 | DB Write, `@Transactional` 선언 |
| QueryService | `*QueryService` | 뒷단 | DB Read |
