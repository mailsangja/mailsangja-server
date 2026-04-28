---
name: core-conventions
description: core 모듈(HTTP API 서버) 개발 규칙입니다. Controller, 인증 파라미터, Handler/Classifier, Redis 캐시 키 설계, 비동기 처리 작성 시 이 규칙을 따릅니다.
allowed-tools: Read, Write, Edit, Glob
---

# Core Module Conventions

`spring-api-rules.md`의 공통 규칙을 기반으로 하며, `core` 모듈에서 추가로 적용하는 규칙을 정의합니다.

- Root Package: `com.mailsangja.core`
- 역할: HTTP API 서버. 사용자 인증, Gmail OAuth 연동, Pub/Sub 수신, MQ 발행 담당
- 진입점: HTTP (REST API, OAuth Callback, Google Pub/Sub)

---

## 패키지 구조

```
com.mailsangja.core
├── controller/
│   ├── {Domain}Controller.java
│   └── docs/                         # Swagger interface
├── facade/{domain}/
│   └── {Domain}Facade.java
├── handler/{domain}/
│   ├── *Classifier.java             # 외부 입력을 내부 이벤트/분기 기준으로 해석
│   └── *Handler.java                # 해석된 이벤트를 적절한 service 호출로 연결
├── service/{domain}/
│   ├── {Domain}CommandService.java
│   └── {Domain}QueryService.java
├── common/
│   ├── auth/
│   │   ├── AuthUser.java
│   │   ├── AuthAdmin.java
│   │   └── AuthArgumentResolver.java
│   ├── dto/        # SliceResponse, PageResponse, ResponseDto
│   ├── exception/
│   └── util/
├── config/
│   ├── SecurityConfig.java
│   ├── AsyncConfig.java
│   ├── RabbitMqConfig.java
│   └── properties/
│       └── *Properties.java
├── dto/{domain}/
│   ├── *Result.java
│   ├── *Command.java
│   ├── *Request.java
│   └── *Response.java
└── {domain}/
    ├── exception/
    └── config/
```

---

## Controller

```java
@RestController
@RequiredArgsConstructor
public class UserController implements UserControllerDocs {

    private final UserFacade userFacade;

    @PostMapping("/api/v1/users")
    public ResponseEntity<Void> register(@RequestBody RegisterRequest request, @AuthUser User user) {
        userFacade.register(request, user);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/api/v1/admin/users/{id}")
    public ResponseEntity<Void> deleteUser(@AuthAdmin User admin, @PathVariable Long id) {
        userFacade.deleteUser(admin, id);
        return ResponseEntity.ok().build();
    }
}
```

- `@RequestMapping` 클래스 레벨 사용 금지 — 각 메서드에 **전체 경로** 작성 (예: `@PostMapping("/api/v1/users")`)
- `@RequestBody`, `@CookieValue`, `@PathVariable`, `@RequestParam` 등 **파라미터 레벨 애노테이션은 Docs 인터페이스에서 상속되지 않음** — 구현체 메서드에도 반드시 중복 선언할 것
- **모든 엔드포인트는 `/api/v1/`로 시작**
- 반환 타입은 반드시 `ResponseEntity<T>`
- Service 직접 호출 금지 — Facade만 호출
- `Principal` 사용 금지 — 반드시 `@AuthUser` / `@AuthAdmin` 애노테이션으로 `User` 객체를 직접 주입받을 것

---

## 인증 파라미터 애노테이션

컨트롤러에서 로그인 사용자를 `Principal`로 받지 않고, `@AuthUser` / `@AuthAdmin` 애노테이션으로 `User` 엔티티를 바로 주입받는다.
내부적으로 `AuthArgumentResolver`가 `SecurityContextHolder`에서 이메일을 꺼내 DB 조회 후 반환한다.
`AuthArgumentResolver`는 `WebMvcConfig.addArgumentResolvers()`에 등록되어 있다.

| 애노테이션 | 파일 | 비인증(Anonymous) | 비관리자 | 반환 |
|-----------|------|------------------|---------|------|
| `@AuthUser` | `common/auth/AuthUser.java` | `401` throw | — | `User` |
| `@AuthAdmin` | `common/auth/AuthAdmin.java` | `401` throw | `403` throw | `User` |

| 엔드포인트 성격 | 사용 애노테이션 |
|----------------|----------------|
| 로그인 필수 (일반 사용자) | `@AuthUser` |
| 로그인 필수 (관리자 전용) | `@AuthAdmin` |

