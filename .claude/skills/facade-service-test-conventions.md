---
name: facade-service-test-conventions
description: Facade 및 Service 계층 테스트 코드 작성, 실행, 커버리지 확인 절차를 정의한 규칙 문서입니다. 백엔드 구현 완료 후 반드시 다시 읽고 테스트를 작성/보강합니다.
allowed-tools: Read, Write, Edit, Glob, Grep, Bash
---

# Facade / Service Test Conventions

이 문서는 `facade` 및 `service` 계층 테스트 코드의 작성 기준과, 구현 완료 후 반드시 수행해야 하는 테스트 검증 절차를 정의합니다.
테스트는 BDD 스타일을 명시적으로 따르며, IDE와 리포트에서 시나리오를 즉시 식별할 수 있도록 `@DisplayName`을 함께 사용합니다.

적용 대상:
- `core/src/main/java/com/mailsangja/core/facade/**`
- `core/src/main/java/com/mailsangja/core/service/**`
- `worker/src/main/java/com/mailsangja/worker/facade/**`
- `worker/src/main/java/com/mailsangja/worker/service/**`

## 1. 기본 원칙

- 구현을 마친 뒤에는 반드시 이 문서를 다시 읽고 테스트 누락 여부를 점검합니다.
- `facade` 또는 `service`를 수정했다면 대응하는 테스트를 반드시 작성하거나 보강합니다.
- 새로 작성하거나 수정하는 `facade`/`service` 테스트는 BDD 스타일로 작성합니다.
- 모든 테스트 메서드에는 `@DisplayName`을 작성합니다.
- 테스트를 작성한 뒤에는 반드시 실제 테스트를 실행합니다.
- 테스트가 통과해도 종료하지 말고, 커버리지 리포트를 생성해 `facade`/`service` 패키지 기준 브랜치 커버리지 60% 이상인지 확인합니다.
- 브랜치 커버리지가 60% 미만이면 누락된 분기, 예외, 경계값 테스트를 추가 작성한 뒤 다시 실행합니다.

## 2. 테스트 클래스 / 메서드 규칙

- 테스트 클래스명은 `{TargetClass}Test` 형식을 사용합니다.
- 테스트 파일 경로는 운영 코드 패키지 구조를 최대한 그대로 따릅니다.
- 테스트 메서드명은 한글 기반의 `상황_행동_결과` 형식을 사용합니다.
- 테스트 메서드마다 `@DisplayName`을 사용해 자연어 문장으로 시나리오를 설명합니다.
- 테스트 클래스에도 가능하면 `@DisplayName`을 작성해 대상 책임을 드러냅니다.
- 시나리오가 많은 경우 공개 메서드나 조건 그룹 기준으로 `@Nested` 클래스로 묶고, `@Nested` 클래스에도 `@DisplayName`을 작성합니다.
- 하나의 테스트는 하나의 행위 또는 하나의 검증 의도만 드러내도록 유지합니다.
- 공통 준비 코드가 길어지면 private fixture 메서드로 정리하되, 과도한 추상화는 피합니다.

예시:

```java
@DisplayName("MailAccountFacade 테스트")
class MailAccountFacadeTest {

    @Nested
    @DisplayName("connectGoogleAccount")
    class ConnectGoogleAccount {

        @Test
        @DisplayName("중복 연결된 계정이면 예외를 반환한다")
        void connectGoogleAccount_중복연결계정이면예외를반환한다() {
        }
    }
}
```

## 3. BDD 작성 규칙

