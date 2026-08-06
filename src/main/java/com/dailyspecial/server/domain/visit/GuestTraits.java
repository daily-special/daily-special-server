package com.dailyspecial.server.domain.visit;

import java.util.EnumSet;
import java.util.Set;

/**
 * 오늘 상태와 욕구를 뽑는 데 <b>실제로 필요한 손님 정보만</b> 담는다. 페르소나 전체를
 * 끌어오지 않는다.
 *
 * <p>페르소나는 데이터 계약이 소유하고 클라이언트가 번들로 읽는다. 도메인이 그 모양을 알게 되면
 * 계약이 바뀔 때마다 규칙이 흔들린다. 여기서는 계산에 쓰는 것만 받고, 페르소나에서 이 값을
 * 뽑아내는 일은 바깥 층이 한다.
 *
 * @param preferredNeeds 평소 성향. <b>이번 방문의 욕구가 아니다</b> (데이터 계약 7절).
 *     오늘의 욕구는 상태가 정하고, 이 값은 저울을 기울이기만 한다
 */
public record GuestTraits(Set<Need> preferredNeeds) {

	public GuestTraits {
		if (preferredNeeds == null) {
			throw new IllegalArgumentException("preferredNeeds가 없다");
		}
		preferredNeeds = Set.copyOf(preferredNeeds);
	}

	public static GuestTraits of(Need... needs) {
		return new GuestTraits(needs.length == 0 ? EnumSet.noneOf(Need.class) : EnumSet.of(needs[0], needs));
	}

	/** 지갑이 빠듯한 손님인가. 페르소나에 지갑 필드를 두지 않기로 한 결정의 반대편이다. */
	public boolean prefersAffordable() {
		return preferredNeeds.contains(Need.AFFORDABLE);
	}
}
