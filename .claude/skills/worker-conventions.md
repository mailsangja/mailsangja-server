---
name: worker-conventions
description: Worker 모듈 아키텍처 및 개발 규칙입니다. RabbitMQ 큐 등록, Listener, Handler, Message Record 작성 시 이 규칙을 따릅니다.
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

Worker 모듈의 모든 흐름은 RabbitMQ를 경계로 **앞단(Producer)**과 **뒷단(Consumer)**으로 명확히 분리됩니다.

```
[앞단 — Producer Side]
  HTTP 진입점 or Cron Scheduler
    → Facade (흐름 조율 + MQ 발행)
         → Classifier (이벤트 분류)
         → Publisher (MQ 발행)

[RabbitMQ 경계]

[뒷단 — Consumer Side]
  Listener (비즈니스 Facade 역할)
    → Handler (이벤트 타입별 전략 단위)
         → *ApiService (외부 API 호출)
         → *CommandService / *QueryService (DB 조작)
```

### 앞단 역할

- 동기 처리만 담당합니다: OIDC 검증, 토큰 갱신, 외부 API 조회, 이벤트 분류, MQ 발행
- DB Write를 직접 수행하지 않습니다. DB Write는 뒷단의 책임입니다.
- MQ 발행 완료 즉시 응답합니다 (Pub/Sub ACK).

### 뒷단 역할

- 핵심 비즈니스 로직 전체가 뒷단에 위치합니다.
- **Listener가 비즈니스 Facade 역할을 합니다. 뒷단에 별도의 Facade 클래스를 두지 않습니다.**
- Listener가 Handler, ApiService, CommandService를 직접 조율합니다.

---

## 패키지 구조

```
com.mailsangja.worker
├── controller/
│   └── {Domain}Controller.java
├── facade/
│   └── {Domain}Facade.java             # 앞단 HTTP 진입 흐름 조율 전용
├── handler/{domain}/
│   ├── *Classifier.java
│   └── *Handler.java
├── service/{domain}/
│   ├── {Domain}ApiService.java         # 외부 Gmail/Google API 호출 전용
│   ├── {Domain}CommandService.java     # DB Write
│   └── {Domain}QueryService.java       # DB Read
├── messaging/
│   ├── listener/
│   │   └── *Listener.java              # 뒷단 비즈니스 진입점 (Facade 역할)
│   └── publisher/
│       └── *Publisher.java             # 앞단 MQ 발행 전용
├── scheduler/
│   └── *Scheduler.java                 # Cron 트리거
├── config/
│   ├── rabbitmq/
│   │   ├── RabbitMqConfig.java         # Exchange, Converter, RabbitTemplate, 공통 유틸
│   │   └── {TaskName}RabbitConfig.java # 큐 1개당 파일 1개
│   └── properties/
│       └── *RabbitProperties.java
├── common/
│   ├── exception/
│   └── util/
└── dto/{domain}/
    ├── *Message.java    # MQ payload (record)
    ├── *Request.java    # 외부 Gmail API 요청 파라미터 (record)
    ├── *Response.java   # 외부 Gmail API 원본 응답 (Jackson 역직렬화용, record)
    ├── *Result.java     # 내부 변환 결과 (record)
    └── *Command.java    # 내부 명령 전달 (record)
```

---

## 새 메시지 큐 등록

새 task를 추가할 때 반드시 아래를 함께 정의한다.

### 1. application-mq.yaml — 튜닝 값만 관리

큐 이름은 코드에 고정(하드코딩)한다. `application-mq.yaml`에는 운영 튜닝 값(동시성·TTL·재시도)만 환경변수로 관리한다.

```yaml
# application-mq.yaml
mailsangja:
  rabbitmq:
    task:
      concurrency: ${MAIL_TASK_CONCURRENCY:1}
      ttl: ${MAIL_TASK_TTL:30m}
      retry-max-attempts: ${MAIL_TASK_RETRY_MAX_ATTEMPTS:3}
```

task 이름은 점(`.`) 구분 소문자 형식, 도메인이 아닌 작업 의미 중심으로 짓는다.

- 예: `sync.gmail.initial`, `event.gmail.message-added`, `watch.renewal.gmail`

> **큐 이름은 RabbitMQ 브로커에 실제로 생성되는 인프라 이름이므로 환경마다 달라지면 안 된다. yaml이 아닌 Properties 클래스 상수로 고정하고, 동시성·TTL 같은 운영 튜닝 값만 환경변수로 관리한다.**

