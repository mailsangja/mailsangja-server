---
name: spring-api-rules
description: Spring Boot 기반 REST API 개발 규칙입니다. API 설계, 도메인 분리, DTO 작성 시 이 규칙을 따릅니다.
allowed-tools: Read, Write, Edit, Glob
---

# Spring API Development Rules

Standard rules for Spring Boot REST API development in this project.

- Root Package: `com.mailsangja.{module}`
- Java 21 / Spring Boot 4.0.5 / MySQL

---

## Multi-Module Architecture

### 프로젝트 모듈 구조

```
mailsangja_server/
├── settings.gradle          # 루트: 모듈 목록 선언
├── db/                      # 공유 인프라 라이브러리 (단독 실행 불가)
└── {feature}/               # 실행 모듈 (Spring Boot App)
```

### db 모듈 패키지 구조

```
com.mailsangja.db
├── entity/
│   ├── common/
│   │   └── BaseEntity.java              # 공통 시간 필드 + Soft Delete
│   └── {domain}/
│       ├── {Domain}.java                # JPA Entity
│       └── {EnumName}.java              # 도메인 Enum (같은 패키지)
├── port/
│   └── {Domain}RepositoryPort.java      # 순수 Java 인터페이스
├── adapter/{domain}/
│   └── {Domain}RepositoryAdapter.java   # Port 구현체 (@Repository)
└── module/{domain}/
    └── {Domain}JpaRepositoryModule.java  # extends JpaRepository
```

### Port / Adapter / JpaModule 패턴

```java
// Port — 순수 Java 인터페이스 (db 모듈: port/)
public interface UserRepositoryPort {
    User save(User user);
    Optional<User> findById(UUID id);
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
}

// JpaModule — JPA 저장소 (db 모듈: module/{domain}/)
public interface UserJpaRepositoryModule extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
}

// Adapter — Port 구현체 (db 모듈: adapter/{domain}/)
@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepositoryPort {
    private final UserJpaRepositoryModule userJpaRepositoryModule;

    @Override
    public User save(User user) {
        return userJpaRepositoryModule.save(user);
    }
}
```

**규칙:**
- Service 레이어는 반드시 **Port 인터페이스**만 주입받음 — `JpaRepositoryModule` 직접 주입 금지
- `JpaRepositoryModule`은 `Adapter` 내부에서만 사용

### 실행 모듈에서 db 모듈 통합

실행 모듈(`core` 등)이 `db` 모듈을 참조하려면 아래 세 가지 설정이 필요합니다.

**1. settings.gradle — db 모듈 경로 포함**

```gradle
rootProject.name = 'core'
include 'db'
project(':db').projectDir = new File('../db')
```

**2. build.gradle — 의존성 추가**

```gradle
dependencies {
    implementation project(':db')
}
```

**3. @SpringBootApplication — 컴포넌트 스캔 범위 확장**

```java
@EnableJpaAuditing
@EntityScan(basePackages = "com.mailsangja.db")
@EnableJpaRepositories(basePackages = "com.mailsangja.db")
@SpringBootApplication(scanBasePackages = {"com.mailsangja.{module}", "com.mailsangja.db"})
public class {Module}Application {
    public static void main(String[] args) {
        SpringApplication.run({Module}Application.class, args);
    }
}
```

| 어노테이션 | 목적 |
|-----------|------|
| `@EnableJpaAuditing` | BaseEntity의 `@CreatedDate`, `@LastModifiedDate` 활성화 |
| `@EntityScan` | db 모듈 Entity 클래스 인식 |
| `@EnableJpaRepositories` | db 모듈 JpaRepository 빈 등록 |
| `scanBasePackages` | db 모듈 `@Repository` Adapter 빈 등록 포함 |

---

## Package Structure

```
com.mailsangja.{module}
├── controller/
│   ├── {Domain}Controller.java
│   └── docs/                         # Swagger interface
├── facade/{domain}/
│   └── {Domain}Facade.java
├── service/{domain}/
│   ├── {Domain}CommandService.java   # 쓰기 작업
│   └── {Domain}QueryService.java     # 읽기 작업
├── common/
│   ├── dto/        # SliceResponse, PageResponse, ResponseDto
│   ├── exception/
│   └── util/
├── config/
│   ├── SecurityConfig.java
│   ├── AsyncConfig.java
│   └── properties/
├── dto/{domain}/
│   ├── *Result.java  # Service ↔ Facade 내부 전달 응답 DTO
│   ├── *Command.java  # Service ↔ Facade 내부 전달 요청 DTO
│   ├── *Request.java  # Controller ↔ Facade
│   ├── *Response.java # Controller ↔ Facade
│   └── properties/
└── {domain}/
    ├── exception/
    └── config/     # 도메인 전용 Properties
```

---

## Layer Dependency (단방향 엄수)

```
Controller → Facade → CommandService / QueryService → Repository
```

| 규칙 | 내용 |
|------|------|
| Controller | Facade만 호출 — Service 직접 호출 금지 |
| Facade만 | 같은 도메인 Command/QueryService 호출. 타 도메인은 Facade 레벨에서만 주입 |
| Facade → Facade | 금지 |
| Service → 타 도메인 Service | 금지 — 반드시 팀 회의 후 결정 |

---

## Command / Query Service Split

