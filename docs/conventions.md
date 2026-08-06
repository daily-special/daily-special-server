# 코드 규약 — daily-special-server

> **코드를 쓰기 전에 읽는다.** 이 문서가 코드를 강제한다.
>
> **지금은 얇다.** 엔티티도 API도 아직 없어서, 추측으로 규약을 박으면 며칠 뒤에 거짓말이 된다.
> 여기 있는 것은 **지금 정하지 않으면 나중에 비싸지는 것**만이다.
>
> 이 문서는 자체 완결로 쓴다. 외부 문서를 참조하지 않는다.
> 최종 수정: 2026-08-06

---

## 1. ⭐ 도메인은 Spring을 모른다

**이 저장소에서 가장 중요한 규약이다.**

```
com.dailyspecial.server
  domain/       순수 Java. org.springframework을 import하지 않는다
  application/  유스케이스. 트랜잭션 경계
  api/          컨트롤러 · 요청/응답 DTO
  infra/        JPA 엔티티 · 리포지토리 구현
```

```java
// ✅ 순수 Java. 스프링 컨텍스트 없이 밀리초 단위로 테스트된다
public record VisitState(int hunger, int condition, int mood, int wallet) { ... }

// ❌ 도메인이 JPA를 안다. 이제 DB 없이는 규칙 하나도 못 돌린다
@Entity
public class VisitState { @Id Long id; ... }
```

**도메인 규칙과 JPA 엔티티를 같은 클래스로 쓰지 않는다.** 붙여두면 규칙을 검증하려고 컨테이너를 띄우게 되고, 스키마를 바꾸려면 규칙을 건드리게 된다. 나중에 떼는 건 비싸고, 지금 나누는 건 공짜다.

## 2. 스키마는 Flyway가 소유한다

`src/main/resources/db/migration/`의 `V<번호>__<설명>.sql`이 유일한 스키마 정의다.

- JPA는 `ddl-auto=validate`다. **엔티티와 스키마가 어긋나면 기동에서 막힌다**
- `update`로 바꾸지 않는다 — 어긋난 채로 굴러가서 나중에 알게 된다
- 이미 머지된 마이그레이션은 **고치지 않는다.** 새 번호를 더한다

## 3. JSON은 `snake_case`다

파이프라인 계약(1-1절)이 `snake_case`이고, 클라이언트도 그렇게 읽는다. **세 저장소가 같은 모양을 쓰는 편이 싸다.**

`application.properties`에 `spring.jackson.property-naming-strategy=SNAKE_CASE`로 걸어뒀다. 필드마다 `@JsonProperty`를 붙이지 않는다.

## 4. 테스트는 진짜 Postgres에 붙는다

- Testcontainers를 쓴다. **인메모리 DB로 대체하지 않는다** (근거: `server-design.md` 4-1절)
- 도메인 테스트는 스프링 컨텍스트를 띄우지 않는다. 순수 JUnit이다
- 컨텍스트가 필요하면 슬라이스를 쓴다 — 통짜 `@SpringBootTest`를 기본값으로 삼지 않는다
- 설계 결정은 주석이 아니라 테스트로 고정한다

**Docker가 떠 있어야 테스트가 돈다.** 이건 규약의 대가로 받아들인 것이다.

## 5. Boot 4는 스타터 이름이 다르다

```kotlin
implementation("org.springframework.boot:spring-boot-starter-webmvc")      // starter-web 아님
testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
```

Boot 4에서 스타터가 잘게 쪼개졌다. **인터넷의 3.x 예제를 그대로 붙이면 의존성 이름부터 안 맞는다.** 검색 결과를 복사하기 전에 버전을 확인한다.

## 6. 컨테이너 이미지 태그를 고정한다

`compose.yaml`과 `TestcontainersConfiguration.java`가 **같은 태그**를 써야 한다. 지금은 `postgres:18-alpine`이다.

`latest`로 두면 어느 날 메이저가 올라가 조용히 깨지고, 둘이 다르면 **테스트가 통과해도 실서버에서 깨진다.** 올릴 때는 두 곳을 같이 올린다.

## 7. 언어

- 식별자는 영어
- 주석·독스트링·오류 메시지는 **한국어**

## 8. 커밋·브랜치

파이프라인·클라이언트 저장소와 같다.

- 브랜치 `<type>/<영문-슬러그>` (예: `feat/day-cycle`). **main에서 작업하지 않는다**
- 커밋 `<type>: <한국어 설명>` 제목 한 줄, 50자 이내
- squash 머지. PR 제목이 곧 커밋 제목이므로 45자 안쪽
- `type`: `feat` `fix` `refactor` `docs` `test` `chore`
