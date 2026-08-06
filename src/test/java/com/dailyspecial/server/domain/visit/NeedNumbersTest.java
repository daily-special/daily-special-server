package com.dailyspecial.server.domain.visit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 설정이 어긋난 채로 서버가 뜨면 손님이 이상한 것을 원하고, 원인을 찾기 어렵다. */
class NeedNumbersTest {

	@Test
	@DisplayName("기본값은 불변식을 만족한다")
	void defaultsAreValid() {
		NeedNumbers numbers = NeedNumbers.defaults();

		assertEquals(70, numbers.hungerHigh());
		assertEquals(40, numbers.hungerMid());
		assertEquals(2, numbers.maxNeeds());
	}

	@Test
	@DisplayName("뒤집힌 문턱을 거부한다")
	void rejectsInvertedThresholds() {
		assertThrows(IllegalArgumentException.class, () -> withHunger(70, 40));
		assertThrows(IllegalArgumentException.class, () -> withWallet(24, 16));
	}

	@Test
	@DisplayName("욕구가 0개 나오는 설정을 거부한다")
	void rejectsZeroNeeds() {
		// 욕구가 없으면 만족도의 첫 항을 계산할 수 없고 화면에 띄울 것도 없다.
		assertThrows(IllegalArgumentException.class, () -> withMaxNeeds(0));
	}

	@Test
	@DisplayName("선택 문턱이 0 이하이면 거부한다")
	void rejectsNonPositiveThreshold() {
		assertThrows(IllegalArgumentException.class, () -> withSelectionThreshold(0));
	}

	private static NeedNumbers rebuilt(
			int hungerHigh, int hungerMid, int walletLow, int walletMid, int threshold, int maxNeeds) {
		NeedNumbers base = NeedNumbers.defaults();
		return new NeedNumbers(
				hungerHigh,
				hungerMid,
				base.fillingWhenVeryHungry(),
				base.fillingWhenHungry(),
				base.restorativeWhenInjured(),
				base.mildWhenInjured(),
				base.restorativeWhenTired(),
				base.mildWhenTired(),
				base.stimulatingWhenNormal(),
				base.mildWhenGloomy(),
				base.specialWhenElated(),
				base.stimulatingWhenElated(),
				walletLow,
				walletMid,
				base.affordableWhenBroke(),
				base.affordableWhenTight(),
				base.preferredBonus(),
				threshold,
				maxNeeds);
	}

	private static NeedNumbers withHunger(int mid, int high) {
		NeedNumbers base = NeedNumbers.defaults();
		return rebuilt(high, mid, base.walletLow(), base.walletMid(), base.selectionThreshold(), base.maxNeeds());
	}

	private static NeedNumbers withWallet(int low, int mid) {
		NeedNumbers base = NeedNumbers.defaults();
		return rebuilt(base.hungerHigh(), base.hungerMid(), low, mid, base.selectionThreshold(), base.maxNeeds());
	}

	private static NeedNumbers withMaxNeeds(int maxNeeds) {
		NeedNumbers base = NeedNumbers.defaults();
		return rebuilt(
				base.hungerHigh(), base.hungerMid(), base.walletLow(), base.walletMid(),
				base.selectionThreshold(), maxNeeds);
	}

	private static NeedNumbers withSelectionThreshold(int threshold) {
		NeedNumbers base = NeedNumbers.defaults();
		return rebuilt(
				base.hungerHigh(), base.hungerMid(), base.walletLow(), base.walletMid(),
				threshold, base.maxNeeds());
	}
}
