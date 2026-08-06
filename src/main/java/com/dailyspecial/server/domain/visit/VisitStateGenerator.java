package com.dailyspecial.server.domain.visit;

import java.util.Map;

/**
 * 씨앗에서 손님의 오늘 상태를 결정적으로 뽑는다. 서버가 존재하는 이유의 알맹이다.
 *
 * <p>순수 함수다 — 스프링도 DB도 시계도 모른다. 같은 씨앗과 같은 수치면 언제 어디서 불러도
 * 같은 값이 나온다.
 *
 * <p><b>뽑는 순서가 명세의 일부다 (이식 명세).</b> 난수기 하나에서 차례로 뽑으므로 순서를
 * 바꾸면 모든 값이 달라진다. 순서는 <b>허기 → 컨디션 → 기분 → 지갑</b>이다.
 *
 * <p><b>열거 선언 순서도 명세의 일부다.</b> 가중 추첨이 선언 순서대로 누적하므로,
 * C# 이식에서 {@code enum} 상수 순서가 다르면 다른 값이 나온다.
 */
public final class VisitStateGenerator {

	private final VisitNumbers numbers;

	public VisitStateGenerator(VisitNumbers numbers) {
		if (numbers == null) {
			throw new IllegalArgumentException("numbers가 없다");
		}
		this.numbers = numbers;
	}

	public VisitState generate(VisitSeed seed, GuestTraits traits) {
		if (seed == null) {
			throw new IllegalArgumentException("seed가 없다");
		}
		if (traits == null) {
			throw new IllegalArgumentException("traits가 없다");
		}

		SplitMix64 random = new SplitMix64(seed.toSeed());

		int hunger = between(random, numbers.hungerMin(), numbers.hungerMax());
		Condition condition = pick(random, Condition.values(), numbers.conditionWeights());
		Mood mood = pick(random, Mood.values(), numbers.moodWeights());
		int wallet = between(random, numbers.walletMin(), walletMaxFor(traits));

		return new VisitState(hunger, condition, mood, wallet);
	}

	/** 저렴 성향이면 상한을 낮춘다. 하한은 그대로다 — 가난한 손님도 최소한은 쓴다. */
	private int walletMaxFor(GuestTraits traits) {
		return traits.prefersAffordable() ? numbers.affordableWalletMax() : numbers.walletMax();
	}

	/** 양 끝을 포함하는 구간에서 하나 뽑는다. */
	private static int between(SplitMix64 random, int min, int max) {
		return min + random.nextInt(max - min + 1);
	}

	/** 선언 순서대로 가중치를 누적해 고른다. */
	private static <T extends Enum<T>> T pick(SplitMix64 random, T[] values, Map<T, Integer> weights) {
		int total = 0;
		for (T value : values) {
			total += weights.get(value);
		}

		int roll = random.nextInt(total);
		int accumulated = 0;
		for (T value : values) {
			accumulated += weights.get(value);
			if (roll < accumulated) {
				return value;
			}
		}

		// 가중치 합이 total이므로 roll < total이면 반드시 위에서 반환된다.
		throw new IllegalStateException("가중 추첨이 값을 고르지 못했다: roll=" + roll + ", total=" + total);
	}
}
