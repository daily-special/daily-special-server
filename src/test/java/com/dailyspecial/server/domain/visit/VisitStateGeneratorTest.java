package com.dailyspecial.server.domain.visit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * 오늘 상태 생성기 이식 명세.
 *
 * <p><b>클라이언트가 C#으로 옮길 때 이 케이스를 그대로 가져간다.</b> 전부 통과하면 두 구현이
 * 어긋나지 않는다. 만족도 엔진과 같은 방식이다.
 */
class VisitStateGeneratorTest {

	private final VisitStateGenerator generator = new VisitStateGenerator(VisitNumbers.defaults());

	@ParameterizedTest(name = "{0} / {1}일차 / {2} / 저렴성향={3}")
	@DisplayName("고정 벡터 — 기본 수치에서 나오는 값")
	@CsvSource({
		"save-1, 1, guest_rolf, false, 71, TIRED,  ELATED, 24",
		"save-1, 1, guest_rolf, true,  71, TIRED,  ELATED,  8",
		"save-1, 2, guest_rolf, false, 63, NORMAL, CALM,   13",
		"save-1, 2, guest_rolf, true,  63, NORMAL, CALM,   19",
		"save-2, 1, guest_rolf, false, 99, NORMAL, CALM,    8",
		"save-2, 1, guest_rolf, true,  99, NORMAL, CALM,   10",
	})
	void matchesGoldenVectors(
			String saveId,
			int dayNumber,
			String guestId,
			boolean prefersAffordable,
			int hunger,
			Condition condition,
			Mood mood,
			int wallet) {
		VisitState state =
				generator.generate(new VisitSeed(saveId, dayNumber, guestId), traitsFor(prefersAffordable));

		assertEquals(new VisitState(hunger, condition, mood, wallet), state);
	}

	@Test
	@DisplayName("같은 씨앗이면 몇 번을 물어도 같은 상태가 나온다")
	void isDeterministic() {
		VisitSeed seed = new VisitSeed("save-1", 3, "guest_mira");
		GuestTraits traits = GuestTraits.of();

		VisitState first = generator.generate(seed, traits);
		for (int i = 0; i < 100; i++) {
			assertEquals(first, generator.generate(seed, traits), i + "번째에 값이 갈렸다");
		}
	}

	@Test
	@DisplayName("저렴 성향은 지갑만 바꾼다 — 허기·컨디션·기분은 그대로다")
	void affordableOnlyMovesTheWallet() {
		// 지갑을 마지막에 뽑기 때문이다. 뽑는 순서가 명세의 일부라는 근거가 여기 있다.
		for (int day = 1; day <= 50; day++) {
			VisitSeed seed = new VisitSeed("save-1", day, "guest_rolf");

			VisitState plain = generator.generate(seed, GuestTraits.of());
			VisitState thrifty = generator.generate(seed, GuestTraits.of(Need.AFFORDABLE));

			assertEquals(plain.hunger(), thrifty.hunger(), day + "일차 허기");
			assertEquals(plain.condition(), thrifty.condition(), day + "일차 컨디션");
			assertEquals(plain.mood(), thrifty.mood(), day + "일차 기분");
		}
	}

	@Test
	@DisplayName("모든 값이 설정한 구간 안에 있다")
	void staysWithinConfiguredRanges() {
		VisitNumbers numbers = VisitNumbers.defaults();

		for (int day = 1; day <= 2_000; day++) {
			VisitState state = generator.generate(new VisitSeed("save-1", day, "guest_rolf"), GuestTraits.of());

			assertTrue(
					state.hunger() >= numbers.hungerMin() && state.hunger() <= numbers.hungerMax(),
					"허기가 구간을 벗어났다: " + state.hunger());
			assertTrue(
					state.wallet() >= numbers.walletMin() && state.wallet() <= numbers.walletMax(),
					"지갑이 구간을 벗어났다: " + state.wallet());
		}
	}

	@Test
	@DisplayName("저렴 성향 손님의 지갑은 낮춘 상한을 넘지 않는다")
	void thriftyGuestsStayUnderTheLoweredCeiling() {
		int ceiling = VisitNumbers.defaults().affordableWalletMax();

		for (int day = 1; day <= 2_000; day++) {
			VisitState state = generator.generate(new VisitSeed("save-1", day, "guest_rolf"), GuestTraits.of(Need.AFFORDABLE));

			assertTrue(state.wallet() <= ceiling, "저렴 성향인데 지갑이 " + state.wallet() + "이다");
		}
	}

	@Test
	@DisplayName("분포가 설정한 가중치에 수렴한다")
	void distributionFollowsTheWeights() {
		int samples = 20_000;
		Map<Condition, Integer> conditions = new EnumMap<>(Condition.class);
		Map<Mood, Integer> moods = new EnumMap<>(Mood.class);

		for (int day = 1; day <= samples; day++) {
			VisitState state = generator.generate(new VisitSeed("save-1", day, "guest_rolf"), GuestTraits.of());

			conditions.merge(state.condition(), 1, Integer::sum);
			moods.merge(state.mood(), 1, Integer::sum);
		}

		// 허용오차 3%p. 가중치가 70/20/10과 60/25/15이므로 표본 2만이면 넉넉히 들어온다.
		assertShare(conditions, Condition.NORMAL, samples, 0.70);
		assertShare(conditions, Condition.TIRED, samples, 0.20);
		assertShare(conditions, Condition.INJURED, samples, 0.10);
		assertShare(moods, Mood.CALM, samples, 0.60);
		assertShare(moods, Mood.ELATED, samples, 0.25);
		assertShare(moods, Mood.GLOOMY, samples, 0.15);
	}

	@Test
	@DisplayName("없는 인자를 거부한다")
	void rejectsMissingArguments() {
		assertThrows(IllegalArgumentException.class, () -> new VisitStateGenerator(null));
		assertThrows(
				IllegalArgumentException.class, () -> generator.generate(null, GuestTraits.of()));
		assertThrows(
				IllegalArgumentException.class,
				() -> generator.generate(new VisitSeed("save-1", 1, "guest_rolf"), null));
	}

	/** 고정 벡터 표가 boolean을 주므로 여기서 성향으로 옮긴다. */
	private static GuestTraits traitsFor(boolean prefersAffordable) {
		return prefersAffordable ? GuestTraits.of(Need.AFFORDABLE) : GuestTraits.of();
	}

	private static <T extends Enum<T>> void assertShare(
			Map<T, Integer> counts, T value, int samples, double expected) {
		double actual = counts.getOrDefault(value, 0) / (double) samples;

		assertTrue(
				Math.abs(actual - expected) <= 0.03,
				"%s 비율이 %.3f인데 기대는 %.2f다".formatted(value, actual, expected));
	}
}