```java
// ✅ 로그인 필수
@GetMapping("/api/v1/users")
public ResponseEntity<UserDetailResponse> getUserInfo(@AuthUser User user) { ... }

// ✅ 관리자 전용
@DeleteMapping("/api/v1/admin/users/{id}")
public ResponseEntity<Void> deleteUser(@AuthAdmin User admin, @PathVariable Long id) { ... }

// ❌ 금지 — Principal 직접 사용
@GetMapping("/api/v1/users")
public ResponseEntity<UserDetailResponse> getUserInfo(Principal principal) { ... }
```

---

## Handler / Classifier 규칙

core 모듈의 Handler / Classifier는 HTTP 요청 흐름에서 이벤트 분류와 처리 로직을 분리한다.

### Classifier

- 외부 HTTP 입력(Google Pub/Sub 알림, 외부 콜백 등)을 내부 이벤트 타입으로 해석한다
- 외부 I/O, DB 조작, 상태 변경을 포함하지 않는다 — 순수 분류 로직만 담당한다
- Facade에서 호출되며 분류 결과를 반환한다

### Handler

- Classifier가 분류한 이벤트 타입에 따라 적절한 Service 조합으로 처리한다
- `supports()` 반환값과 클래스명 접두사를 반드시 일치시킨다
- Facade에서 직접 호출되며, `@Component` 등록 시 `List<EventHandler>`에 자동 주입된다

```java
// ✅ Facade — Classifier + Handler 전략 패턴 조율
public void handlePubsubEvent(String header, PubsubRequest request) {
    List<InternalEvent> events = classifier.classify(request.decode(objectMapper));
    events.forEach(event ->
        handlers.stream()
            .filter(h -> h.supports() == event.eventType())
            .findFirst()
            .orElseThrow()
            .handle(event)
    );
}
```

> Worker 모듈의 Handler / Classifier는 MQ Listener 기반 흐름에 적용된다. 규칙은 `worker-conventions.md`의 **Handler / Classifier 규칙** 섹션을 참조한다.

---

## OAuth 연동 규칙

> 공통 OAuth 연동 흐름 규칙은 `spring-api-rules.md`의 **OAuth 연동 규칙** 섹션을 따른다. Gmail API 호출(Access Token 갱신, History/Message API)은 Worker 모듈이 담당한다.

---

## Redis Key Naming Convention

### 키 구조

```
{도메인} : {목적} : {entityId} : {식별자...}
```

예시:

```
MailEvent:views:{eventId}
MailEvent:user:{eventId}:{email}
MailEvent:anon:{eventId}:{ip}:{userAgent}
```

### 규칙

**1. prefix 상수는 어댑터 클래스 상단에 모두 선언한다**

```java
private static final String VIEW_KEY_PREFIX     = "MailEvent:views:";
private static final String USER_REQUEST_PREFIX = "MailEvent:user:";
private static final String ANON_REQUEST_PREFIX = "MailEvent:anon:";
private static final long   VIEW_COUNT_TTL_MINUTES = 4L;
private static final long   VIEW_DEDUP_TTL_SECONDS = 86400L;
```

**2. 키에서 entityId를 파싱하는 로직은 Port 메서드로 제공한다**

Service가 `split(":")[n]` 인덱스로 직접 파싱하면 키 형식이 바뀔 때 런타임에 버그가 발생한다.
파싱 책임은 키를 정의한 어댑터가 갖고, Port 메서드로 노출한다.

```java
// ❌ 금지 — Service가 키 구조를 직접 알면 안 됨
Long eventId = Long.parseLong(key.split(":")[2]);

// ✅ Port 메서드로 추상화
Long eventId = noticeCachePort.extractEventIdFromViewKey(key);

// Adapter 구현 — prefix 상수로 파싱해 키 형식 변경에 자동 대응
@Override
public Long extractEventIdFromViewKey(String key) {
    return Long.parseLong(key.substring(VIEW_KEY_PREFIX.length()));
}
```

**3. SCAN은 스케줄러 flush 대상 키에만 허용한다**

단순 존재 여부 확인은 `hasKey`(O(1))를 사용한다. SCAN은 O(N)이므로 남용하면 Redis 성능에 영향을 준다.

**4. 모든 Redis 키에는 반드시 TTL을 설정한다**

TTL 없는 키는 Redis 메모리를 영구 점유한다. `setIfAbsent`, `set` 호출 시 항상 TTL 파라미터를 포함한다.

---

## @Async Rules

- `@Async` 사용 가능 위치: **`PushFacade`만 허용**
- `@EnableAsync`는 `config/AsyncConfig.java`에서 활성화
