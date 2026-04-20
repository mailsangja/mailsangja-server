---
name: facade-service-test-conventions
description: Facade 및 Service 계층 테스트 코드 작성, 실행, 커버리지 확인 절차를 정의한 규칙 문서입니다. 백엔드 구현 완료 후 반드시 다시 읽고 테스트를 작성/보강합니다.
allowed-tools: Read, Write, Edit, Glob, Grep, Bash
---

# Facade / Service Test Conventions

이 문서는 `facade` 및 `service` 계층 테스트 코드의 작성 기준과, 구현 완료 후 반드시 수행해야 하는 테스트 검증 절차를 정의합니다.

적용 대상:
- `core/src/main/java/com/mailsangja/core/facade/**`
- `core/src/main/java/com/mailsangja/core/service/**`
- `worker/src/main/java/com/mailsangja/worker/facade/**`
- `worker/src/main/java/com/mailsangja/worker/service/**`

## 1. 기본 원칙

- 구현을 마친 뒤에는 반드시 이 문서를 다시 읽고 테스트 누락 여부를 점검합니다.
- `facade` 또는 `service`를 수정했다면 대응하는 테스트를 반드시 작성하거나 보강합니다.
- 테스트를 작성한 뒤에는 반드시 실제 테스트를 실행합니다.
- 테스트가 통과해도 종료하지 말고, 커버리지 리포트를 생성해 `facade`/`service` 패키지 기준 70% 이상인지 확인합니다.
- 커버리지가 70% 미만이면 누락된 분기, 예외, 경계값 테스트를 추가 작성한 뒤 다시 실행합니다.

## 2. 테스트 클래스 / 메서드 규칙

- 테스트 클래스명은 `{TargetClass}Test` 형식을 사용합니다.
- 테스트 파일 경로는 운영 코드 패키지 구조를 최대한 그대로 따릅니다.
- 테스트 메서드명은 한글 기반의 `상황_행동_결과` 형식을 사용합니다.
- 하나의 테스트는 하나의 행위 또는 하나의 검증 의도만 드러내도록 유지합니다.
- 공통 준비 코드가 길어지면 private fixture 메서드로 정리하되, 과도한 추상화는 피합니다.

예시:

```java
class MailAccountFacadeTest {

    @Test
    void connectGoogleAccount_중복연결계정이면예외를반환한다() {
    }
}
```

## 3. Facade 테스트 규칙

`Facade` 테스트는 아래 책임을 우선 검증합니다.

- Controller 등 상위 계층에서 내려온 입력 검증
- 유스케이스 orchestration
- 내부 `CommandService` / `QueryService` 호출 조합
- 응답 DTO 조립
- 예외 전파 및 변환

반드시 포함할 후보:
- 정상 흐름 1건 이상
- 잘못된 입력값에 대한 검증 실패
- 권한/소유권/상태 불일치
- 조합 응답이 있는 경우 필수 필드 매핑 검증

지양할 것:
- 내부 구현 세부 호출 순서만 과도하게 검증하는 테스트
- DTO getter 수준의 저가치 검증

## 4. Service 테스트 규칙

`Service` 테스트는 아래 책임을 우선 검증합니다.

- Repository Port 조회 결과 검증
- 상태 변경 메서드 호출 후 Entity 상태 변화
- 저장/수정/삭제 흐름
- 외부 연동 결과 처리
- 도메인 예외 및 ErrorCode 검증

반드시 포함할 후보:
- 정상 흐름 1건 이상
- 조회 결과 없음 또는 잘못된 상태
- 외부 API 실패 또는 포트 반환값 이상 상태
- 경계값 입력
- 동시성 보호 또는 조건부 업데이트 로직이 있으면 경쟁 상황 케이스

지양할 것:
- Mocking만으로 구현 세부사항을 과도하게 고정하는 테스트
- 포트/클라이언트 계약과 무관한 private 구현 디테일 검증

## 5. Mock / Fixture 원칙

- Mock은 Repository Port, 외부 API Client, 메시징 Port 등 경계에만 사용합니다.
- 같은 도메인 내 단순 값 객체나 record는 실제 객체를 우선 사용합니다.
- Fixture는 테스트 의도가 흐려지지 않는 선에서만 재사용합니다.
- 불필요한 stubbing은 만들지 않습니다.
- 예외 테스트에서는 가능하면 `ErrorCode`까지 함께 검증합니다.

## 6. 작업 완료 후 필수 절차

구현을 마친 뒤에는 아래 순서를 반드시 지킵니다.

1. 이 문서를 다시 읽고 변경한 `facade`/`service`의 테스트 누락 여부를 확인합니다.
2. 대응 테스트 코드를 작성하거나 부족한 케이스를 보강합니다.
3. 관련 모듈 디렉토리에서 테스트를 실제 실행합니다.
4. 관련 모듈 디렉토리에서 JaCoCo 리포트와 커버리지 검증을 실행합니다.
5. `facade`/`service` 패키지 기준 라인 커버리지가 70% 미만이면 테스트를 추가 작성합니다.
6. 테스트와 커버리지 검증을 다시 실행합니다.

## 7. 실행 명령

`core` 모듈:

```bash
./gradlew test
./gradlew jacocoTestReport jacocoTestCoverageVerification
```

`worker` 모듈:

```bash
./gradlew test
./gradlew jacocoTestReport jacocoTestCoverageVerification
```

필요 시 특정 테스트만 먼저 확인할 수 있지만, 작업 마무리 단계에서는 반드시 모듈 단위 테스트와 커버리지 검증을 다시 수행합니다.

## 8. 커버리지 기준

- 기준 값은 `facade`/`service` 패키지 대상 라인 커버리지 70%입니다.
- 저장소 전체 커버리지가 아니라 작업 대상 모듈의 `facade`/`service` 패키지를 기준으로 판단합니다.
- 70%를 넘겼더라도 핵심 실패 흐름이 빠져 있다면 테스트를 보강합니다.
- 70%에 미달하면 작업 완료로 간주하지 않습니다.

## 9. 리뷰 체크포인트

리뷰 시 아래 항목을 확인합니다.

- 변경한 `facade`/`service`에 대응하는 테스트가 존재하는가
- 정상 흐름과 실패 흐름이 모두 검증되는가
- 예외 코드와 주요 상태 변화를 검증하는가
- 테스트를 실제 실행했는가
- JaCoCo 리포트와 커버리지 기준을 확인했는가
