package com.dailyspecial.server.domain.relationship;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * 관계가 어떻게 자라고 무엇이 열리는가.
 *
 * <p>순수 함수다. 스프링도 DB도 시계도 난수도 쓰지 않는다 — 같은 입력이면 언제나 같은 결과다.
 *
 * <p><b>만족도를 여기서 계산하지 않는다.</b> 만족도 엔진은 클라이언트가 소유한다. 서버가 같이
 * 가지면 두 벌이 되고, 두 벌은 반드시 갈라진다. 경계는 이렇다 — <b>클라가 계산한 것</b>(만족도,
 * 어긋난 축)을 받고, <b>상태 진행</b>(관계 증감, 공개 해금)을 여기서 정한다.
 */
public final class RelationshipRules {

	private final RelationshipNumbers numbers;

	public RelationshipRules(RelationshipNumbers numbers) {
		if (numbers == null) {
			throw new IllegalArgumentException("numbers가 없다");
		}
		this.numbers = numbers;
	}

	/**
	 * 한 번의 방문이 끝난 뒤의 관계.
	 *
	 * <p><b>연산 순서가 명세의 일부다 (이식 명세).</b> 아래 순서 그대로 계산한다.
	 *
	 * <pre>
	 *   raw   = (만족도 - 중립) * 증감폭
	 *   delta = floor(raw + 0.5)
	 * </pre>
	 *
	 * <p>{@code Math.round}를 쓰지 않는 이유가 있다. 자바의 {@code Math.round}는 음수 절반을
	 * 0 쪽으로 올리고 C#의 {@code MidpointRounding.AwayFromZero}는 반대로 내린다 —
	 * {@code -0.5}에서 갈린다. {@code floor(x + 0.5)}는 두 언어가 같다.
	 *
	 * <p>실수 연산이라 경계값은 부동소수 오차가 결정한다. 두 언어 모두 IEEE 754 배정밀도로
	 * <b>같은 순서</b>로 계산하면 같은 값이 나오므로, 순서를 바꾸지 마라. 고정 벡터가 이것을 잡는다.
	 *
	 * @param satisfaction 클라이언트가 계산한 만족도 0~1
	 * @param offAxes      이상 구간을 벗어난 축들. 축마다 힌트가 하나씩 쌓인다
	 */
	public Relationship afterVisit(
			Relationship current, double satisfaction, Collection<String> offAxes) {
		if (current == null) {
			throw new IllegalArgumentException("current가 없다");
		}
		if (offAxes == null) {
			throw new IllegalArgumentException("offAxes가 없다");
		}
		if (!(satisfaction >= 0.0) || satisfaction > 1.0) {
			// NaN도 여기서 걸린다 — 첫 비교가 거짓이 된다.
			throw new IllegalArgumentException("만족도는 0~1이어야 한다: " + satisfaction);
		}

		double raw = (satisfaction - numbers.satisfactionNeutral()) * numbers.affinityGain();
		int delta = (int) Math.floor(raw + 0.5);
		int affinity = clamp(current.affinity() + delta);

		Map<String, Integer> hints = new TreeMap<>(current.axisHints());
		for (String axis : offAxes) {
			if (axis == null || axis.isBlank()) {
				throw new IllegalArgumentException("축 키가 비어 있다");
			}
			hints.merge(axis, 1, Integer::sum);
		}

		return new Relationship(affinity, hints);
	}

	public Tier tierOf(Relationship relationship) {
		int affinity = relationship.affinity();
		if (affinity >= numbers.regularFrom()) {
			return Tier.REGULAR;
		}
		return affinity >= numbers.familiarFrom() ? Tier.FAMILIAR : Tier.STRANGER;
	}

	/**
	 * 지금 무엇이 열려 있는가.
	 *
	 * <pre>
	 *   낯선 손님   아무것도
	 *   낯이 익음   평소 성향 + 피드백이 쌓인 축
	 *   단골        평소 성향 + 식이 제약 + 모든 축
	 * </pre>
	 *
	 * <p>축이 따로 열리는 것이 이 곡선의 핵심이다. "좀 짠데요"를 여러 번 들으면 간을 알게
	 * 된다 — 관계가 얕아도 <b>관찰로</b> 좁혀지는 길이 있어야 취향 추론이 놀이가 된다.
	 */
	public Disclosure disclose(Relationship relationship) {
		if (relationship == null) {
			throw new IllegalArgumentException("relationship이 없다");
		}

		Tier tier = tierOf(relationship);
		boolean regular = tier == Tier.REGULAR;

		Set<String> revealed = new TreeSet<>();
		for (Map.Entry<String, Integer> entry : relationship.axisHints().entrySet()) {
			if (entry.getValue() >= numbers.axisRevealHints()) {
				revealed.add(entry.getKey());
			}
		}

		return new Disclosure(tier != Tier.STRANGER, regular, regular, revealed);
	}

	private int clamp(int affinity) {
		return Math.max(0, Math.min(numbers.affinityMax(), affinity));
	}
}
