package com.dailyspecial.server.application.visit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dailyspecial.server.application.port.GuestCatalog;
import com.dailyspecial.server.domain.visit.GuestTraits;
import com.dailyspecial.server.domain.visit.Need;
import com.dailyspecial.server.domain.visit.NeedNumbers;
import com.dailyspecial.server.domain.visit.NeedResolver;
import com.dailyspecial.server.domain.visit.TodayVisit;
import com.dailyspecial.server.domain.visit.VisitNumbers;
import com.dailyspecial.server.domain.visit.VisitSeed;
import com.dailyspecial.server.domain.visit.VisitState;
import com.dailyspecial.server.domain.visit.VisitStateGenerator;
import com.dailyspecial.server.infra.content.JsonGuestCatalog;
import tools.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 스프링 컨텍스트를 띄우지 않는다. 유스케이스는 그냥 객체다. */
class VisitStateQueryTest {

	private static final VisitStateGenerator GENERATOR = new VisitStateGenerator(VisitNumbers.defaults());
	private static final NeedResolver NEEDS = new NeedResolver(NeedNumbers.defaults());

	@Test
	@DisplayName("손님 성향을 찾아 생성기에 넘긴다")
	void passesTraitsToTheGenerator() {
		VisitSeed seed = new VisitSeed("save-1", 2, "guest_thrifty");
		VisitStateQuery query = new VisitStateQuery(fixed("guest_thrifty", true), GENERATOR, NEEDS);

		VisitState actual = query.today(seed).state();

		assertEquals(GENERATOR.generate(seed, GuestTraits.of(Need.AFFORDABLE)), actual);
	}

	@Test
	@DisplayName("모르는 손님이면 UnknownGuestException을 던진다")
	void rejectsUnknownGuest() {
		VisitStateQuery query = new VisitStateQuery(fixed("guest_known", false), GENERATOR, NEEDS);

		UnknownGuestException thrown =
				assertThrows(
						UnknownGuestException.class,
						() -> query.today(new VisitSeed("save-1", 1, "guest_missing")));

		assertEquals("guest_missing", thrown.guestId());
	}

	@Test
	@DisplayName("같은 요청은 몇 번을 물어도 같은 답을 낸다")
	void isDeterministic() {
		VisitStateQuery query = new VisitStateQuery(fixed("guest_known", false), GENERATOR, NEEDS);
		VisitSeed seed = new VisitSeed("save-1", 5, "guest_known");

		TodayVisit first = query.today(seed);
		for (int i = 0; i < 20; i++) {
			assertEquals(first, query.today(seed));
		}
	}

	@Test
	@DisplayName("실제 스냅샷의 손님으로도 상태가 나온다")
	void worksWithTheRealSnapshot() {
		// 가짜 카탈로그만 쓰면 스냅샷의 guest_id가 바뀌어도 초록이 뜬다.
		VisitStateQuery query =
				new VisitStateQuery(new JsonGuestCatalog(new ObjectMapper()), GENERATOR, NEEDS);
		VisitNumbers numbers = VisitNumbers.defaults();

		VisitState state = query.today(new VisitSeed("save-1", 1, "guest_dusty_patrol_01")).state();

		assertTrue(state.hunger() >= numbers.hungerMin() && state.hunger() <= numbers.hungerMax());
		// 이 손님은 선호 욕구에 affordable이 있어 지갑 상한이 낮다.
		assertTrue(state.wallet() <= numbers.affordableWalletMax(), "지갑=" + state.wallet());
	}

	private static GuestCatalog fixed(String guestId, boolean prefersAffordable) {
		Map<String, GuestTraits> known = Map.of(guestId, prefersAffordable ? GuestTraits.of(Need.AFFORDABLE) : GuestTraits.of());
		return id -> Optional.ofNullable(known.get(id));
	}
}
