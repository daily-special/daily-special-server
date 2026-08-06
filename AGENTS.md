# daily-special-server 작업 지침

「오늘의 정식」(Daily Special)의 Spring Boot 런타임 서버 저장소다.

「오늘의 정식」은 모험가 길드의 가성비 구내식당을 운영하는 코지 경영 시뮬이다.
플레이어는 장을 보고, 요리하고, 손님의 반응을 본다.

## 먼저 읽을 문서

- `docs/server-design.md`: 서버의 책임과 구현 순서
- `docs/conventions.md`: 계층, 스키마, 테스트, API 규약
- `docs/data-contract.md`: 파이프라인·클라이언트와 공유하는 JSON 계약

저장소 문서는 각각 자체 완결이어야 한다. 다른 저장소의 문서를 참조하도록 새로 쓰지 않는다.

## 세 저장소

```
daily-special-pipeline   Python AI 콘텐츠 생성 — 고정 콘텐츠 JSON을 뱉는다
daily-special-server     Spring Boot 런타임    ← 여기. 플레이 중 변하는 상태를 소유
daily-special-client     Unity 2D 클라이언트   — 화면·입력·만족도 엔진
```

셋은 **JSON 계약으로만** 만난다. 서로의 코드를 복사하지 않는다.

**경계 기준: 플레이 중에 바뀌면 서버, 안 바뀌면 파이프라인의 계약이다.**

### 클라이언트와의 왕복

편지는 **클라이언트 저장소에 둔다** — 저쪽 에이전트가 자기 저장소에만 PR을 올리면 되게 하려는 것이다.

| 파일 (`daily-special-client`) | 무엇 |
|---|---|
| `docs/handoff-from-server.md` | 이쪽에서 보낸 요청과 예고 |
| `docs/handoff-to-server.md` | 저쪽에서 오는 답과 질문. **여기를 읽는다** |

**지금 기다리는 답**: 클라 임시 상태 구현의 **필드 목록**. 그게 API 응답의 모양이 된다.
그 답이 오기 전에는 엔드포인트를 만들지 않는다 (`docs/server-design.md` 0절).

## 현재 단계

**기동 확인까지 끝났다.** `./gradlew bootRun`으로 뜨고 `/actuator/health`가 200이다.

다음은 도메인 상태 모델과 스키마다. 다만 **클라가 실제로 무엇을 요구하는지 보고 나서** 하는 편이 싸다 — 자세한 순서와 이유는 `docs/server-design.md` 0절에 있다.

## 스택

Java 21 · Spring Boot 4.1.0 · Gradle (Kotlin DSL) · PostgreSQL 18 · Flyway · Testcontainers

**Boot 4는 스타터 이름이 3.x와 다르다.** `spring-boot-starter-web`이 아니라
`spring-boot-starter-webmvc`이고, 테스트 스타터도 슬라이스별로 쪼개졌다
(`spring-boot-starter-data-jpa-test` 등). 인터넷의 3.x 예제를 그대로 붙이면 의존성부터 안 맞는다.

## 개발 환경

DB는 Docker로 띄운다. H2를 쓰지 않는다 — 로컬에서 되고 배포에서 깨지는 문법 차이를 애초에 없앤다.

```bash
./gradlew bootRun    # compose.yaml의 Postgres를 자동으로 띄우고 datasource를 꽂는다
./gradlew test       # Testcontainers가 진짜 Postgres를 띄운다. Docker가 떠 있어야 한다
```

`compose.yaml`과 `TestcontainersConfiguration.java`의 **이미지 태그는 같아야 한다.**
다르면 테스트가 통과해도 실서버에서 깨진다.

## 코드 규약

- 식별자는 영어, 주석·독스트링·오류 메시지는 한국어로 쓴다.
- **도메인은 Spring을 모른다.** `domain/`에 `org.springframework` import를 두지 않는다.
- 스키마는 **Flyway가 소유한다.** JPA는 `ddl-auto=validate`로 검증만 한다.
- JSON은 `snake_case`다. `spring.jackson.property-naming-strategy=SNAKE_CASE`로 맞춰뒀다.
- 테스트는 Testcontainers로 진짜 Postgres에 붙인다. 인메모리 DB로 대체하지 않는다.
- 설계 결정은 주석보다 테스트로 고정한다.

## 작업 방식

코드나 파일을 변경하기 전에는 다음을 짧게 보고한다.

1. 무엇을 만들며, 어떤 파일을 새로 만들거나 수정하는가
2. 이번 범위 밖인 것은 무엇인가
3. 사용자 판단이 필요한 것이 있는가. 없으면 `결정 필요 없음`이라고 명시한다

작업 중 파일을 변경할 때는 무엇을 왜 바꾸는지 한 줄로 알린다. 계획이나 범위가 달라지면 계속 진행하지 말고 보고한다.

완료 후에는 변경 사항, 하지 않은 사항과 이유, 테스트 결과, 문서 반영 여부를 보고한다.
테스트가 실패했으면 실패했다고 쓰고 출력을 함께 보인다.

커밋은 사용자가 요청할 때만 한다. 요청이 없으면 권장 커밋 메시지만 제시한다.

## Git

- 작업은 `main`이 아닌 `<type>/<english-slug>` 브랜치에서 한다. 예: `feat/day-cycle`
- 커밋 메시지: `<type>: <한국어 설명>` 한 줄, 50자 이내
- 허용 type: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`
- squash merge를 기본으로 한다.

## 대회

NAN 2026 (NHN 게임×AI 해커톤, 채용 연계형). **예선 마감 8/10, 본선 9/4~6.**

**예선 제출물 중 이 서버에서 나오는 것은 없다.** 예선은 빌드·시연 영상·게임 소개서·
AI 활용 기술 문서이고, 그건 클라이언트와 파이프라인이 만든다. 이 저장소의 목표 시점은
**본선**이다. 예선 주간에 여기서 시간을 많이 쓰고 있다면 우선순위를 잘못 잡은 것이다.
