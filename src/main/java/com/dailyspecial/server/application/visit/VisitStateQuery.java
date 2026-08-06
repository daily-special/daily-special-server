package com.dailyspecial.server.application.visit;

import com.dailyspecial.server.application.port.GuestCatalog;
import com.dailyspecial.server.domain.visit.GuestTraits;
import com.dailyspecial.server.domain.visit.VisitSeed;
import com.dailyspecial.server.domain.visit.VisitState;
import com.dailyspecial.server.domain.visit.VisitStateGenerator;
import org.springframework.stereotype.Service;

/**
 * 오늘 손님의 상태를 돌려준다.
 *
 * <p>읽기 전용이고 트랜잭션이 없다 — 저장하는 것이 없기 때문이다. 생성이 순수하고 결정적이라
 * 같은 요청은 언제 와도 같은 응답을 낸다.
 */
@Service
public class VisitStateQuery {

	private final GuestCatalog guests;
	private final VisitStateGenerator generator;

	public VisitStateQuery(GuestCatalog guests, VisitStateGenerator generator) {
		this.guests = guests;
		this.generator = generator;
	}

	public VisitState today(VisitSeed seed) {
		GuestTraits traits =
				guests.findTraits(seed.guestId()).orElseThrow(() -> new UnknownGuestException(seed.guestId()));

		return generator.generate(seed, traits);
	}
}
