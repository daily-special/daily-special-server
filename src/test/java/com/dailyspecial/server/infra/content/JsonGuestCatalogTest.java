package com.dailyspecial.server.infra.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dailyspecial.server.domain.visit.GuestTraits;
import com.dailyspecial.server.domain.visit.Need;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 실제 스냅샷을 읽는다. 가짜 파일로 바꾸면 스냅샷이 깨져도 초록이 뜬다. */
class JsonGuestCatalogTest {

	private static final java.util.List<String> GUEST_IDS = java.util.List.of(
			"guest_dusty_patrol_01",
			"guest_pale_clerk_02",
			"guest_bright_courier_03",
			"guest_weary_caravaner_04",
			"guest_proud_assessor_05",
			"guest_green_healer_06",
			"guest_iron_apprentice_07",
			"guest_quiet_mapmaker_08");

	private final JsonGuestCatalog catalog = new JsonGuestCatalog(new ObjectMapper());

	@Test
	@DisplayName("스냅샷의 손님을 전부 읽는다")
	void loadsEveryGuest() {
		assertEquals(8, catalog.size());
	}

	@Test
	@DisplayName("선호 욕구에 affordable이 있으면 지갑이 빠듯한 손님이다")
	void marksThriftyGuests() {
		assertTrue(prefersAffordable("guest_dusty_patrol_01"));
		assertTrue(prefersAffordable("guest_quiet_mapmaker_08"));
	}

	@Test
	@DisplayName("affordable이 없는 손님은 표시되지 않는다")
	void leavesOtherGuestsAlone() {
		assertFalse(prefersAffordable("guest_pale_clerk_02"));
		assertFalse(prefersAffordable("guest_green_healer_06"));
	}

	@Test
	@DisplayName("모르는 손님이면 빈 값을 준다 — 예외를 던지지 않는다")
	void returnsEmptyForUnknownGuest() {
		// 404로 옮기는 것은 API 층의 몫이다. 어댑터는 사실만 말한다.
		assertTrue(catalog.findTraits("guest_does_not_exist").isEmpty());
		assertTrue(catalog.findTraits("").isEmpty());
	}

	@Test
	@DisplayName("선호 욕구를 어휘로 읽어 온다")
	void parsesPreferredNeeds() {
		// 슬러그를 그대로 들고 다니면 오타가 런타임까지 살아남는다.
		assertEquals(
				java.util.Set.of(Need.FILLING, Need.AFFORDABLE),
				catalog.findTraits("guest_dusty_patrol_01").orElseThrow().preferredNeeds());
	}

	@Test
	@DisplayName("스냅샷의 모든 손님이 욕구를 하나 이상 갖는다")
	void everyGuestHasAtLeastOnePreferredNeed() {
		// 하나도 없으면 서버가 오늘의 욕구를 뽑을 근거가 약해진다 (계약 7절).
		for (String guestId : GUEST_IDS) {
			assertFalse(
					catalog.findTraits(guestId).orElseThrow().preferredNeeds().isEmpty(),
					guestId + "의 선호 욕구가 비었다");
		}
	}

	private boolean prefersAffordable(String guestId) {
		return catalog
				.findTraits(guestId)
				.map(GuestTraits::prefersAffordable)
				.orElseThrow(() -> new AssertionError("스냅샷에 없는 손님이다: " + guestId));
	}
}
