package com.dailyspecial.server.domain.relationship;

/**
 * 관계 단계. 설계 문서의 정보 공개 곡선 셋과 같다.
 *
 * <p><b>선언 순서가 명세의 일부다.</b> 이식본의 순서가 다르면 비교가 어긋난다.
 */
public enum Tier {
	/** 첫 방문 무렵. 욕구 힌트만 있고 취향은 추측한다 */
	STRANGER("stranger"),
	/** 낯이 익다. 평소 성향과, 피드백으로 좁혀진 축이 보인다 */
	FAMILIAR("familiar"),
	/** 단골. 취향 구간과 식이 제약이 열린다 */
	REGULAR("regular");

	private final String slug;

	Tier(String slug) {
		this.slug = slug;
	}

	/** 계약 표기(소문자). API 응답이 이 값을 쓴다. */
	public String slug() {
		return slug;
	}
}
