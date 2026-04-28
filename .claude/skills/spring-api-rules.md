---
name: spring-api-rules
description: 모든 모듈에 공통으로 적용되는 Spring Boot 개발 규칙입니다. 레이어 의존성, Service 분리, DTO 작성(compact constructor/decode 포함), 예외 처리, 트랜잭션, Lombok, Properties, 로깅, 외부 API 연동(ApiService), OAuth 연동 규칙을 정의합니다.
allowed-tools: Read, Write, Edit, Glob
---

# Spring API Development Rules

모든 모듈(`core`, `worker` 등)에 공통으로 적용되는 규칙입니다.

- Root Package: `com.mailsangja.{module}`
- Java 21 / Spring Boot 4.0.5 / PostgreSQL / Redis / RabbitMQ

모듈별 추가 규칙:

| 모듈 | 규칙 파일 |
|------|-----------|
| db 모듈 (Entity, Repository 패턴) | `.claude/skills/db-conventions.md` |
| core 모듈 (Controller, 인증, Handler/Classifier, Redis) | `.claude/skills/core-conventions.md` |
| worker 모듈 (Listener, Handler, 큐 설계) | `.claude/skills/worker-conventions.md` |
| 새 모듈 추가 절차 | `.claude/skills/new-module-guide.md` |

---

## Layer Dependency (단방향 엄수)

```
Controller → Facade → CommandService / QueryService → Repository
```

| 규칙 | 내용 |
|------|------|
| Controller | Facade만 호출 — Service 직접 호출 금지 |
| Facade | 같은 도메인 Command/QueryService 호출. 타 도메인은 Facade 레벨에서만 주입 |
| Facade → Facade | 금지 |
| Service → 타 도메인 Service | 금지 — 반드시 팀 회의 후 결정 |

---

## Validation Rules

- Validation 메서드는 검증 책임이 있는 계층 내부의 `private` 메서드로 작성한다
- **Facade는 위에서 아래로 들어오는 입력을 검증한다**
- Controller에서 전달받은 `*Request`, 쿼리 파라미터, path variable, 세션 값 등은 Facade에서 private validation 메서드로 검증한다
- **Service는 아래에서 위로 올라오는 결과를 검증한다**
- Repository에서 조회한 Entity, 외부 API 호출 결과, 캐시 조회 결과 등은 Service에서 private validation 메서드로 검증한다
- 단순 `null` 체크라도 계층 책임에 맞는 위치에서 수행한다. 상위 입력 검증은 Facade, 하위 결과 검증은 Service에서 처리한다

```java
// Facade: 상위 입력 검증
public class MailAccountFacade {
    public MailAccountResponse handleGoogleCallback(User user, String code) {
        validateAuthorizationCode(code);
        ...
    }
    private void validateAuthorizationCode(String code) { ... }
}

// Service: 하위 결과 검증
public class MailAccountCommandService {
    public MailAccount createGoogleMailAccount(User user, GoogleMailAccountResult result) {
        validateGoogleMailAccountResult(result);
        ...
    }
    private void validateGoogleMailAccountResult(GoogleMailAccountResult result) { ... }
}
```

**3. Java record의 self-validation은 compact constructor에서 수행한다**

record 생성 시점에 유효성이 보장되므로, 호출 측에서 중복 검증하지 않는다.

```java
// ✅ compact constructor에서 self-validation
public record SyncCommand(UUID mailAccountId, String provider) {
    public SyncCommand {
        Objects.requireNonNull(mailAccountId, "mailAccountId must not be null");
        if (provider == null || provider.isBlank())
            throw new IllegalArgumentException("provider must not be blank");
    }
}

// ❌ 금지 — compact constructor가 이미 보장하는 것을 호출 측에서 중복 검증
void doSomething(SyncCommand cmd) {
    if (cmd.mailAccountId() == null || ...) { throw ...; }
}
```

---

## Command / Query Service Split

