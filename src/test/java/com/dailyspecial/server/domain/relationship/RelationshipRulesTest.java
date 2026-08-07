package com.dailyspecial.server.domain.relationship;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * 관계와 정보 공개의 이식 명세.
 *
 * <p><b>클라이언트가 C#으로 옮길 때 이 케이스를 그대로 가져간다.</b> 예선 빌드는 오프라인이라
 * 서버를 부르지 못한다 — 필요한 것은 API가 아니라 이 규칙이다.
 */
class RelationshipRulesTest {

	private final RelationshipRules rules = new RelationshipRules(RelationshipNumbers.defaults());

	@Nested
	@DisplayName("관계 증감")
	class Affinity {

		@ParameterizedTest(name = "만족 {0} → {1}")
		@DisplayName("고정 벡터 — delta = floor((만족 - 0.4) * 20 + 0.5)")
		@CsvSource({
			"1.0,    12",
			"0.75,    7",
			"0.5,     2",
			"0.4,     0",
			// 경계. raw가 정확히 -0.5가 아니라 부동소수 오차로 아주 살짝 아래다.
			// 두 언어가 같은 순서로 계산하면 같은 값이 나온다.
			"0.375,  -1",
			"0.3,    -2",
			"0.15,   -5",
			"0.0,    -8",
		})
		void matchesGoldenDeltas(double satisfaction, int expectedDelta) {
			Relationship before = new Relationship(50, Map.of());

			Relationship after = rules.afterVisit(before, satisfaction, List.of());

			assertEquals(50 + expectedDelta, after.affinity());
		}

		@Test
		@DisplayName("0 아래로 내려가지 않는다")
		void neverGoesBelowZero() {
			Relationship broke = rules.afterVisit(Relationship.none(), 0.0, List.of());

			assertEquals(0, broke.affinity());
		}

		@Test
		@DisplayName("상한을 넘지 않는다")
		void neverExceedsTheCeiling() {
			Relationship maxed = new Relationship(95, Map.of());

			assertEquals(100, rules.afterVisit(maxed, 1.0, List.of()).affinity());
		}

		@Test
		@DisplayName("만족 1.0을 거듭하면 다섯 번에 낯이 익고 열 번에 단골이 된다")
		void curveFeelsRight() {
			// 수치가 가정값이라 "곡선이 그럴듯한가"가 유일한 근거다. 그 근거를 여기 고정한다.
			Relationship relationship = Relationship.none();
			int familiarAt = 0;
			int regularAt = 0;

			for (int visit = 1; visit <= 12; visit++) {
				relationship = rules.afterVisit(relationship, 1.0, List.of());
				Tier tier = rules.tierOf(relationship);
				if (familiarAt == 0 && tier == Tier.FAMILIAR) {
					familiarAt = visit;
				}
				if (regularAt == 0 && tier == Tier.REGULAR) {
					regularAt = visit;
				}
			}

			assertEquals(2, familiarAt, "낯이 익는 방문 수");
			assertEquals(5, regularAt, "단골이 되는 방문 수");
		}

		@Test
		@DisplayName("같은 입력이면 몇 번을 물어도 같은 결과")
		void isDeterministic() {
			Relationship before = new Relationship(37, Map.of("heat", 1));

			Relationship first = rules.afterVisit(before, 0.62, List.of("seasoning"));
			for (int i = 0; i < 50; i++) {
				assertEquals(first, rules.afterVisit(before, 0.62, List.of("seasoning")));
			}
		}
	}

	@Nested
	@DisplayName("단계")
	class Tiers {

		@ParameterizedTest(name = "{0} → {1}")
		@DisplayName("문턱 경계")
		@CsvSource({
			"0,   STRANGER", "19,  STRANGER",
			"20,  FAMILIAR", "59,  FAMILIAR",
			"60,  REGULAR", "100, REGULAR",
		})
		void boundaries(int affinity, Tier expected) {
			assertEquals(expected, rules.tierOf(new Relationship(affinity, Map.of())));
		}
	}