```java
// CommandService — 쓰기 (INSERT / UPDATE / DELETE)
@Service
@RequiredArgsConstructor
public class UserCommandService {
    // register(), update(), delete() 등 상태 변경 메서드
    // @Transactional은 상태 변경이 있는 메서드에만 선언
}

// QueryService — 읽기 (SELECT)
@Service
@RequiredArgsConstructor
public class UserQueryService {
    // findById(), getList() 등 조회 전용 메서드 (@Transactional 불필요)
}
```

메서드가 2개 이하이고 모두 같은 성격이면 단일 `{Domain}Service`로 유지 가능.

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

## 인증 파라미터 애노테이션

컨트롤러에서 로그인 사용자를 `Principal`로 받지 않고, `@AuthUser` / `@AuthAdmin` 애노테이션으로 `User` 엔티티를 바로 주입받는다.
내부적으로 `AuthArgumentResolver`가 `SecurityContextHolder`에서 이메일을 꺼내 DB 조회 후 반환한다.
`AuthArgumentResolver`는 `SecurityConfig.addArgumentResolvers()`에 등록되어 있다.

| 애노테이션 | 파일 | 비인증(Anonymous) | 비관리자 | 반환 |
|-----------|------|------------------|---------|------|
| `@AuthUser` | `config/auth/AuthUser.java` | `401` throw | — | `User` |
| `@AuthAdmin` | `config/auth/AuthAdmin.java` | `401` throw | `403` throw | `User` |

### 사용 기준

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

## DTO

> **모든 DTO는 Java `record`로 작성. `@Data`/`@Getter` class 사용 금지.**

### 네이밍

| 접미사 | 기준 | 예시 |
|--------|------|------|
| `*Request` | Controller 메서드 파라미터로 직접 사용 (HTTP 요청 입력) | `OauthRequest`, `UserUpdateRequest` |
| `*Response` | Controller 메서드 반환 타입으로 직접 사용 (HTTP 응답 출력) | `LoginResponse`, `UserInfoResponse` |
| `*Result` | Controller 메서드 시그니처에 등장하지 않는 내부 결과 전달 | `UserLoginResult`, `AuthTokenResult`, `GoogleUserInfoResult` |
| `*Command` | Controller 메서드 시그니처에 등장하지 않는 내부 명령 전달 | `SendNotificationCommand` |

- **판단 기준: Controller 메서드의 파라미터/반환 타입에 직접 등장하면 `*Request` / `*Response`, 그 외 레이어 간 내부 전달이면 `*Result` / `*Command`**
- Controller 내부 지역 변수로만 사용하더라도 HTTP I/O 목적이 아니면 `*Result` / `*Command`
- `*Dto` 접미사 사용 금지

### Presentation DTO 조립 책임

> **Service는 `*Response` DTO를 생성하거나 반환하지 않는다. `*Response` 조립은 반드시 Facade에서 수행한다.**

| 레이어 | 책임 |
|--------|------|
| Service | 비즈니스 로직 수행, 도메인 객체(Entity / `*Result`) 반환 |
| Facade | Service 결과를 받아 `*Response` DTO로 조립 후 Controller에 전달 |

이유:
1. **계층 간 책임 분리** — Service는 비즈니스 로직, Facade가 DTO 조립
2. **의존성 방향 유지** — 하위 계층(Service)이 상위 계층의 API 응답 포맷(`*Response`)을 알아서는 안 됨
3. **비즈니스 로직 재사용** — 동일한 Service를 여러 API(다른 응답 포맷)에서 재사용 가능

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
    public UserInfoResponse getUserInfo(User user) {
        return new UserInfoResponse(user.getName(), user.getEmail());
    }
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
        .name(request.name())
        .build();
    userRepository.save(user);
}

// ❌ 금지
public record RegisterRequest(String email, String password, String name) {
    public User toEntity() { ... }  // DTO가 Entity를 알아서는 안 됨
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

## Entity

모든 Entity는 `db` 모듈(`com.mailsangja.db.entity`) 에 위치하며 `BaseEntity`를 상속합니다.

```java
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    public void updateName(String name) { this.name = name; }  // Setter 금지
}
```

- `@NoArgsConstructor(access = PROTECTED)` 필수
- **ID 타입: `UUID`, 전략: `GenerationType.UUID`** — `Long` + `IDENTITY` 사용 금지
- **Setter 전면 금지** — 상태 변경은 명시적 메서드

### BaseEntity

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class BaseEntity {

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime modifiedAt;

    private LocalDateTime deletedAt;

    public void delete() { this.deletedAt = LocalDateTime.now(); }
    public boolean isDeleted() { return this.deletedAt != null; }
    public void restore() { this.deletedAt = null; }
}
```

- **Soft Delete**: 물리 삭제(`DELETE` SQL) 금지 — `delete()` 메서드로 `deletedAt` 설정
- `@EnableJpaAuditing`은 실행 모듈의 `@SpringBootApplication` 클래스에 선언

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

## Exception Handling

모든 예외 클래스는 **`common/exception/{subdomain}/`** 에 위치한다.

```
common/exception/
├── BaseException.java          # 추상 베이스
├── ErrorCode.java              # 인터페이스
├── ErrorResponse.java
├── GlobalExceptionHandler.java
├── auth/                       # 인증 예외
│   ├── AuthErrorCode.java
│   └── AuthException.java
├── common/                     # 도메인 없는 인프라/공통 예외
│   ├── CommonErrorCode.java
│   └── CommonException.java
└── {subdomain}/                # 도메인별 예외 추가 시 여기에
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

## @Async Rules

- `@Async` 사용 가능 위치: **`PushFacade`만 허용**
- `@EnableAsync`는 `config/AsyncConfig.java`에서 활성화

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
