package com.dailyspecial.server.domain.visit;

import java.util.EnumMap;
import java.util.Map;

/**
 * 오늘 상태 생성에 쓰는 게임 수치. <b>수치를 코드에 박지 않는다</b> — 밸런스를 만질 때 여기만 만진다.
 *
 * <p>불변식은 생성 시점에 예외로 막는다. 설정이 어긋난 채로 서버가 뜨면 어긋난 손님이 나오고,
 * 그때는 원인을 찾기 어렵다.
 *
 * @param hungerMin                 허기 하한
 * @param hungerMax                 허기 상한
 * @param conditionWeights          컨디션 가중치
 * @param moodWeights               기분 가중치
 * @param walletMin                 지갑 하한
 * @param walletMax                 지갑 상한
 * @param affordableWalletMaxRatio  저렴 성향 손님의 지갑 상한 비율 (0 초과 1 이하)
 */
public record VisitNumbers(
		int hungerMin,
		int hungerMax,
		Map<Condition, Integer> conditionWeights,
		Map<Mood, Integer> moodWeights,
		int walletMin,
		int walletMax,
		double affordableWalletMaxRatio) {

	public VisitNumbers {
		if (hungerMin < 0 || hungerMin > hungerMax) {
			throw new IllegalArgumentException(
					"허기 구간이 뒤집혔다: %d~%d".formatted(hungerMin, hungerMax));
		}
		if (walletMin <= 0 || walletMin > walletMax) {
			throw new IllegalArgumentException(
					"지갑 구간이 뒤집혔다: %d~%d".formatted(walletMin, walletMax));
		}
		if (affordableWalletMaxRatio <= 0.0 || affordableWalletMaxRatio > 1.0) {
			throw new IllegalArgumentException(
					"저렴 성향 지갑 비율은 0 초과 1 이하여야 한다: " + affordableWalletMaxRatio);
		}
		conditionWeights = checkedWeights(Condition.values(), conditionWeights, "컨디션");
		moodWeights = checkedWeights(Mood.values(), moodWeights, "기분");
	}

	/**
	 * 지금 쓰는 기본값.
	 *
	 * <p><b>확정</b> — 컨디션·기분의 어휘, 지갑 스케일 8~40 (설계·로드맵 문서가 소유).
	 *
	 * <p><b>가정값</b> — 허기 구간, 컨디션·기분의 가중치, 저렴 성향 지갑 비율.
	 * 근거는 게임 수학이 아니라 코지 게임의 감각이다: 기본 상태가 흔해야 예외 상태가
	 * 그날의 사건이 된다. <b>밸런스를 볼 때 가장 먼저 만질 자리다.</b>
	 */
	public static VisitNumbers defaults() {
		Map<Condition, Integer> conditions = new EnumMap<>(Condition.class);
		conditions.put(Condition.NORMAL, 70);
		conditions.put(Condition.TIRED, 20);
		conditions.put(Condition.INJURED, 10);

		Map<Mood, Integer> moods = new EnumMap<>(Mood.class);
		moods.put(Mood.CALM, 60);
		moods.put(Mood.ELATED, 25);
		moods.put(Mood.GLOOMY, 15);

		return new VisitNumbers(0, 100, conditions, moods, 8, 40, 0.5);
	}

	/**
	 * 저렴 성향 손님의 지갑 상한.
	 *
	 * <p>손님 페르소나에 지갑 필드를 두지 않기로 한 결정의 반대편이다. 데이터 계약 7절이
	 * "손님의 지갑이 빠듯하다는 사실은 계약에 담을 수 없고, {@code preferred_needs}에
	 * {@code affordable}이 들어가는 것으로 나타난다"고 정한다. 그 성향을 지갑으로 되돌리는 자리가 여기다.
	 */
	public int affordableWalletMax() {
		return walletMin + (int) Math.round((walletMax - walletMin) * affordableWalletMaxRatio);
	}

	private static <T extends Enum<T>> Map<T, Integer> checkedWeights(
			T[] values, Map<T, Integer> weights, String label) {
		if (weights == null) {
			throw new IllegalArgumentException(label + " 가중치가 없다");
		}
		int total = 0;
		for (T value : values) {
			Integer weight = weights.get(value);
			if (weight == null) {
				throw new IllegalArgumentException("%s 가중치에 %s이(가) 빠졌다".formatted(label, value));
			}
			if (weight < 0) {
				throw new IllegalArgumentException(
						"%s 가중치가 음수다: %s=%d".formatted(label, value, weight));
			}
			total += weight;
		}
		if (total == 0) {
			throw new IllegalArgumentException(label + " 가중치 합이 0이다");
		}
		return Map.copyOf(weights);
	}
}
