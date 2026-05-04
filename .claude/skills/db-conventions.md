---
name: db-conventions
description: db 모듈(JPA Entity, Repository Port/Adapter/JpaModule) 개발 규칙입니다. Entity 작성, Repository 패턴 작성 시 이 규칙을 따릅니다.
allowed-tools: Read, Write, Edit, Glob
---

# DB Module Conventions

`spring-api-rules.md`의 공통 규칙을 기반으로 하며, `db` 모듈에서 추가로 적용하는 규칙을 정의합니다.

- Root Package: `com.mailsangja.db`
- 역할: JPA Entity · Repository 정의. 단독 실행 불가 — `core`, `worker` 등 실행 모듈에 라이브러리로 참조됨

새 실행 모듈에서 `db` 모듈을 통합하는 Gradle/Application 설정 절차는 `.claude/skills/new-module-guide.md`를 참조하십시오.

---

## db 모듈 패키지 구조

```
com.mailsangja.db
├── common/{domain}/
│   └── {DomainValueObject}.java         # 도메인 값 객체 (record/enum). Entity·Port 양쪽에서 참조
├── entity/
│   ├── common/
│   │   └── BaseEntity.java              # 공통 시간 필드 + Soft Delete
│   └── {domain}/
│       ├── {Domain}.java                # JPA Entity
│       └── {EnumName}.java              # 도메인 Enum (같은 패키지)
├── port/
│   ├── {Domain}RepositoryPort.java      # 순수 Java 인터페이스
│   └── {Domain}View.java                # Port 반환 타입 record (Service가 소비하는 공개 계약)
├── adapter/{domain}/
│   └── {Domain}RepositoryAdapter.java   # Port 구현체 (@Repository)
└── module/{domain}/
    ├── {Domain}JpaRepositoryModule.java  # extends JpaRepository
    └── {Domain}Projection.java          # JPA Projection 인터페이스 (JpaModule 전용 구현 세부사항)
```

---

## Port / Adapter / JpaModule 패턴

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

---

## JPA Projection

Spring Data JPA Projection 인터페이스는 `module/{domain}/` 패키지에 위치합니다.

```java
// module/label/LabelUnreadCountProjection.java
public interface LabelUnreadCountProjection {
    UUID getLabelId();
    Long getUnreadCount();
}

// module/label/LabelJpaRepositoryModule.java 에서 사용
List<LabelUnreadCountProjection> findUnreadThreadCountsByUserId(@Param("userId") UUID userId);
```

- JPA 쿼리 결과 매핑을 위한 구현 세부사항이므로 `JpaRepositoryModule`과 같은 패키지에 배치
- `Port`나 `Adapter` 외부로 노출하지 않는다
- `Adapter`가 Projection → Port 반환 타입(record 또는 primitive)으로 변환한다

---

## Port View (반환 타입 record)

Port 메서드의 반환 타입으로 사용되는 record는 `port/` 패키지에 위치합니다.

```java
// port/ThreadLabelView.java — MessageRepositoryPort.findLabelsByThreadIdIn()의 반환 타입
public record ThreadLabelView(UUID threadId, UUID labelId, String labelName, String colorCode) {}
```

- Service 레이어(core)가 직접 소비하는 **Port 공개 계약**의 일부이므로 `port/`에 위치
- Adapter 내부에서 JPA Projection → View record 변환을 수행한다
- JPA Projection이 `module/`에 숨겨지고, Service는 View record만 알면 된다

---

## common 패키지

`common/{domain}/`은 Entity와 Port 양쪽에서 참조하는 도메인 값 객체(record, enum)를 담습니다.

```java
// common/label/LabelRule.java — Label Entity의 JSONB 컬럼 타입이자 Port 파라미터로도 사용
public record LabelRule(List<Group> groups) { ... }
```

- Entity 필드 타입이면서 동시에 core 모듈 서비스에서도 다뤄야 하는 객체를 배치한다
- 단순 JPA 구현 세부사항은 `module/`에, Port 계약 타입은 `port/`에 두고, 양쪽 공통 참조가 필요한 경우만 `common/`을 사용한다

---

## Entity

모든 Entity는 `db` 모듈(`com.mailsangja.db.entity`)에 위치하며 `BaseEntity`를 상속합니다.

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

---

## BaseEntity

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
- `@EnableJpaAuditing`은 실행 모듈의 `@SpringBootApplication` 클래스에 선언 (`.claude/skills/new-module-guide.md` 참조)
