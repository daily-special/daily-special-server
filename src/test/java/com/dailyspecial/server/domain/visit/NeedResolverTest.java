package com.dailyspecial.server.domain.visit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 상태에서 욕구를 뽑는 규칙의 이식 명세.
 *
 * <p><b>클라이언트가 C#으로 옮길 때 이 케이스를 그대로 가져간다.</b>
 *
 * <p>가장 중요한 것은 첫 묶음이다 — 설계 문서에 적힌 예시 셋. 점수표의 숫자는 가정값이지만
 * <b>그 셋을 재현하도록 맞춘 값</b>이라, 숫자를 만지면 여기가 먼저 빨개진다. 그때 예시가
 * 여전히 맞는지 다시 보라는 뜻이다.
 */
class NeedResolverTest {

	private final NeedResolver resolver = new NeedResolver(NeedNumbers.defaults());

	/** 다른 신호가 끼어들지 않는 평범한 상태. 하나씩만 바꿔가며 본다. */
	private static VisitState plain(int hunger, Condition condition, Mood mood, int wallet) {
		return new VisitState(hunger, condition, mood, wallet);
	}

	@Nested
	@DisplayName("설계 문서의 예시")
	class DesignDocumentExamples {

		@Test
		@DisplayName("몸이 안 좋으면 → 회복 + 순한")
		void hurtOrTiredWantsRestorativeAndMild() {
			// 설계 문서는 "부상 + 피로"라고 적었지만 컨디션은 셋 중 하나다.
			// "몸이 안 좋으면"으로 읽어 둘 다 같은 결과를 내게 했다.
			VisitState injured = plain(30, Condition.INJURED, Mood.CALM, 30);
			VisitState tired = plain(30, Condition.TIRED, Mood.CALM, 30);

			assertEquals(List.of(Need.RESTORATIVE, Need.MILD), resolver.resolve(injured, GuestTraits.of()));
			assertEquals(List.of(Need.RESTORATIVE, Need.MILD), resolver.resolve(tired, GuestTraits.of()));
		}

		@Test
		@DisplayName("지갑이 부족하면 → 저렴")
		void brokeGuestWantsAffordable() {
			VisitState broke = plain(30, Condition.NORMAL, Mood.CALM, 10);

			assertEquals(List.of(Need.AFFORDABLE), resolver.resolve(broke, GuestTraits.of()));
		}

		@Test
		@DisplayName("들뜬 기분이면 → 특별 + 포만")
		void elatedAndHungryWantsSpecialAndFilling() {
			// 설계 문서의 "축제 기분"을 들뜸(elated)으로 읽었다 — 기분 3종에 축제가 없다.
			VisitState festive = plain(80, Condition.NORMAL, Mood.ELATED, 30);

			assertEquals(List.of(Need.SPECIAL, Need.FILLING), resolver.resolve(festive, GuestTraits.of()));
		}
	}

	@Nested
	@DisplayName("고르는 규칙")
	class SelectionRules {

		@Test
		@DisplayName("욕구는 최소 하나, 최대 둘")
		void alwaysBetweenOneAndTwo() {
			for (Condition condition : Condition.values()) {
				for (Mood mood : Mood.values()) {
					for (int hunger : new int[] {0, 39, 40, 69, 70, 100}) {
						for (int wallet : new int[] {8, 16, 17, 24, 25, 40}) {
							List<Need> needs =
									resolver.resolve(plain(hunger, condition, mood, wallet), GuestTraits.of());

							assertTrue(!needs.isEmpty() && needs.size() <= 2, "욕구 수: " + needs);
						}
					}
				}
			}
		}

		@Test
		@DisplayName("문턱을 넘은 것이 없어도 하나는 나온다")
		void fallsBackToTheTopScoreWhenNothingReachesTheThreshold() {
			// 허기 50(포만 1) · 정상(자극 1) · 평온 · 지갑 30(저렴 0) — 둘 다 1점이라 문턱 미달.
			VisitState bland = plain(50, Condition.NORMAL, Mood.CALM, 30);

			assertEquals(List.of(Need.FILLING), resolver.resolve(bland, GuestTraits.of()));
		}