### 2. Properties 클래스

위치: `config/properties/{TaskName}RabbitProperties.java`

```java
@Component
public class {TaskName}RabbitProperties {

    private static final String TASK_NAME = "{task.name}";

    public String getTaskName()              { return TASK_NAME; }
    public String getQueueName()             { return "mailsangja." + TASK_NAME; }
    public String getRoutingKey()            { return "mail." + TASK_NAME; }
    public String getDeadLetterQueueName()   { return getQueueName() + ".dlq"; }
    public String getDeadLetterRoutingKey()  { return getRoutingKey() + ".dlq"; }
}
```

> **`TASK_NAME`을 private static final 상수로 선언하고 나머지 이름은 반드시 여기서 파생한다. `@ConfigurationProperties` 사용 금지 — yaml 키 부재 시 기동 실패의 원인이 된다.**

### 3. RabbitConfig 클래스

위치: `config/rabbitmq/{TaskName}RabbitConfig.java` — **큐 1개당 파일 1개**

반드시 아래 7개 Bean을 모두 정의한다.

| Bean | 설명 |
|------|------|
| `{taskName}Queue` | 메인 큐 (TTL + DLX 설정) |
| `{taskName}DeadLetterQueue` | DLQ (단순 durable) |
| `{taskName}Binding` | 메인 큐 → mailTaskExchange 바인딩 |
| `{taskName}DeadLetterBinding` | DLQ → mailTaskDeadLetterExchange 바인딩 |
| `{taskName}MessageRecoverer` | 재시도 소진 시 DLQ 전송 |
| `{taskName}RetryInterceptor` | stateless 재시도 정책 |
| `{taskName}RabbitListenerContainerFactory` | Concurrency, Retry, Converter 설정 |

```java
@Configuration
public class {TaskName}RabbitConfig {

    private static final Logger log = LoggerFactory.getLogger({TaskName}RabbitConfig.class);

    @Bean
    public Queue {taskName}Queue(
            {TaskName}RabbitProperties properties,
            MailTaskRabbitProperties mailTaskRabbitProperties
    ) {
        return QueueBuilder.durable(properties.getQueueName())
                .ttl(RabbitMqConfig.toQueueTtlMillis(mailTaskRabbitProperties.getTtl(), "mailsangja.rabbitmq.task.ttl"))
                .deadLetterExchange(mailTaskRabbitProperties.getDeadLetterExchange())
                .deadLetterRoutingKey(properties.getDeadLetterRoutingKey())
                .build();
    }

    @Bean
    public Queue {taskName}DeadLetterQueue({TaskName}RabbitProperties properties) {
        return QueueBuilder.durable(properties.getDeadLetterQueueName()).build();
    }

    @Bean
    public Binding {taskName}Binding(
            @Qualifier("{taskName}Queue") Queue {taskName}Queue,
            @Qualifier("mailTaskExchange") DirectExchange mailTaskExchange,
            {TaskName}RabbitProperties properties
    ) {
        return BindingBuilder.bind({taskName}Queue).to(mailTaskExchange).with(properties.getRoutingKey());
    }

    @Bean
    public Binding {taskName}DeadLetterBinding(
            @Qualifier("{taskName}DeadLetterQueue") Queue {taskName}DeadLetterQueue,
            @Qualifier("mailTaskDeadLetterExchange") DirectExchange mailTaskDeadLetterExchange,
            {TaskName}RabbitProperties properties
    ) {
        return BindingBuilder.bind({taskName}DeadLetterQueue)
                .to(mailTaskDeadLetterExchange)
                .with(properties.getDeadLetterRoutingKey());
    }

    @Bean
    public MessageRecoverer {taskName}MessageRecoverer({TaskName}RabbitProperties properties) {
        return (message, cause) -> {
            log.warn(
                    "{TaskName} retries exhausted. Sending to DLQ routingKey={} messageId={} payloadSize={}B",
                    properties.getDeadLetterRoutingKey(),
                    message.getMessageProperties().getMessageId(),
                    message.getBody().length,
                    cause
            );
            throw new AmqpRejectAndDontRequeueException("{TaskName} retries exhausted", cause);
        };
    }

    @Bean
    public MethodInterceptor {taskName}RetryInterceptor(
            MailTaskRabbitProperties properties,
            @Qualifier("{taskName}MessageRecoverer") MessageRecoverer {taskName}MessageRecoverer
    ) {
        return RetryInterceptorBuilder.stateless()
                .maxRetries(properties.getRetryMaxAttempts())
                .recoverer({taskName}MessageRecoverer)
                .build();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory {taskName}RabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter rabbitMessageConverter,
            @Qualifier("{taskName}RetryInterceptor") MethodInterceptor {taskName}RetryInterceptor,
            MailTaskRabbitProperties properties
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(rabbitMessageConverter);
        factory.setConcurrentConsumers(properties.getConcurrency());
        factory.setMaxConcurrentConsumers(properties.getConcurrency());
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain({taskName}RetryInterceptor);
        return factory;
    }
}
```

