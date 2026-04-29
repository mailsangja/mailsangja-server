## Project Overview
이 프로젝트는 다중 계정 메일 통합 관리 및 AI 기반 메일 작성/분류를 지원하는 "메일상자" 서비스의 백엔드 서버입니다.
Gmail, Naver Mail 등 외부 벤더 연동과 LLM API 기반의 RAG 처리가 핵심 기능입니다.
Gmail OAuth는 서비스 로그인 수단이 아니라, 로그인된 사용자의 외부 메일 계정을 연결하고 인박스 접근 권한을 확보하기 위한 별도 연동 흐름으로 다룹니다.

## 개발 규칙 파일 위치

| 대상 | 규칙 파일 |
|------|-----------|
| 공통 규칙 (Layer Dependency, Service 분리, DTO, Exception, Transactional 등) | `.claude/skills/spring-api-rules.md` |
| **db 모듈** (Entity, Port/Adapter/JpaModule 패턴) | `.claude/skills/db-conventions.md` |
| **core 모듈** (Controller, 인증, OAuth 연동, Redis) | `.claude/skills/core-conventions.md` |
| **worker 모듈** (Listener, Handler, Publisher, 큐 아키텍처) | `.claude/skills/worker-conventions.md` |
| **RabbitMQ 새 큐 등록 절차** (Properties, RabbitConfig, Listener 연결) | `.claude/skills/rabbitmq-queue-registration.md` |
| **새 모듈 추가 절차** (Gradle 설정, Application 클래스, 패키지 구조) | `.claude/skills/new-module-guide.md` |
| 기획서 (Epic, User Story) | `.claude/skills/product-requirements.md` |

각 모듈 작업 시 해당 모듈 규칙 파일을 참조하고, 공통 패턴은 `spring-api-rules.md`를 함께 확인하십시오.

## General Rules
- AI 에이전트는 사용자가 명시적으로 지시하지 않은 코드 변경이나 리팩토링을 선제적으로 수행해서는 안 됩니다.
- 파일 삭제나 데이터베이스 스키마 변경 시 반드시 개발자의 승인을 먼저 구하십시오.
- 환경 변수나 API Key 등 민감 정보는 절대 하드코딩하지 마십시오.
- Do not touch `git push`
- Do not touch `git commit`


## Architecture

**Tech stack**: Spring Boot 4.0.5, Java 21, Gradle 9.4.1, Spring MVC, Spring Data JPA, PostgreSQL, RabbitMQ, Lombok

**모듈 목록:**

| 모듈 | 역할 |
|------|------|
| `db` | JPA Entity, Repository 정의. 단독 실행 불가 — 다른 모듈에 라이브러리로 참조됨 |
| `core` | HTTP API 서버. 사용자 인증, Gmail OAuth 연동, Pub/Sub 수신, MQ 발행 담당 |
| `worker` | MQ Consumer. Gmail 동기화, Watch 갱신 등 비동기 후속 처리 담당 |

각 실행 모듈(`core`, `worker`)은 `db` 모듈을 `implementation project(':db')`로 참조한다.

**빌드 명령 (각 모듈 디렉토리 내에서 실행):**
```bash
./gradlew build
./gradlew bootRun
./gradlew test --tests "패키지.클래스명"
```