```java
// CommandService — 쓰기 (INSERT / UPDATE / DELETE)
@Service
@RequiredArgsConstructor
public class UserCommandService {
    // register(), update(), delete() 등 상태 변경 메서드
}

// QueryService — 읽기 (SELECT)
@Service
@RequiredArgsConstructor
public class UserQueryService {
    // findById(), getList() 등 조회 전용 메서드
}
```

- `find*`, `get*`, `read*`, `exists*` 등 **읽기 성격의 메서드와 로직은 `QueryService`에서만 작성한다**
- `save*`, `create*`, `update*`, `delete*` 등 **쓰기 성격의 메서드와 로직은 `CommandService`에서만 작성한다**
- **동일 도메인에서는 `CommandService → QueryService` 호출을 허용한다**
- **동일 도메인에서도 `QueryService → CommandService` 호출은 금지한다**
- Repository의 읽기 메서드 호출은 `QueryService`를 기본 위치로 한다
- 메서드가 2개 이하이고 모두 같은 성격이면 단일 `{Domain}Service`로 유지 가능

---

## DTO

> **모든 DTO는 Java `record`로 작성. `@Data`/`@Getter` class 사용 금지.**

### 네이밍

| 접미사 | 기준 | 예시 |
|--------|------|------|
| `*Request` | Controller 메서드 파라미터로 직접 사용 (HTTP 요청 입력) | `OauthRequest`, `UserUpdateRequest` |
| `*Response` | Controller 메서드 반환 타입으로 직접 사용 (HTTP 응답 출력) | `LoginResponse`, `UserInfoResponse` |
| `*Result` | Controller 메서드 시그니처에 등장하지 않는 내부 결과 전달 | `UserLoginResult`, `GoogleUserInfoResult` |
| `*Command` | Controller 메서드 시그니처에 등장하지 않는 내부 명령 전달 | `SendNotificationCommand` |

- **Controller 메서드의 파라미터/반환 타입에 직접 등장하면 `*Request` / `*Response`, 그 외 내부 전달이면 `*Result` / `*Command`**
- `*Dto` 접미사 사용 금지

### `*Result` / `*Command` 변환 규칙

- `*Result`는 `*Command`를 직접 생성하거나 반환하지 않는다
- 상태 변경 입력으로 변환이 필요하면 `*Command` 쪽에 `from(result, ...)` 정적 팩토리 메서드를 우선 둔다
- `from(...)` 메서드는 매핑과 조립 책임만 가지며, 복잡한 비즈니스 판단까지 포함하지 않는다

### Record Self-Validation — compact constructor

DTO는 Java record로 작성하므로 유효성 검증은 compact constructor에서 수행한다. record 생성 시점에 유효성이 보장된다.

```java
// ✅ MQ payload, Result, Command 모두 동일하게 적용
public record UserCommand(String email, String name) {
    public UserCommand {
        if (email == null || email.isBlank())
            throw new IllegalArgumentException("email must not be blank");
        Objects.requireNonNull(name, "name must not be null");
    }
}
```

### HTTP 진입 DTO — decode() 위임

복잡한 파싱과 검증이 필요한 HTTP 진입 DTO는 `decode()` 메서드로 변환 책임을 record 내부에 위임한다. Facade는 결과만 받는다.

```java
// ✅ DTO가 파싱 + 검증 + 변환을 모두 담당
public record PushRequest(MessageData message, String subscription) {

    public NotificationResult decode(ObjectMapper objectMapper) {
        if (message == null || message.data() == null || message.data().isBlank())
            throw new PushException(PushErrorCode.INVALID_DATA);
        try {
            byte[] decoded = Base64.getDecoder().decode(message.data());
            return objectMapper.readValue(decoded, NotificationResult.class);
        } catch (IllegalArgumentException | IOException e) {
            throw new PushException(PushErrorCode.INVALID_DATA);
        }
    }
}
```

### Presentation DTO 조립 책임