	@Nested
	@DisplayName("정보 공개")
	class Disclosing {

		@Test
		@DisplayName("낯선 손님에게는 아무것도 열리지 않는다")
		void strangerRevealsNothing() {
			Disclosure disclosure = rules.disclose(Relationship.none());

			assertFalse(disclosure.preferredNeeds());
			assertFalse(disclosure.dietary());
			assertFalse(disclosure.allAxes());
			assertTrue(disclosure.revealedAxes().isEmpty());
		}

		@Test
		@DisplayName("낯이 익으면 평소 성향이 열린다 — 식이 제약은 아직이다")
		void familiarRevealsPreferredNeedsOnly() {
			Disclosure disclosure = rules.disclose(new Relationship(30, Map.of()));

			assertTrue(disclosure.preferredNeeds());
			assertFalse(disclosure.dietary());
			assertFalse(disclosure.allAxes());
		}

		@Test
		@DisplayName("단골이면 식이 제약과 모든 축이 열린다")
		void regularRevealsEverything() {
			Disclosure disclosure = rules.disclose(new Relationship(60, Map.of()));

			assertTrue(disclosure.preferredNeeds());
			assertTrue(disclosure.dietary());
			assertTrue(disclosure.allAxes());
			assertTrue(disclosure.revealsAxis("heat"), "단골이면 힌트가 없어도 축이 보인다");
		}

		@Test
		@DisplayName("피드백이 세 번 쌓인 축은 관계가 얕아도 열린다")
		void axisOpensByObservation() {
			// 이 곡선의 핵심이다 — 관계가 얕아도 관찰로 좁혀지는 길이 있어야 추론이 놀이가 된다.
			Relationship relationship = Relationship.none();
			for (int visit = 0; visit < 3; visit++) {
				relationship = rules.afterVisit(relationship, 0.2, List.of("seasoning"));
			}

			Disclosure disclosure = rules.disclose(relationship);

			assertEquals(Tier.STRANGER, rules.tierOf(relationship), "관계는 아직 얕다");
			assertEquals(Set.of("seasoning"), disclosure.revealedAxes());
			assertTrue(disclosure.revealsAxis("seasoning"));
			assertFalse(disclosure.revealsAxis("heat"), "힌트가 없는 축은 안 열린다");
		}

		@Test
		@DisplayName("두 번으로는 열리지 않는다")
		void twoHintsAreNotEnough() {
			Relationship relationship = Relationship.none();
			for (int visit = 0; visit < 2; visit++) {
				relationship = rules.afterVisit(relationship, 0.5, List.of("heat"));
			}

			assertTrue(rules.disclose(relationship).revealedAxes().isEmpty());
		}

		@Test
		@DisplayName("어긋난 축마다 힌트가 하나씩 쌓인다")
		void hintsAccumulatePerAxis() {
			Relationship relationship =
					rules.afterVisit(Relationship.none(), 0.5, List.of("heat", "seasoning"));
			relationship = rules.afterVisit(relationship, 0.5, List.of("heat"));

			assertEquals(2, relationship.hintsFor("heat"));
			assertEquals(1, relationship.hintsFor("seasoning"));
			assertEquals(0, relationship.hintsFor("cook_time"));
		}
	}

	@Test
	@DisplayName("잘못된 입력을 거부한다")
	void rejectsBadInput() {
		assertThrows(IllegalArgumentException.class, () -> new RelationshipRules(null));
		assertThrows(
				IllegalArgumentException.class, () -> rules.afterVisit(null, 0.5, List.of()));
		assertThrows(
				IllegalArgumentException.class,
				() -> rules.afterVisit(Relationship.none(), 1.5, List.of()));
		assertThrows(
				IllegalArgumentException.class,
				() -> rules.afterVisit(Relationship.none(), Double.NaN, List.of()));
		assertThrows(
				IllegalArgumentException.class,
				() -> rules.afterVisit(Relationship.none(), 0.5, List.of(" ")));
		assertThrows(IllegalArgumentException.class, () -> new Relationship(-1, Map.of()));
	}
}
