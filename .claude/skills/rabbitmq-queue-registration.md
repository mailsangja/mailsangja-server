---
name: rabbitmq-queue-registration
description: Worker 모듈에서 새 RabbitMQ 큐를 등록하는 절차입니다. Properties, RabbitConfig, Listener 연결까지 전 과정을 다룹니다.
allowed-tools: Read, Write, Edit, Glob
---

# RabbitMQ 새 큐 등록 절차

새 task를 추가할 때 반드시 아래 순서대로 정의한다.

---

## 1. application-mq.yaml — 튜닝 값만 관리

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

---

## 2. Properties 클래스

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

---

## 3. RabbitConfig 클래스

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

---

## 4. RabbitMqConfig 역할

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

## 5. Listener 연결

새 큐 등록 후 반드시 Listener에 `@RabbitListener`를 연결한다.

- `queues`는 반드시 `#{@{beanName}.name}` SpEL 참조 — 문자열 하드코딩 금지
- `containerFactory`에 해당 task의 팩토리 Bean 이름을 명시한다

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

```
[1단계] initialMailSyncQueue 소비
    → Gmail Messages API로 최신 메일 목록 조회
    → threadId 중복 제거 후 batchSize로 분할
    → initialMailSyncThreadBatchQueue로 배치 발행   ← Producer 역할

[2단계] initialMailSyncThreadBatchQueue 소비
    → 각 threadId별 Gmail Thread API 조회 → DB 저장
```
