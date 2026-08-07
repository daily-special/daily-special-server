package com.dailyspecial.server.domain.relationship;

/**
 * 관계 진행에 쓰는 수치. <b>수치를 코드에 박지 않는다</b> — 밸런스를 만질 때 여기만 만진다.
 *
 * <p><b>전부 가정값이다.</b> 설계 문서에 정보 공개 곡선의 세 단계는 있지만 수치는 없다.
 * 여기 값은 밸런스 시뮬레이션이 아니라 <b>곡선이 그럴듯한가</b>로 골랐다. 만족 1.0을 거듭하면
 * <b>두 번째 방문에 낯이 익고 다섯 번째에 단골</b>이 된다. 한 손님이 매일 오지는 않으므로
 * 14일짜리 게임에서 단골 하나를 얻는 데 게임의 절반쯤이 든다.
 *
 * <p>그 곡선은 {@code RelationshipRulesTest}가 고정한다 — 수치가 가정값이라 "이만큼이면
 * 그럴듯한가"가 유일한 근거이고, 근거가 테스트 밖에 있으면 숫자를 만질 때 사라진다.
 *
 * @param affinityMax        관계 상한
 * @param satisfactionNeutral 이 만족도에서 관계가 그대로다. 아래면 깎인다
 * @param affinityGain       만족도 차이에 곱하는 값
 * @param familiarFrom       이 값부터 낯이 익다
 * @param regularFrom        이 값부터 단골이다
 * @param axisRevealHints    한 축에 이만큼 피드백이 쌓이면 그 축의 구간이 열린다
 */
public record RelationshipNumbers(
		int affinityMax,
		double satisfactionNeutral,
		double affinityGain,
		int familiarFrom,
		int regularFrom,
		int axisRevealHints) {

	public RelationshipNumbers {
		if (affinityMax < 1) {
			throw new IllegalArgumentException("관계 상한은 1 이상이어야 한다: " + affinityMax);
		}
		if (satisfactionNeutral < 0.0 || satisfactionNeutral > 1.0) {
			throw new IllegalArgumentException(
					"중립 만족도는 0~1이어야 한다: " + satisfactionNeutral);
		}
		if (affinityGain <= 0.0) {
			throw new IllegalArgumentException("관계 증감 폭은 양수여야 한다: " + affinityGain);
		}
		if (familiarFrom < 1 || regularFrom <= familiarFrom || regularFrom > affinityMax) {
			throw new IllegalArgumentException(
					"단계 문턱이 뒤집혔다: familiar=%d regular=%d max=%d"
							.formatted(familiarFrom, regularFrom, affinityMax));
		}
		if (axisRevealHints < 1) {
			throw new IllegalArgumentException("축 공개 힌트 수는 1 이상이어야 한다: " + axisRevealHints);
		}
	}

	public static RelationshipNumbers defaults() {
		return new RelationshipNumbers(100, 0.4, 20.0, 20, 60, 3);
	}
}
