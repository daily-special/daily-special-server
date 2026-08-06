package com.dailyspecial.server.domain.visit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 오늘 상태에서 <b>이번 방문의 우선 욕구</b>를 뽑는다.
 *
 * <p>순수 함수다. 난수를 쓰지 않는다 — 무작위는 이미 상태를 만들 때 다 썼고, 욕구는 그 상태의
 * 결과일 뿐이다. 같은 상태와 같은 성향이면 언제나 같은 욕구가 나온다.
 *
 * <p><b>동점 처리가 명세의 일부다.</b> 점수가 같으면 {@link Need}의 선언 순서로 앞선 것을
 * 고른다. C# 이식본의 열거 순서가 다르면 다른 욕구가 나온다.
 */
public final class NeedResolver {

	private final NeedNumbers numbers;

	public NeedResolver(NeedNumbers numbers) {
		if (numbers == null) {
			throw new IllegalArgumentException("numbers가 없다");
		}
		this.numbers = numbers;
	}

	/** 점수가 높은 순으로 최대 {@code maxNeeds}개. 최소 하나는 반드시 나온다. */
	public List<Need> resolve(VisitState state, GuestTraits traits) {
		if (state == null) {
			throw new IllegalArgumentException("state가 없다");
		}
		if (traits == null) {
			throw new IllegalArgumentException("traits가 없다");
		}

		Map<Need, Integer> scores = score(state, traits);

		List<Need> ranked = scores.entrySet().stream()
				.filter(entry -> entry.getValue() > 0)
				.sorted(
						Comparator.<Map.Entry<Need, Integer>>comparingInt(Map.Entry::getValue)
								.reversed()
								.thenComparing(entry -> entry.getKey().ordinal()))
				.map(Map.Entry::getKey)
				.toList();

		List<Need> chosen = ranked.stream()
				.filter(need -> scores.get(need) >= numbers.selectionThreshold())
				.limit(numbers.maxNeeds())
				.toList();

		// 문턱을 넘은 것이 없어도 손님은 무언가를 원한다. 욕구가 0개면 만족도의 첫 항을
		// 계산할 수 없고, 화면에도 띄울 것이 없다.
		return chosen.isEmpty() ? ranked.stream().limit(1).toList() : chosen;
	}

	private Map<Need, Integer> score(VisitState state, GuestTraits traits) {
		Map<Need, Integer> scores = new EnumMap<>(Need.class);
		for (Need need : Need.values()) {
			scores.put(need, 0);
		}

		add(scores, Need.FILLING, hungerScore(state.hunger()));

		switch (state.condition()) {
			case INJURED -> {
				add(scores, Need.RESTORATIVE, numbers.restorativeWhenInjured());
				add(scores, Need.MILD, numbers.mildWhenInjured());
			}
			case TIRED -> {
				add(scores, Need.RESTORATIVE, numbers.restorativeWhenTired());
				add(scores, Need.MILD, numbers.mildWhenTired());
			}
			case NORMAL -> add(scores, Need.STIMULATING, numbers.stimulatingWhenNormal());
		}

		switch (state.mood()) {
			case GLOOMY -> add(scores, Need.MILD, numbers.mildWhenGloomy());
			case ELATED -> {
				add(scores, Need.SPECIAL, numbers.specialWhenElated());
				add(scores, Need.STIMULATING, numbers.stimulatingWhenElated());
			}
			case CALM -> {
				// 평온은 아무 쪽으로도 기울이지 않는다. 그것이 평온의 뜻이다.
			}
		}

		add(scores, Need.AFFORDABLE, walletScore(state.wallet()));

		// 평소 성향은 저울을 기울이기만 한다 — 결정권은 오늘 상태가 갖는다 (데이터 계약 7절).
		// 같은 상태의 두 손님이 갈리는 자리가 여기뿐이다.
		for (Need need : traits.preferredNeeds()) {
			add(scores, need, numbers.preferredBonus());
		}

		return scores;
	}

	private int hungerScore(int hunger) {
		if (hunger >= numbers.hungerHigh()) {
			return numbers.fillingWhenVeryHungry();
		}
		return hunger >= numbers.hungerMid() ? numbers.fillingWhenHungry() : 0;
	}

	private int walletScore(int wallet) {
		if (wallet <= numbers.walletLow()) {
			return numbers.affordableWhenBroke();
		}
		return wallet <= numbers.walletMid() ? numbers.affordableWhenTight() : 0;
	}

	private static void add(Map<Need, Integer> scores, Need need, int points) {
		scores.merge(need, points, Integer::sum);
	}

	/**
	 * 점수 자체를 돌려준다. <b>왜 이 욕구가 나왔는지</b>를 설명해야 할 때 쓴다.
	 *
	 * <p>밸런스를 만질 때 결과만 보면 어느 숫자를 건드려야 할지 알 수 없다.
	 */
	public Map<Need, Integer> explain(VisitState state, GuestTraits traits) {
		if (state == null || traits == null) {
			throw new IllegalArgumentException("state와 traits가 있어야 한다");
		}
		return new EnumMap<>(score(state, traits));
	}
}
