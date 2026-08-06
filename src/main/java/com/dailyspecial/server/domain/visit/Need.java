package com.dailyspecial.server.domain.visit;

/**
 * 욕구 어휘. <b>데이터 계약이 소유한다</b> — 요리의 욕구 태그와 같은 말을 쓴다.
 *
 * <p><b>선언 순서가 명세의 일부다.</b> 점수가 같을 때 이 순서로 앞선 것을 고르므로,
 * C# 이식본의 순서가 다르면 다른 욕구가 나온다.
 *
 * <p>계약의 어휘가 늘면 여기도 늘려야 한다. 빠뜨리면 콘텐츠 스냅샷을 읽을 때 기동이
 * 막힌다 — {@code JsonGuestCatalog}가 모르는 값을 거부한다.
 */
public enum Need {
	/** 포만 — 배를 든든히 채우고 싶다 */
	FILLING("filling"),
	/** 회복 — 지치거나 다친 몸을 달래고 싶다 */
	RESTORATIVE("restorative"),
	/** 순한 — 속에 부담이 가지 않는 것 */
	MILD("mild"),
	/** 자극 — 맵거나 진한, 정신이 번쩍 드는 것 */
	STIMULATING("stimulating"),
	/** 저렴 — 지갑 사정이 빠듯하다 */
	AFFORDABLE("affordable"),
	/** 특별 — 평소와 다른 것 */
	SPECIAL("special");

	private final String slug;

	Need(String slug) {
		this.slug = slug;
	}

	/** 계약이 쓰는 소문자 표기. API 응답과 콘텐츠 JSON이 이 값을 쓴다. */
	public String slug() {
		return slug;
	}

	/** 계약 표기를 어휘로 되돌린다. 모르는 값이면 빈 값 — 부르는 쪽이 어떻게 다룰지 정한다. */
	public static Need fromSlug(String slug) {
		for (Need need : values()) {
			if (need.slug.equals(slug)) {
				return need;
			}
		}
		throw new IllegalArgumentException("계약에 없는 욕구다: " + slug);
	}
}
