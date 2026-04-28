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