> **`rabbitListenerContainerFactory` (default 이름)는 `InitialMailSyncRabbitConfig`가 점유 중이다. 새 task는 반드시 `{taskName}RabbitListenerContainerFactory` 이름으로 등록하고 `@RabbitListener`에 `containerFactory`를 명시해야 한다.**

### 4. RabbitMqConfig 역할

`RabbitMqConfig`는 공통 인프라와 shared 유틸 메서드만 담당한다. 큐/바인딩은 각 `{TaskName}RabbitConfig`에서 정의한다.

| Bean / 유틸 | 설명 |
|------------|------|
| `mailTaskExchange` | 공유 Direct Exchange |
| `mailTaskDeadLetterExchange` | 공유 DLX |
| `rabbitMessageConverter` | `JacksonJsonMessageConverter` |
| `rabbitTemplate` | 발행 전용 Template |
| `static toQueueTtlMillis(Duration, String)` | TTL 검증 유틸 |
| `static validateTaskName(String, String)` | taskName 검증 유틸 |

---

## DTO / Message Record

> **모든 DTO는 Java `record`. 검증 책임은 record 내부에 집중하여 응집도를 높인다.**

### MQ Message — compact constructor 검증

```java
// ✅ compact constructor에서 self-validation
public record {TaskName}Message(
        UUID mailAccountId,
        UUID userId,
        String provider,
        String emailAddress
) {
    public {TaskName}Message {
        Objects.requireNonNull(mailAccountId, "mailAccountId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        if (provider == null || provider.isBlank())
            throw new IllegalArgumentException("provider must not be blank");
        if (emailAddress == null || emailAddress.isBlank())
            throw new IllegalArgumentException("emailAddress must not be blank");
        if (!MailProvider.GMAIL.name().equals(provider))
            throw new IllegalArgumentException("Unsupported provider: " + provider);
    }
}

// ❌ 검증을 Publisher에 흩뿌리는 방식 지양
public void publish(SomeMessage message) {
    if (message.mailAccountId() == null || ...) { throw ...; }
}
```

### HTTP 진입 DTO — decode() 위임

복잡한 파싱과 검증이 필요한 HTTP 진입 DTO는 `decode()` 메서드로 변환 책임을 record 내부에 위임한다. Facade는 결과만 받는다.

```java
// ✅ DTO가 파싱 + 검증 + 변환을 모두 담당
public record GooglePubsubPushRequest(GooglePubsubMessageRequest message, String subscription) {

    public GoogleMailPushNotificationResult decode(ObjectMapper objectMapper) {
        if (message == null || message.data() == null || message.data().isBlank())
            throw new MailPushException(MailPushErrorCode.INVALID_PUBSUB_MESSAGE_DATA);
        try {
            byte[] decoded = Base64.getDecoder().decode(message.data());
            return objectMapper.readValue(decoded, GoogleMailPushNotificationResult.class);
        } catch (IllegalArgumentException | IOException e) {
            throw new MailPushException(MailPushErrorCode.INVALID_PUBSUB_MESSAGE_DATA);
        }
    }
}

// compact constructor로 결과 DTO도 self-validation
public record GoogleMailPushNotificationResult(String emailAddress, String historyId) {
    public GoogleMailPushNotificationResult {
        if (emailAddress == null || emailAddress.isBlank())
            throw new MailPushException(MailPushErrorCode.INVALID_GMAIL_PUSH_NOTIFICATION);
        if (historyId == null || historyId.isBlank())
            throw new MailPushException(MailPushErrorCode.INVALID_GMAIL_PUSH_NOTIFICATION);
    }
}
```

### 정적 팩토리 메서드 (`from`)

Entity → Message 변환이 한 곳 이상에서 발생하면 `from()` 정적 팩토리를 Record 내부에 선언한다.

