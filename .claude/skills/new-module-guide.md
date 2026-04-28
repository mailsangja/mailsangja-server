---
name: new-module-guide
description: 새 Spring Boot 실행 모듈을 추가하는 절차입니다. Gradle 설정, db 모듈 통합, Application 클래스 작성, 표준 패키지 구조 생성 시 이 가이드를 따릅니다.
allowed-tools: Read, Write, Edit, Glob, Bash
---

# New Module Setup Guide

새 실행 모듈(`core`, `worker` 외에 추가 모듈이 필요한 경우)을 생성할 때 아래 절차를 순서대로 따릅니다.

---

## 프로젝트 모듈 구조

```
mailsangja_server/
├── settings.gradle          # 루트: 모듈 목록 선언
├── build.gradle             # 루트: 공통 의존성 (선택)
├── db/                      # 공유 인프라 라이브러리 (단독 실행 불가)
├── core/                    # HTTP API 실행 모듈
├── worker/                  # MQ Consumer 실행 모듈
└── {new-module}/            # 새 실행 모듈
    ├── build.gradle
    ├── settings.gradle
    └── src/main/java/com/mailsangja/{newModule}/
```

---

## Step 1. 루트 settings.gradle에 모듈 추가

루트 `settings.gradle`에 새 모듈을 선언합니다.

```gradle
// mailsangja_server/settings.gradle
rootProject.name = 'mailsangja_server'
include 'db', 'core', 'worker', '{new-module}'
```

---

## Step 2. 모듈 디렉토리 생성

```
{new-module}/
├── settings.gradle
├── build.gradle
└── src/
    ├── main/
    │   ├── java/com/mailsangja/{newModule}/
    │   └── resources/
    │       ├── application.yaml
    │       └── application-mq.yaml   # MQ 사용 시
    └── test/
        └── java/com/mailsangja/{newModule}/
```

---

## Step 3. 모듈 settings.gradle 작성

```gradle
// {new-module}/settings.gradle
rootProject.name = '{new-module}'
include 'db'
project(':db').projectDir = new File('../db')
```

---

## Step 4. 모듈 build.gradle 작성

```gradle
// {new-module}/build.gradle
plugins {
    id 'org.springframework.boot' version '4.0.5'
    id 'io.spring.dependency-management' version '1.1.7'
    id 'java'
}

group = 'com.mailsangja'
version = '0.0.1-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation project(':db')

    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-security'

    // 필요한 의존성 추가
    runtimeOnly 'org.postgresql:postgresql'

    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

---

## Step 5. Application 클래스 작성

```java
@EnableJpaAuditing
@EntityScan(basePackages = "com.mailsangja.db")
@EnableJpaRepositories(basePackages = "com.mailsangja.db")
@SpringBootApplication(scanBasePackages = {"com.mailsangja.{newModule}", "com.mailsangja.db"})
public class {NewModule}Application {
    public static void main(String[] args) {
        SpringApplication.run({NewModule}Application.class, args);
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

## Step 6. 표준 패키지 구조 생성

모듈의 역할에 따라 패키지 구조를 선택한다.

### HTTP API 모듈인 경우 (core 패턴)

```
com.mailsangja.{newModule}
├── controller/
├── facade/{domain}/
├── service/{domain}/
├── common/exception/
├── config/
│   ├── SecurityConfig.java
│   └── properties/
└── dto/{domain}/
```

자세한 규칙: `.claude/skills/core-conventions.md`

### MQ Consumer 모듈인 경우 (worker 패턴)

```
com.mailsangja.{newModule}
├── messaging/listener/
├── messaging/publisher/
├── handler/{domain}/
├── service/{domain}/
├── scheduler/
├── config/rabbitmq/
└── dto/{domain}/
```

자세한 규칙: `.claude/skills/worker-conventions.md`

---

## Step 7. application.yaml 기본 설정

```yaml
spring:
  application:
    name: {new-module}
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

server:
  port: ${SERVER_PORT:8080}
```

---

## 참고 파일

| 목적 | 파일 |
|------|------|
| db 모듈 Port/Adapter/Entity 규칙 | `.claude/skills/db-conventions.md` |
| HTTP API 개발 규칙 | `.claude/skills/core-conventions.md` |
| Worker / RabbitMQ 개발 규칙 | `.claude/skills/worker-conventions.md` |
| 공통 레이어 규칙 | `.claude/skills/spring-api-rules.md` |
