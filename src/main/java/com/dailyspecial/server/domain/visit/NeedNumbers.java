package com.dailyspecial.server.domain.visit;

/**
 * 상태에서 욕구를 뽑는 점수표. <b>수치를 코드에 박지 않는다</b> — 밸런스를 만질 때 여기만 만진다.
 *
 * <p><b>전부 가정값이다.</b> 설계 문서에는 예시 셋만 있었다 — 몸이 안 좋으면 회복과 순한,
 * 지갑이 부족하면 저렴, 들뜨면 특별과 포만. 여기 숫자는 <b>그 셋을 재현하도록 맞춘 값</b>이지
 * 밸런스 시뮬레이션에서 나온 값이 아니다. 조정할 때 가장 먼저 볼 자리다.
 *
 * <p>조합 표(컨디션 × 기분 × 허기 × 지갑)를 만들지 않은 이유는 손이 못 미치기 때문이다.
 * 욕구가 하나 늘면 표가 통째로 커지지만, 점수는 줄 하나만 는다.
 */
public record NeedNumbers(
		int hungerHigh,
		int hungerMid,
		int fillingWhenVeryHungry,
		int fillingWhenHungry,
		int restorativeWhenInjured,
		int mildWhenInjured,
		int restorativeWhenTired,
		int mildWhenTired,
		int stimulatingWhenNormal,
		int mildWhenGloomy,
		int specialWhenElated,
		int stimulatingWhenElated,
		int walletLow,
		int walletMid,
		int affordableWhenBroke,
		int affordableWhenTight,
		int preferredBonus,
		int selectionThreshold,
		int maxNeeds) {

	public NeedNumbers {
		if (hungerMid >= hungerHigh) {
			throw new IllegalArgumentException(
					"허기 문턱이 뒤집혔다: %d >= %d".formatted(hungerMid, hungerHigh));
		}
		if (walletLow >= walletMid) {
			throw new IllegalArgumentException(
					"지갑 문턱이 뒤집혔다: %d >= %d".formatted(walletLow, walletMid));
		}
		if (selectionThreshold < 1) {
			throw new IllegalArgumentException("선택 문턱은 1 이상이어야 한다: " + selectionThreshold);
		}
		if (maxNeeds < 1) {
			throw new IllegalArgumentException("욕구는 최소 1개는 나와야 한다: " + maxNeeds);
		}
	}

	/**
	 * 지금 쓰는 기본값.
	 *
	 * <p>설계 문서의 예시 셋이 그대로 나오는지는 테스트가 고정한다. 숫자를 만지면 그 테스트가
	 * 먼저 빨개진다 — 그때 예시가 여전히 맞는지 다시 보라는 뜻이다.
	 */
	public static NeedNumbers defaults() {
		return new NeedNumbers(
				70, // 허기 높음
				40, // 허기 보통
				2, // 많이 고프면 포만
				1, // 좀 고프면 포만
				3, // 부상 → 회복
				2, // 부상 → 순한
				2, // 피로 → 회복
				2, // 피로 → 순한
				1, // 정상 → 자극
				2, // 우울 → 순한
				3, // 들뜸 → 특별
				1, // 들뜸 → 자극
				16, // 지갑 빠듯
				24, // 지갑 보통
				3, // 빠듯하면 저렴
				1, // 보통이면 저렴
				1, // 평소 성향 가산점
				2, // 이 점수부터 고른다
				2); // 최대 욕구 수
	}
}