```java
public record WatchRenewalMessage(...) {
    // compact constructor ...

    public static WatchRenewalMessage from(MailAccount mailAccount) {
        return new WatchRenewalMessage(
                mailAccount.getId(),
                mailAccount.getUser().getId(),
                mailAccount.getProvider().name(),
                mailAccount.getEmailAddress()
        );
    }
}
```

### DTO 접미사 구분

| 접미사 | 역할 | 예시 |
|--------|------|------|
| `*Message` | RabbitMQ 메시지 payload | `WatchRenewalMessage` |
| `*Request` | 외부 Gmail API 요청 파라미터 / HTTP 요청 입력 | `GmailMessageListRequest` |
| `*Response` | 외부 Gmail API 원본 JSON 응답 (Jackson 역직렬화) | `GmailThreadResponse` |
| `*Result` | 내부 변환 결과 | `GmailHistoryListResult` |
| `*Command` | 내부 명령 전달 | `RenewGoogleWatchCommand` |

---

## 앞단 규칙 (Producer Side)

### Facade

앞단 Facade는 HTTP 요청 수명 내에서 수행해야 하는 동기 흐름만 조율한다.

- 핵심 비즈니스 로직을 포함하지 않는다.
- 저수준 파싱, null 체크, 필드 검증을 Facade 메서드 안에 직접 작성하지 않는다. DTO의 compact constructor 또는 `decode()` 메서드에 위임한다.
- MailAccount 도메인 로직은 엔티티 메서드에 위임한다.
- MQ 발행 완료 후 즉시 응답한다. DB Write 결과를 기다리지 않는다.

```java
// ✅ Facade — 흐름 조율만, 저수준 검증 없음
public void handlePush(String authorizationHeader, GooglePubsubPushRequest request) {
    googlePubsubOidcApiService.validateAuthorization(authorizationHeader);

    GoogleMailPushNotificationResult notification = request.decode(objectMapper);

    MailAccount mailAccount = googleAccessTokenEnsureService.ensureValidGoogleAccessToken(
            mailAccountQueryService.findActiveGoogleMailAccountByEmailAddress(notification.emailAddress())
    );

    GoogleMailHistoryListResult historyResult = gmailHistoryApiService.getHistory(
            mailAccount.getAccessToken(),
            mailAccount.resolveStartHistoryId(notification.historyId())
    );

    // publishAll()이 AmqpException을 전파하면 아래 updateSyncHistoryId는 호출되지 않는다.
    // Pub/Sub은 NACK → 재전달하므로 이벤트 누락 없이 재처리된다.
    gmailHistoryEventPublisher.publishAll(
            gmailHistoryEventClassifier.classify(mailAccount, historyResult)
    );

    mailAccountCommandService.updateSyncHistoryId(mailAccount, historyResult.historyId());
}
```

도메인 로직은 엔티티에 위임한다.

```java
// MailAccount 엔티티 — resolveStartHistoryId
public String resolveStartHistoryId(String fallback) {
    return (syncHistoryId != null && !syncHistoryId.isBlank()) ? syncHistoryId : fallback;
}
```

### Publisher

`RabbitTemplate`을 직접 여기저기 주입하지 않고 반드시 `*Publisher`를 통해서만 발행한다.

```java
@Component
@RequiredArgsConstructor
public class GmailHistoryEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final MailTaskRabbitProperties properties;

    public void publishAll(List<GmailHistoryEvent> events) {
        events.forEach(this::publish);
    }

    private void publish(GmailHistoryEvent event) {
        rabbitTemplate.convertAndSend(
                properties.getExchange(),
                event.eventType().getRoutingKey(),
                event,
                new CorrelationData(event.mailAccountId() + ":" + event.gmailMessageId())
        );
    }
}
```

- Message 필드 검증은 Record compact constructor 책임 — Publisher에서 중복 검증 금지
- Publisher는 Properties 설정값의 유효성만 검증한다
- `AmqpException`은 그대로 전파한다 — catch 후 삼키지 않는다
- `GmailHistoryEventType`에 `getRoutingKey()` 메서드를 두어 라우팅 키를 중앙화한다

---

## 뒷단 규칙 (Consumer Side)

### Listener와 Handler를 두 레이어로 나누는 이유

Listener는 단순한 "MQ 수신 어댑터"가 아닙니다. `@RabbitListener`에 선언된 `containerFactory`가 그 메서드에 적용되는 **concurrency · retry · DLQ 정책 전체**이기 때문에, Listener 메서드가 비즈니스 흐름 조율의 진입점이자 처리 특성의 경계선이 됩니다.