		@Test
		@DisplayName("점수가 같으면 어휘 선언 순서로 앞선 것을 고른다")
		void tiesFollowDeclarationOrder() {
			// 피로는 회복과 순한에 똑같이 2점을 준다. 선언 순서가 회복(1) < 순한(2)이다.
			VisitState tired = plain(30, Condition.TIRED, Mood.CALM, 30);

			assertEquals(List.of(Need.RESTORATIVE, Need.MILD), resolver.resolve(tired, GuestTraits.of()));
		}

		@Test
		@DisplayName("같은 입력이면 몇 번을 물어도 같은 답")
		void isDeterministic() {
			VisitState state = plain(72, Condition.TIRED, Mood.GLOOMY, 12);
			GuestTraits traits = GuestTraits.of(Need.SPECIAL);

			List<Need> first = resolver.resolve(state, traits);
			for (int i = 0; i < 50; i++) {
				assertEquals(first, resolver.resolve(state, traits));
			}
		}
	}

	@Nested
	@DisplayName("평소 성향")
	class PreferredNeeds {

		@Test
		@DisplayName("저울을 기울이되 뒤집지는 않는다")
		void nudgesWithoutOverriding() {
			// 부상이면 회복이 3점이라 성향 1점으로는 못 뒤집는다.
			VisitState injured = plain(30, Condition.INJURED, Mood.CALM, 30);

			List<Need> needs = resolver.resolve(injured, GuestTraits.of(Need.SPECIAL));

			assertEquals(Need.RESTORATIVE, needs.get(0));
		}

		@Test
		@DisplayName("같은 상태의 두 손님을 가른다")
		void separatesGuestsInTheSameState() {
			// 이것이 성향이 존재하는 이유다. 상태만 보면 두 손님이 똑같아진다.
			VisitState state = plain(50, Condition.NORMAL, Mood.CALM, 30);

			List<Need> plain = resolver.resolve(state, GuestTraits.of());
			List<Need> stimulated = resolver.resolve(state, GuestTraits.of(Need.STIMULATING));

			assertEquals(List.of(Need.FILLING), plain);
			assertEquals(List.of(Need.STIMULATING), stimulated);
		}
	}

	@Nested
	@DisplayName("설명")
	class Explanation {

		@Test
		@DisplayName("어느 신호가 몇 점을 줬는지 돌려준다")
		void reportsEveryScore() {
			Map<Need, Integer> scores =
					resolver.explain(plain(80, Condition.NORMAL, Mood.ELATED, 30), GuestTraits.of());

			assertEquals(3, scores.get(Need.SPECIAL), "들뜸");
			assertEquals(2, scores.get(Need.FILLING), "허기 80");
			assertEquals(2, scores.get(Need.STIMULATING), "정상 1 + 들뜸 1");
			assertEquals(0, scores.get(Need.AFFORDABLE), "지갑 30");
		}

		@Test
		@DisplayName("어휘 전부가 키로 들어 있다")
		void coversTheWholeVocabulary() {
			Map<Need, Integer> scores =
					resolver.explain(plain(50, Condition.NORMAL, Mood.CALM, 30), GuestTraits.of());

			for (Need need : Need.values()) {
				assertTrue(scores.containsKey(need), "빠진 욕구: " + need);
			}
		}
	}

	@Test
	@DisplayName("없는 인자를 거부한다")
	void rejectsMissingArguments() {
		assertThrows(IllegalArgumentException.class, () -> new NeedResolver(null));
		assertThrows(
				IllegalArgumentException.class, () -> resolver.resolve(null, GuestTraits.of()));
		assertThrows(
				IllegalArgumentException.class,
				() -> resolver.resolve(plain(50, Condition.NORMAL, Mood.CALM, 30), null));
	}

	@Test
	@DisplayName("욕구 어휘는 계약 표기로 왕복한다")
	void slugsRoundTrip() {
		for (Need need : Need.values()) {
			assertEquals(need, Need.fromSlug(need.slug()));
			assertFalse(need.slug().isBlank());
		}

		assertThrows(IllegalArgumentException.class, () -> Need.fromSlug("nonexistent"));
	}
}
