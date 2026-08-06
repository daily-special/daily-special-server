package com.dailyspecial.server.domain.visit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 설정의 불변식은 생성 시점에 막는다 — 어긋난 채로 서버가 뜨면 원인을 찾기 어렵다. */
class VisitNumbersTest {

	@Test
	@DisplayName("기본값은 불변식을 만족한다")
	void defaultsAreValid() {
		VisitNumbers numbers = VisitNumbers.defaults();

		assertEquals(0, numbers.hungerMin());
		assertEquals(100, numbers.hungerMax());
		assertEquals(8, numbers.walletMin());
		assertEquals(40, numbers.walletMax());
	}

	@Test
	@DisplayName("저렴 성향 지갑 상한 = 하한 + (구간 × 비율)")
	void affordableWalletMaxIsDerived() {
		// 8 + (40 - 8) × 0.5 = 24
		assertEquals(24, VisitNumbers.defaults().affordableWalletMax());
	}

	@Test
	@DisplayName("뒤집힌 구간을 거부한다")
	void rejectsInvertedRanges() {
		assertThrows(IllegalArgumentException.class, () -> withHunger(100, 0));
		assertThrows(IllegalArgumentException.class, () -> withWallet(40, 8));
	}

	@Test
	@DisplayName("지갑 하한이 0 이하이면 거부한다")
	void rejectsNonPositiveWalletFloor() {
		assertThrows(IllegalArgumentException.class, () -> withWallet(0, 40));
	}

	@Test
	@DisplayName("가중치에 어휘가 빠지면 거부한다")
	void rejectsMissingWeight() {
		Map<Condition, Integer> incomplete = new EnumMap<>(Condition.class);
		incomplete.put(Condition.NORMAL, 70);
		incomplete.put(Condition.TIRED, 30);
		// INJURED가 없다

		assertThrows(IllegalArgumentException.class, () -> withConditionWeights(incomplete));
	}

	@Test
	@DisplayName("음수 가중치와 합이 0인 가중치를 거부한다")
	void rejectsUnusableWeights() {
		assertThrows(IllegalArgumentException.class, () -> withConditionWeights(conditionWeights(-1, 1, 1)));
		assertThrows(IllegalArgumentException.class, () -> withConditionWeights(conditionWeights(0, 0, 0)));
	}

	@Test
	@DisplayName("저렴 성향 지갑 비율이 0 이하이거나 1을 넘으면 거부한다")
	void rejectsRatioOutOfRange() {
		assertThrows(IllegalArgumentException.class, () -> withRatio(0.0));
		assertThrows(IllegalArgumentException.class, () -> withRatio(1.5));
	}

	private static Map<Condition, Integer> conditionWeights(int normal, int injured, int tired) {
		Map<Condition, Integer> weights = new EnumMap<>(Condition.class);
		weights.put(Condition.NORMAL, normal);
		weights.put(Condition.INJURED, injured);
		weights.put(Condition.TIRED, tired);
		return weights;
	}

	private static VisitNumbers withHunger(int min, int max) {
		VisitNumbers base = VisitNumbers.defaults();
		return new VisitNumbers(
				min, max, base.conditionWeights(), base.moodWeights(),
				base.walletMin(), base.walletMax(), base.affordableWalletMaxRatio());
	}

	private static VisitNumbers withWallet(int min, int max) {
		VisitNumbers base = VisitNumbers.defaults();
		return new VisitNumbers(
				base.hungerMin(), base.hungerMax(), base.conditionWeights(), base.moodWeights(),
				min, max, base.affordableWalletMaxRatio());
	}

	private static VisitNumbers withConditionWeights(Map<Condition, Integer> weights) {
		VisitNumbers base = VisitNumbers.defaults();
		return new VisitNumbers(
				base.hungerMin(), base.hungerMax(), weights, base.moodWeights(),
				base.walletMin(), base.walletMax(), base.affordableWalletMaxRatio());
	}

	private static VisitNumbers withRatio(double ratio) {
		VisitNumbers base = VisitNumbers.defaults();
		return new VisitNumbers(
				base.hungerMin(), base.hungerMax(), base.conditionWeights(), base.moodWeights(),
				base.walletMin(), base.walletMax(), ratio);
	}
}