```
Listener 메서드
    ↳ containerFactory = "..." → 이 메서드에만 적용되는 concurrency / retry / DLQ
```

같은 클래스 안의 두 메서드도 서로 다른 ContainerFactory를 가질 수 있으므로, 처리 비용이 다른 이벤트를 하나의 Listener 클래스로 묶으면서도 정책을 분리할 수 있습니다.

**Listener의 책임 (흐름 조율):**
- MailAccount 조회
- 토큰 갱신 (`GoogleAccessTokenEnsureService`) — Gmail API 호출이 있는 경우만
- Handler 디스패치 (어떤 핸들러를 실행할지 결정)
- 후속 MQ 발행 (필요한 경우)

**Handler의 책임 (비즈니스 실행):**
- `*ApiService` + `*CommandService` 조합으로 실제 작업 수행
- 이벤트 타입 단위의 응집된 비즈니스 로직

이 분리로 인해 Listener는 "무엇을 준비하고 누구에게 넘길 것인가"만 담당하고, Handler는 "실제로 무엇을 할 것인가"만 담당합니다.

---

### Handler 분리 기준

**Handler를 분리해야 하는 경우:**

1. **같은 ContainerFactory를 공유하는 여러 이벤트 타입** — 하나의 Listener 메서드에서 여러 타입을 받을 때, `if/switch` 대신 전략 패턴으로 Handler를 분리한다. 새 타입 추가 시 Listener 수정 없이 Handler `@Component` 등록만으로 확장 가능하다.

    ```java
    // ✅ 전략 패턴 Handler — Listener는 dispatch만
    stateChangeHandlers.stream()
        .filter(h -> h.supports() == event.eventType())
        .findFirst()
        .orElseThrow(...)
        .handle(event);
    ```

2. **비즈니스 로직이 복잡해 Listener 메서드가 길어지는 경우** — Gmail Thread API 조회 + DB 저장 + FCM 푸시처럼 여러 서비스를 조합하는 경우 전용 Handler로 분리한다.

**Handler 없이 Listener에 직접 작성하는 경우:**

큐 1개 = 이벤트 타입 1가지 = 처리 흐름 1가지이고 로직이 직선적으로 이어지는 경우는 Handler로 추출하지 않는다.

```java
// ✅ GmailWatchRenewalListener — 단순 직선 흐름, Handler 불필요
public void handle(WatchRenewalMessage message) {
    MailAccount mailAccount = mailAccountQueryService.findActiveMailAccountById(...);
    GoogleOAuthTokenResult tokenResult = googleOAuthApiService.refreshAccessToken(...);
    GoogleMailWatchResult watchResult = gmailWatchApiService.watch(...);
    mailAccountCommandService.renewGoogleWatch(...);
}
```

| 상황 | 선택 |
|------|------|
| 큐 1개, 이벤트 타입 1가지, 직선 흐름 | **Listener에 직접 작성** |
| 같은 ContainerFactory를 공유하는 여러 이벤트 타입 | **`GmailHistoryEventHandler` 전략 패턴** |
| 타입은 1가지지만 비즈니스 로직이 복잡 | **전용 Handler 분리** (`MessageAddedHistoryEventHandler` 패턴) |

---

### Listener — 비즈니스 Facade 역할

Listener는 MQ 메시지를 수신하는 진입점이자, 뒷단 비즈니스 흐름의 Facade다.
**별도 Facade 클래스 없이** Listener 메서드 안에서 비즈니스 흐름을 직접 조율한다.

**두 그룹을 ContainerFactory로 분리하는 구조:**

```java
// ✅ message-added — Listener에서 토큰 갱신 후 전용 Handler에 MailAccount 직접 전달
@RabbitListener(
        queues = "#{@gmailMessageAddedQueue.name}",
        containerFactory = "gmailMessageAddedContainerFactory"
)
public void handleMessageAdded(GmailHistoryEvent event) {
    MailAccount mailAccount = googleAccessTokenEnsureService.ensureValidGoogleAccessToken(
            mailAccountQueryService.findActiveMailAccountById(event.mailAccountId())
    );
    messageAddedHandler.handle(mailAccount, event);
}

// ✅ history-state 5개 — 전략 풀에 이벤트만 전달, 토큰 갱신은 각 Handler 책임
@RabbitListener(
        queues = {
            "#{@gmailMessageReadQueue.name}",
            "#{@gmailMessageUnreadQueue.name}",
            "#{@gmailMessageTrashedQueue.name}",
            "#{@gmailMessageRestoredQueue.name}",
            "#{@gmailMessagePermanentlyDeletedQueue.name}"
        },
        containerFactory = "gmailHistoryStateContainerFactory"
)
public void handleStateChange(GmailHistoryEvent event) {
    stateChangeHandlers.stream()
            .filter(h -> h.supports() == event.eventType())
            .findFirst()
            .orElseThrow(() -> new MailPushException(MailPushErrorCode.GMAIL_HISTORY_RESULT_INVALID))
            .handle(event);
}
```