- 테스트 본문은 `given / when / then` 흐름이 한눈에 보이도록 작성합니다.
- BDDMockito 문법을 기본으로 사용합니다.
- stubbing은 `given(...).willReturn(...)`, `given(...).willThrow(...)`, `given(...).willAnswer(...)`를 사용합니다.
- 상호작용 검증은 `then(mock).should()` 계열을 사용합니다.
- `when(...).thenReturn(...)`, `verify(...)`는 새로 작성하거나 수정하는 테스트에서는 사용하지 않습니다.
- `given`에는 입력값, mock 응답, fixture 준비만 둡니다.
- `when`에는 테스트 대상 public 메서드 호출 1회만 둡니다.
- `then`에는 반환값, 예외, 상태 변화, 저장/발행 여부 같은 핵심 결과만 검증합니다.
- 하나의 테스트에는 `when`이 하나만 있어야 하며, 성공과 실패를 한 테스트에 함께 넣지 않습니다.
- 핵심 `when`과 `then`은 테스트 메서드 본문에 남겨, helper 메서드가 흐름을 숨기지 않게 합니다.

예시:

```java
@Test
@DisplayName("액세스 토큰이 충분히 남아 있으면 기존 계정을 그대로 반환한다")
void ensureValidGoogleAccessToken_만료가충분히남아있으면기존토큰을사용한다() {
    // given
    MailAccount mailAccount = createMailAccount(...);
    given(mailAccountRepositoryPort.findByIdAndActiveAndDeletedAtIsNull(mailAccount.getId(), true))
            .willReturn(Optional.of(mailAccount));

    // when
    MailAccount result = service.ensureValidGoogleAccessToken(mailAccount);

    // then
    assertThat(result).isSameAs(mailAccount);
    then(mailAccountRepositoryPort).should(never()).updateGoogleTokenIfAccessTokenMatches(any(), any(), any(), any(), any());
}
```

## 4. Facade 테스트 규칙

`Facade` 테스트는 아래 책임을 우선 검증합니다.

- Controller 등 상위 계층에서 내려온 입력 검증
- 유스케이스 orchestration
- 내부 `CommandService` / `QueryService` 호출 조합
- 응답 DTO 조립
- 예외 전파 및 변환

작성 방식:
- `given`: 사용자, 요청 객체, 소유 계정, collaborator 응답을 준비합니다.
- `when`: facade public 메서드를 1회 호출합니다.
- `then`: 응답 DTO의 핵심 필드, 예외, 권한/입력 검증 결과를 확인합니다.
- facade 테스트는 호출 순서보다 최종 정책 결과를 우선 검증합니다.
- 메시지 발행, 저장 위임, 외부 연동 위임처럼 경계 계약이 중요한 경우에만 `then(...).should()`로 상호작용을 검증합니다.

반드시 포함할 후보:
- 정상 흐름 1건 이상
- 잘못된 입력값에 대한 검증 실패
- 권한/소유권/상태 불일치
- 조합 응답이 있는 경우 필수 필드 매핑 검증

지양할 것:
- 내부 구현 세부 호출 순서만 과도하게 검증하는 테스트
- DTO getter 수준의 저가치 검증

## 5. Service 테스트 규칙

`Service` 테스트는 아래 책임을 우선 검증합니다.

- Repository Port 조회 결과 검증
- 상태 변경 메서드 호출 후 Entity 상태 변화
- 저장/수정/삭제 흐름
- 외부 연동 결과 처리
- 도메인 예외 및 ErrorCode 검증

작성 방식:
- `given`: Repository Port, 외부 API Client, lock/messaging Port 응답을 준비합니다.
- `when`: service public 메서드를 1회 호출합니다.
- `then`: 반환값, Entity 상태 변화, 저장 여부, 예외 코드, 조건부 업데이트 결과를 검증합니다.
- QueryService는 조회 결과와 예외를 우선 검증합니다.
- CommandService는 상태 변화와 저장 경계를 우선 검증합니다.
- 동시성 보호나 조건부 업데이트 로직은 성공 케이스와 실패 시 기존 상태 유지 케이스를 분리해 작성합니다.

