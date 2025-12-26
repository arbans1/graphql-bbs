---
applyTo: 'src/test/java/**'
---

# 테스팅 지침 (Testing Guidelines)

**적용 범위**: `src/test/java/**`  
**컨텍스트**: JUnit5, AssertJ, Mockito, Spring GraphQL Tester 환경에서의 테스트 코드 작성

---

## 1. 최신 스택 및 Deprecated 방지 규칙

- JUnit 4(@Test 권장 안 함) 대신 **JUnit 5(@Test org.junit.jupiter.api.Test)** 사용
- `MockitoAnnotations.initMocks()` 대신 **@ExtendWith(MockitoExtension.class)** 사용
- GraphQL 테스트 시 `GraphQLTestTemplate` 대신 최신 **GraphQlTester** 또는 **HttpGraphQlTester** 사용
- `expected` 속성(예: @Test(expected=...)) 대신 **Assertions.assertThrows()** 사용

---

## 2. 테스트 구조 및 명명 (Given-When-Then)

- **@DisplayName**을 반드시 사용하여 테스트 목적을 한글로 명시
  - 예: `@DisplayName("존재하지 않는 사용자 ID로 조회 시 예외 발생")`
- 테스트 코드는 `// given`, `// when`, `// then` 주석으로 단계를 구분
- 테스트 메서드 명칭은 `snake_case` 또는 `camelCase` 중 프로젝트 관례를 따르되, 의도가 명확해야 함

---

## 3. 검증(Assertion) 지침

- 모든 단언문은 **AssertJ의 assertThat()**을 기본으로 사용
- `assertEquals(a, b)` 같은 JUnit 기본 메서드보다 `assertThat(a).isEqualTo(b)` 스타일 권장
- 리스트 검증 시 `hasSize()`, `extracting()`, `containsExactly()` 등을 활용하여 상세히 검증

---

## 4. GraphQL 및 API 테스트 특화

- GraphQL 요청 시 실제 스키마와 일치하는지 `variable`을 활용해 테스트
- 에러 응답 검증 시 `errors().expect()`를 사용하여 구체적인 `FieldErrorCode`가 반환되는지 확인
- 권한(Role) 테스트 시 `@WithMockUser` 등을 활용하여 인가 로직을 반드시 포함

---

## 5. 주석 및 언어 규칙 (필수)

- 모든 주석은 **한국어로 작성**
- 주석 끝에 ~합니다, ~습니다 체 사용 금지
  - ✅ 올바른 예: `// 데이터를 초기화함`, `// 예외 발생 여부 확인`
  - ❌ 잘못된 예: `// 데이터를 초기화합니다`, `// 예외 발생 여부를 확인합니다`
- 복잡한 Mocking 설정에는 반드시 이유를 주석으로 기록

---

## 6. Null 체크 및 예외 처리

- **NullMarked**가 반드시 적용되어 있는지 확인
- Null 체크가 누락된 부분이 없는지 꼼꼼히 점검
- 예외 처리 로직이 적절히 구현되어 있는지 확인

---

## 7. 보안 및 권한 제어 테스트

- 권한이 필요한 API의 경우 인가되지 않은 사용자의 접근을 막는 테스트 포함
- 관리자(Admin/Operator) 전용 기능에 일반 사용자가 접근할 수 없음을 검증
- 본인의 데이터가 아닌 타인의 데이터 접근 시도를 차단하는지 테스트