**`MessageAddedHistoryEventHandler`는 전략 풀이 아닌 전용 컴포넌트:**

`GmailHistoryEventHandler` 인터페이스를 구현하지 않으며 `(MailAccount, GmailHistoryEvent)` 시그니처를 가진다. Listener가 이미 갱신된 `MailAccount`를 전달하므로 Handler 내부에서 토큰 갱신이 필요 없다.

**전략 풀 Handler의 토큰 갱신 위치:**

`history-state` 핸들러는 Listener로부터 이벤트만 받으므로, Gmail API 호출이 필요한 경우 Handler 내부에서 직접 토큰을 갱신한다.

```java
// ✅ read/unread — 메시지 미존재 시 Gmail API 재조회 가능, Handler 내부에서 토큰 갱신
@Override
public void handle(GmailHistoryEvent event) {
    MailAccount mailAccount = googleAccessTokenEnsureService.ensureValidGoogleAccessToken(
            mailAccountQueryService.findActiveMailAccountById(event.mailAccountId())
    );
    InitialMailSyncThreadSaveCommand syncCommand = prepareSyncCommandIfNeeded(mailAccount, event);
    gmailHistoryStateApplyCommandService.applyMessageReadState(mailAccount, event, true, syncCommand);
}

// ✅ trashed/permanently-deleted — DB 반영만, 토큰 갱신 없음
@Override
public void handle(GmailHistoryEvent event) {
    MailAccount mailAccount = mailAccountQueryService.findActiveMailAccountById(event.mailAccountId());
    gmailHistoryDeleteApplyCommandService.applyMessageTrashed(mailAccount, event);
}
```

- `queues`는 반드시 `#{@{beanName}.name}` SpEL 참조 — 문자열 하드코딩 금지
- `containerFactory`에 해당 task의 팩토리 Bean 이름을 명시한다
- 같은 도메인의 관련 큐 여러 개는 하나의 Listener 클래스에 묶을 수 있다

---

## 큐 / ContainerFactory 전체 현황

### ContainerFactory 목록

| ContainerFactory Bean 이름 | 정의 위치 | 처리 특성 |
|---------------------------|-----------|-----------|
| `rabbitListenerContainerFactory` | `InitialMailSyncRabbitConfig` | default factory — `containerFactory` 미지정 시 사용 |
| `gmailMessageAddedContainerFactory` | `GmailMessageAddedRabbitConfig` | message-added 전용 |
| `initialMailSyncThreadBatchRabbitListenerContainerFactory` | `InitialMailSyncThreadBatchRabbitConfig` | 초기 동기화 2단계 배치 전용 |
| `gmailHistoryStateContainerFactory` | `GmailMessagePermanentlyDeletedRabbitConfig` | history-state 5개 큐 공유 |
| `watchRenewalRabbitListenerContainerFactory` | `WatchRenewalRabbitConfig` | Watch 갱신 전용 |

> **`gmailHistoryStateContainerFactory`는 `GmailMessagePermanentlyDeletedRabbitConfig`에 정의되어 있으며, read/unread/trashed/restored/permanently-deleted 5개 큐가 공유한다. 나머지 4개 config(`GmailMessageReadRabbitConfig` 등)는 Queue + Binding Bean만 정의하고 ContainerFactory는 정의하지 않는다.**

### 큐 → ContainerFactory → Listener 연결 전체 맵