반드시 포함할 후보:
- 정상 흐름 1건 이상
- 조회 결과 없음 또는 잘못된 상태
- 외부 API 실패 또는 포트 반환값 이상 상태
- 경계값 입력
- 동시성 보호 또는 조건부 업데이트 로직이 있으면 경쟁 상황 케이스

지양할 것:
- Mocking만으로 구현 세부사항을 과도하게 고정하는 테스트
- 포트/클라이언트 계약과 무관한 private 구현 디테일 검증

## 6. Assert / Interaction 원칙

- 검증의 우선순위는 `상태/결과 > 상호작용`입니다.
- 반환값이 있으면 계약상 중요한 필드만 검증하고, 단순 매핑 필드 전체를 기계적으로 모두 assert하지 않습니다.
- 상태 변경이 있는 경우 실행 전후 값이 드러나도록 assert를 작성합니다.
- 예외 테스트는 `assertThrows`만으로 끝내지 말고 가능하면 `ErrorCode`나 핵심 실패 사유를 함께 검증합니다.
- `then(mock).should()`는 저장, 발행, 외부 API 호출 같은 경계 계약 검증에만 사용합니다.
- collaborator 호출 횟수나 내부 호출 순서를 테스트의 핵심 성공 조건으로 두지 않습니다.

## 7. Mock / Fixture 원칙

- Mock은 Repository Port, 외부 API Client, 메시징 Port 등 경계에만 사용합니다.
- 같은 도메인 내 단순 값 객체나 record는 실제 객체를 우선 사용합니다.
- Fixture는 테스트 클래스 내부의 `create*`, `build*`, `stub*` 메서드를 우선 사용합니다.
- 공용 fixture 유틸은 같은 도메인 테스트에서 반복이 과도할 때만 예외적으로 사용합니다.
- 불필요한 stubbing은 만들지 않습니다.
- 예외 테스트에서는 가능하면 `ErrorCode`까지 함께 검증합니다.

## 8. 작업 완료 후 필수 절차

구현을 마친 뒤에는 아래 순서를 반드시 지킵니다.

1. 이 문서를 다시 읽고 변경한 `facade`/`service`의 테스트 누락 여부를 확인합니다.
2. 대응 테스트 코드를 BDD 스타일과 `@DisplayName` 규칙에 맞춰 작성하거나 부족한 케이스를 보강합니다.
3. 관련 모듈 디렉토리에서 테스트를 실제 실행합니다.
4. 관련 모듈 디렉토리에서 JaCoCo 리포트와 커버리지 검증을 실행합니다.
5. `facade`/`service` 패키지 기준 브랜치 커버리지가 60% 미만이면 테스트를 추가 작성합니다.
6. 테스트와 커버리지 검증을 다시 실행합니다.

## 9. 실행 명령

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

## 10. 커버리지 기준

- 기준 값은 `facade`/`service` 패키지 대상 브랜치 커버리지 60%입니다.
- 저장소 전체 커버리지가 아니라 작업 대상 모듈의 `facade`/`service` 패키지를 기준으로 판단합니다.
- 60%를 넘겼더라도 핵심 실패 분기나 예외 분기가 빠져 있다면 테스트를 보강합니다.
- 60%에 미달하면 작업 완료로 간주하지 않습니다.

## 11. 리뷰 체크포인트

리뷰 시 아래 항목을 확인합니다.

- 변경한 `facade`/`service`에 대응하는 테스트가 존재하는가
- 테스트 메서드에 `@DisplayName`이 작성되었는가
- 시나리오가 복잡할 때 `@Nested`로 읽기 좋게 그룹화했는가
- BDDMockito 문법과 `given / when / then` 흐름을 따르는가
- 정상 흐름과 실패 흐름이 모두 검증되는가
- 예외 코드와 주요 상태 변화를 검증하는가
- 상태/결과 중심으로 검증하고 불필요한 상호작용 검증을 남용하지 않는가
- 테스트를 실제 실행했는가
- JaCoCo 리포트와 커버리지 기준을 확인했는가
