package com.dailyspecial.server.domain.visit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * 씨앗 접기 이식 명세.
 *
 * <p>C# 이식본이 같은 입력에서 같은 64비트 값을 내야 한다. 여기가 갈리면 서버와 클라의
 * 손님이 통째로 달라진다.
 */
class VisitSeedTest {

	@ParameterizedTest(name = "{0} / {1}일차 / {2}")
	@DisplayName("고정 벡터 — SHA-256 앞 8바이트를 빅엔디언으로 읽는다")
	@CsvSource({
		"save-1, 1, guest_rolf, -9089002578166466824",
		"save-1, 2, guest_rolf,   185519666428114252",
		"save-2, 1, guest_rolf, -5797122563683052569",
	})
	void matchesGoldenVectors(String saveId, int dayNumber, String guestId, long expected) {
		assertEquals(expected, new VisitSeed(saveId, dayNumber, guestId).toSeed());
	}

	@Test
	@DisplayName("같은 입력이면 언제나 같은 씨앗이 나온다")
	void isDeterministic() {
		VisitSeed seed = new VisitSeed("save-1", 7, "guest_mira");

		assertEquals(seed.toSeed(), seed.toSeed());
		assertEquals(seed.toSeed(), new VisitSeed("save-1", 7, "guest_mira").toSeed());
	}

	@Test
	@DisplayName("세 축 중 하나만 달라도 씨앗이 달라진다")
	void everyAxisChangesTheSeed() {
		long base = new VisitSeed("save-1", 1, "guest_rolf").toSeed();

		assertNotEquals(base, new VisitSeed("save-2", 1, "guest_rolf").toSeed(), "세이브");
		assertNotEquals(base, new VisitSeed("save-1", 2, "guest_rolf").toSeed(), "날짜");
		assertNotEquals(base, new VisitSeed("save-1", 1, "guest_mira").toSeed(), "손님");
	}

	@Test
	@DisplayName("빈 식별자와 0 이하 날짜를 거부한다")
	void rejectsBadInput() {
		assertThrows(IllegalArgumentException.class, () -> new VisitSeed(null, 1, "guest_rolf"));
		assertThrows(IllegalArgumentException.class, () -> new VisitSeed("  ", 1, "guest_rolf"));
		assertThrows(IllegalArgumentException.class, () -> new VisitSeed("save-1", 1, ""));
		assertThrows(IllegalArgumentException.class, () -> new VisitSeed("save-1", 0, "guest_rolf"));
	}
}