| 큐 Bean 이름 | 큐 이름 (runtime) | ContainerFactory | Listener 메서드 | Handler |
|---|---|---|---|---|
| `initialMailSyncQueue` | `mailsangja.sync.gmail.initial` | `rabbitListenerContainerFactory` (default) | `InitialMailSyncListener#handle()` | 없음 (Listener 직접 처리) |
| `initialMailSyncThreadBatchQueue` | `mailsangja.sync.gmail.initial.thread-batch` | `initialMailSyncThreadBatchRabbitListenerContainerFactory` | `InitialMailSyncListener#handleThreadBatch()` | 없음 (Listener 직접 처리) |
| `watchRenewalQueue` | `mailsangja.watch.renewal.gmail` | `watchRenewalRabbitListenerContainerFactory` | `GmailWatchRenewalListener#handle()` | 없음 (Listener 직접 처리) |
| `gmailMessageAddedQueue` | `mailsangja.event.gmail.message-added` | `gmailMessageAddedContainerFactory` | `GmailHistoryEventListener#handleMessageAdded()` | `MessageAddedHistoryEventHandler` (전용, 인터페이스 미구현) |
| `gmailMessageReadQueue` | `mailsangja.event.gmail.message-read` | `gmailHistoryStateContainerFactory` | `GmailHistoryEventListener#handleStateChange()` | `MessageReadHistoryEventHandler` |
| `gmailMessageUnreadQueue` | `mailsangja.event.gmail.message-unread` | `gmailHistoryStateContainerFactory` | `GmailHistoryEventListener#handleStateChange()` | `MessageUnreadHistoryEventHandler` |
| `gmailMessageTrashedQueue` | `mailsangja.event.gmail.message-trashed` | `gmailHistoryStateContainerFactory` | `GmailHistoryEventListener#handleStateChange()` | `MessageTrashedHistoryEventHandler` |
| `gmailMessageRestoredQueue` | `mailsangja.event.gmail.message-restored` | `gmailHistoryStateContainerFactory` | `GmailHistoryEventListener#handleStateChange()` | `MessageRestoredHistoryEventHandler` |
| `gmailMessagePermanentlyDeletedQueue` | `mailsangja.event.gmail.message-permanently-deleted` | `gmailHistoryStateContainerFactory` | `GmailHistoryEventListener#handleStateChange()` | `MessagePermanentlyDeletedHistoryEventHandler` |

### InitialMailSyncListener의 Producer/Consumer 이중 역할

`InitialMailSyncListener`는 동일 클래스에서 두 단계를 처리한다.

```
[1단계] initialMailSyncQueue 소비
    → Gmail Messages API로 최신 메일 목록 조회
    → threadId 중복 제거 후 batchSize로 분할
    → initialMailSyncThreadBatchQueue로 배치 발행   ← Producer 역할

[2단계] initialMailSyncThreadBatchQueue 소비
    → 각 threadId별 Gmail Thread API 조회
    → DB 저장
```

1단계는 default ContainerFactory(`rabbitListenerContainerFactory`), 2단계는 `initialMailSyncThreadBatchRabbitListenerContainerFactory`를 사용해 처리 특성을 분리한다.

---

## Gmail ApiService 규칙

외부 Google API를 호출하는 서비스는 반드시 `*ApiService`로 명명한다. 내부 비즈니스 서비스(`*CommandService`, `*QueryService`)와 명확히 구분한다.

| 클래스 | 패키지 | 역할 |
|--------|--------|------|
| `GmailHistoryApiService` | `service/google/` | Gmail History API 호출 |
| `GmailMessageApiService` | `service/google/` | Gmail Messages/Threads API 호출 |
| `GmailWatchApiService` | `service/google/` | Gmail Watch API 호출 |
| `GooglePubsubOidcApiService` | `service/google/` | Pub/Sub OIDC 검증 |
| `GoogleOAuthApiService` | `service/google/` | Google OAuth 토큰 교환 |

- `*ApiService`는 외부 API를 호출하고 `*Response` → `*Result` 변환까지 담당한다.
- 호출 측(Listener)은 `*Result`만 받는다. 원본 `*Response`를 직접 다루지 않는다.

```java
// ✅ ApiService — 호출 + 변환 캡슐화
public GoogleMailMessageListResult listMessages(String accessToken, int maxResults) {
    GmailMessageListResponse response = gmailApiClient.listMessages(accessToken, maxResults);
    return GoogleMailMessageListResult.from(response);
}

// ✅ 호출 측은 Result만 사용
GoogleMailMessageListResult result = gmailMessageApiService.listMessages(accessToken, 20);
```

### Google Access Token 갱신 규칙

모든 Google API 호출 전에 반드시 `GoogleAccessTokenEnsureService.ensureValidGoogleAccessToken()`으로 토큰을 선제 갱신한다.

