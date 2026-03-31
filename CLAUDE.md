## Project Overview
이 프로젝트는 다중 계정 메일 통합 관리 및 AI 기반 메일 작성/분류를 지원하는 "메일상자" 서비스의 백엔드 서버입니다.
Gmail, Naver Mail 등 외부 벤더 연동과 LLM API 기반의 RAG 처리가 핵심 기능입니다.
Gmail OAuth는 서비스 로그인 수단이 아니라, 로그인된 사용자의 외부 메일 계정을 연결하고 인박스 접근 권한을 확보하기 위한 별도 연동 흐름으로 다룹니다.

## Important Note for AI Agent
AI 에이전트는 사용자가 명시적으로 지시하지 않은 코드 변경이나 리팩토링을 선제적으로 수행해서는 안 됩니다.
항상 `.claude/skills/product-requirements.md`를 참조하여 현재 개발 중인 기능이 어떤 Epic과 User Story에 해당하는지 맥락을 파악한 후 코드를 작성하십시오.

## General Rules
- 파일 삭제나 데이터베이스 스키마 변경 시 반드시 개발자의 승인을 먼저 구하십시오.
- 환경 변수나 API Key 등 민감 정보는 절대 하드코딩하지 마십시오.
- Do not touch `git push`
- Do not touch `git commit`


## Architecture

멀티 모듈 Gradle 프로젝트. 각 모듈은 독립적으로 빌드되며, 루트에는 `settings.gradle`만 존재합니다.

```
mailsangja_server/
├── settings.gradle     # 모듈 목록 선언 (include)
├── db/                 # 공유 라이브러리 모듈 — JPA Entity, Repository
└── {feature}/          # 실행 모듈 — db 모듈을 의존성으로 참조하여 실행
```

**모듈 역할:**
- **`db`**: Entity, Repository 등 DB 레이어 정의. 단독 실행하지 않고 다른 모듈에 참조됨
- **실행 모듈** (예: `core`): Spring Boot 애플리케이션. `db` 모듈을 `implementation project(':db')`로 참조

**각 모듈 구조:**
```
{module}/
├── gradle/wrapper/
├── gradlew
├── build.gradle    # 해당 모듈의 완전한 독립 빌드 설정
├── settings.gradle
└── src/
```

**Tech stack**: Spring Boot 4.0.5, Java 21, Gradle 9.4.1, Spring MVC, Spring Data JPA, MySQL, Lombok

**빌드 명령 (각 모듈 디렉토리 내에서 실행):**
```bash
./gradlew build
./gradlew bootRun   # 실행 모듈만 해당
./gradlew test --tests "패키지.클래스명"
```

새 모듈 추가 시 루트 `settings.gradle`에 `include '모듈명'`을 추가합니다.