> **Service는 `*Response` DTO를 생성하거나 반환하지 않는다. `*Response` 조립은 반드시 Facade에서 수행한다.**

| 레이어 | 책임 |
|--------|------|
| Service | 비즈니스 로직 수행, 도메인 객체(Entity / `*Result`) 반환 |
| Facade | Service 결과를 받아 `*Response` DTO로 조립 후 Controller에 전달 |

```java
// ✅ Service — 도메인 객체 반환
public class UserService {
    public User findByEmail(String email) { ... }
}

// ✅ Facade — Response 조립
public class UserFacade {
    public UserInfoResponse getUserInfo(User user) {
        return new UserInfoResponse(user.getName(), user.getEmail());
    }
}

// ❌ 금지 — Service가 Response DTO 생성
public class UserService {
    public UserInfoResponse getUserInfo(User user) { ... }
}
```

### Response — `from()` / `of()`

```java
// 단일 도메인
public record UserResponse(Long id, String email) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail());
    }
}

// 다중 도메인 조합
public record NotificationDetailResponse(Long id, String title, String email) {
    public static NotificationDetailResponse of(PushNotification n, MailEvent e, User u) {
        return new NotificationDetailResponse(n.getId(), e.getTitle(), u.getEmail());
    }
}
```

### Request → Entity 변환

> **Request DTO에 `toEntity()` 작성 금지.** 변환 책임은 **CommandService**가 진다.

```java
// ✅ CommandService에서 Builder로 직접 생성
@Transactional
public void register(RegisterRequest request) {
    User user = User.builder()
        .email(request.email())
        .password(passwordEncoder.encode(request.password()))
        .build();
    userRepository.save(user);
}

// ❌ 금지
public record RegisterRequest(String email, String password) {
    public User toEntity() { ... }
}
```

---

## API Response Format

| 상황 | 반환 타입 |
|------|-----------|
| 명령 API, 응답 데이터 없음 | `ResponseEntity<Void>` |
| 명령 API, 응답 데이터 있음 | `ResponseEntity<XxxResponse>` |
| 단건 조회 | `ResponseEntity<XxxResponse>` |
| 불리언 확인 | `ResponseEntity<Boolean>` |
| 단순 문자열 반환 | `ResponseEntity<String>` |
| 무한 스크롤 페이지네이션 | `ResponseEntity<SliceResponse<XxxResponse>>` |
| 페이지 번호 페이지네이션 | `ResponseEntity<PageResponse<XxxResponse>>` |
| 소량 전체 목록 | `ResponseEntity<List<XxxResponse>>` |

---

## Exception Handling

모든 예외 클래스는 **`common/exception/{subdomain}/`** 에 위치한다.

```
common/exception/
├── BaseException.java
├── ErrorCode.java              # 인터페이스
├── ErrorResponse.java
├── GlobalExceptionHandler.java
├── auth/
│   ├── AuthErrorCode.java
│   └── AuthException.java
├── common/
│   ├── CommonErrorCode.java
│   └── CommonException.java
└── {subdomain}/
    ├── {Domain}ErrorCode.java
    └── {Domain}Exception.java
```

```java
// ErrorCode 인터페이스
public interface ErrorCode {
    int getStatus();
    String getCode();     // 형식: MS-{DOMAIN}-{ERROR-NAME}
    String getMessage();
}

// 도메인 ErrorCode
@Getter
@AllArgsConstructor
public enum UserErrorCode implements ErrorCode {
    USER_NOT_FOUND(404, "MS-USER-NOT-FOUND", "사용자를 찾을 수 없습니다.");

    private final int status;
    private final String code;
    private final String message;
}

// 도메인 Exception
public class UserException extends BaseException {
    public UserException(UserErrorCode errorCode) { super(errorCode); }
}
```

---

## @Transactional Rules