```java
// ✅
MailAccount mailAccount = googleAccessTokenEnsureService.ensureValidGoogleAccessToken(
        mailAccountQueryService.findById(message.mailAccountId())
);

// ❌ 금지 — 갱신 없이 직접 사용
String accessToken = mailAccount.getAccessToken();
```

- `mailAccount.getAccessToken()` 직접 사용 금지
- 앞단 Facade, 뒷단 Listener / Handler 모두 동일하게 적용한다
- 만료 10분 전 갱신 버퍼를 두어 API 호출 도중 만료를 방지한다

---

## Handler / Classifier 규칙

### Classifier

- History API 응답을 내부 이벤트 타입으로 변환하는 순수 해석 로직만 담당한다.
- 동일 메시지 ID에 대해 여러 이벤트가 겹칠 경우, `LinkedHashMap<messageId, event>` 구조로 최신 이벤트만 유지한다 (중복 제거).
- 처리 우선순위 순서대로 분류하며, 뒤에 분류된 이벤트가 앞의 이벤트를 덮어쓴다.
- 외부 I/O, DB 조작, 상태 변경을 포함하지 않는다.

### Handler

Handler는 이벤트 타입별 비즈니스 처리 단위다. Listener로부터 호출되어 `*ApiService` + `*CommandService`를 조합해 실제 작업을 수행한다.

- `supports()` 반환값과 클래스명 접두사를 반드시 일치시킨다 (`MESSAGE_ADDED` → `MessageAdded...`)
- `@Component`로만 등록하면 Listener의 `List<{Domain}EventHandler>`에 자동 주입된다
- Repository Port 또는 JPA Module을 Handler에서 직접 주입하지 않는다
- 외부 API 조회는 트랜잭션과 DB 락 바깥에서 수행한다
- 락을 획득한 뒤에는 대상 상태를 다시 검증한다. 락 밖에서 확인한 상태를 그대로 신뢰하지 않는다

```java
// ✅ 전략 Handler — GmailHistoryEventHandler 구현
@Component
@RequiredArgsConstructor
public class MessageTrashedHistoryEventHandler implements GmailHistoryEventHandler {

    private final MailAccountQueryService mailAccountQueryService;
    private final GmailHistoryDeleteApplyCommandService gmailHistoryDeleteApplyCommandService;

    @Override
    public GmailHistoryEventType supports() {
        return GmailHistoryEventType.MESSAGE_TRASHED;
    }

    @Override
    public void handle(GmailHistoryEvent event) {
        MailAccount mailAccount = mailAccountQueryService.findActiveMailAccountById(event.mailAccountId());
        gmailHistoryDeleteApplyCommandService.applyMessageTrashed(mailAccount, event);
    }
}

// ✅ 전용 Handler — 인터페이스 미구현, Listener가 MailAccount를 직접 전달
@Component
@RequiredArgsConstructor
public class MessageAddedHistoryEventHandler {

    private final GmailNewMessageSyncCommandService gmailNewMessageSyncCommandService;
    private final FcmPushCommandService fcmPushCommandService;

    public void handle(MailAccount mailAccount, GmailHistoryEvent event) {
        NewMailPushContext context = gmailNewMessageSyncCommandService.syncNewMessage(mailAccount, event);
        fcmPushCommandService.sendNewMailPush(context);
    }
}
```

### 새 이벤트 타입 추가

1. 이벤트 enum에 새 항목 추가 + `getRoutingKey()` 반환값 추가
2. `{EventType}{Domain}EventHandler` 구현체 작성 후 `@Component` 등록
3. Google API 응답에서 새 타입이 식별되어야 한다면 `*Classifier`에 분류 로직 추가
4. 새 큐가 필요하면 Properties + RabbitConfig + Listener 등록

---

## Gmail 동시성 규칙

- `Message`, `Thread` 상태를 변경하는 모든 비동기 write 경로는 `mailAccountId + gmailThreadId` 단위로 직렬화한다.
- 락 획득에는 `GmailThreadLockRepositoryPort.acquireThreadLock()`을 사용한다.
- 락을 획득한 뒤에만 Thread 집계 상태(`message_count`, `is_read` 등)를 계산하고 반영한다.
- Gmail 원본 thread snapshot으로 DB를 보강할 때는 기존 row를 무분별하게 overwrite하지 말고, 락 획득 이후 현재 DB 상태를 다시 확인한 뒤 필요한 최소 범위만 반영한다.

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