- **클래스 레벨 적용 금지** — 상태 변경 메서드에만 선언
- `@Transactional(readOnly = true)` 사용 금지
- 순수 조회 메서드에는 `@Transactional` 불필요
- **외부 I/O(이메일, 외부 API) 절대 트랜잭션 블록 내 포함 금지** — Facade에서 분리

```java
// ✅
@Transactional
public void register(RegisterRequest request) { ... }  // 쓰기 메서드에만

public Optional<User> findById(Long id) { ... }  // 조회는 @Transactional 없음

// ❌
@Transactional
@Service
public class UserCommandService { ... }  // 클래스 레벨 금지
```

---

## Lombok Rules

| 클래스 타입 | 어노테이션 |
|-------------|------------|
| Entity | `@Getter` `@NoArgsConstructor(access = PROTECTED)` `@Builder` `@AllArgsConstructor` |
| Service / Facade / Controller | `@RequiredArgsConstructor` |
| Config / Component | `@RequiredArgsConstructor` |
| DTO | Java `record` — Lombok 불필요 |

> `@Autowired` 필드 주입 금지 — `@RequiredArgsConstructor`로 생성자 주입 필수

---

## Properties

> **하드코딩 금지.** URL, 토큰 키, 사이즈 등은 반드시 `@ConfigurationProperties` 사용.
> 위치: `config/properties/{Domain}Properties.java`

```java
// ✅
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "mailsangja.mail")
public class MailProperties {
    private String apiKey;
    private String defaultSenderAddress;
}

// ❌
private static final String DEFAULT_SENDER = "noreply@mailsangja.com";
```

---

## Logging (@Slf4j)

> **커스텀 Logger 클래스 작성 금지.** `@Slf4j` (Logback) 사용.

| 레벨 | 사용 상황 |
|------|-----------|
| `log.debug()` | 개발 환경 디버깅 |
| `log.info()` | 정상 처리 흐름 |
| `log.warn()` | 주의 필요 상황 |
| `log.error()` | 예외 발생, 장애 |

---

## External API Integration Rules

외부 API를 호출하는 서비스는 반드시 `*ApiService`로 명명한다. 내부 비즈니스 서비스(`*CommandService`, `*QueryService`)와 명확히 구분한다.

- `*ApiService`는 외부 API를 호출하고 `*Response` → `*Result` 변환까지 담당한다
- 호출 측은 `*Result`만 받는다. 원본 `*Response`를 직접 다루지 않는다

```java
// ✅ ApiService — 호출 + 변환 캡슐화
public XxxResult fetchData(String token) {
    XxxResponse response = apiClient.fetch(token);
    return XxxResult.from(response);
}

// ✅ 호출 측은 Result만 사용
XxxResult result = xxxApiService.fetchData(token);

// ❌ 금지 — 호출 측이 Response를 직접 다룸
XxxResponse raw = xxxApiService.fetchRaw(token);
```

---

## OAuth 연동 규칙

서비스 자체 로그인용 OAuth와 외부 계정 연결용 OAuth를 혼동하지 않는다.

- 외부 계정 연결 OAuth는 로그인된 사용자가 자신의 외부 계정을 추가하는 시나리오로 설계한다
- OAuth 인가 시작 단계: Controller가 세션에 `state`와 시작 사용자 식별값을 저장한다
- OAuth callback 단계: Controller가 세션 `state`와 현재 사용자 식별값을 먼저 검증한 후 Facade를 호출한다
- Facade: Controller에서 내려온 입력을 검증하고, 외부 OAuth 응답을 `*Result`로 정리해 CommandService로 전달한다
- CommandService: 외부 OAuth `*Result`, `*Command`, 동일 사용자 중복 연결, 타 사용자 선점, 저장 결과를 검증한 뒤 저장한다

> core 모듈에서 Gmail OAuth 계정 연결 흐름을 구현한다. Gmail API 호출(Access Token 갱신, History/Message API)은 Worker 모듈이 담당하며, 관련 규칙은 `worker-conventions.md`를 참조한다.
