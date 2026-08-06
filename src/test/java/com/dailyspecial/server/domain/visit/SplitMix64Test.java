package com.dailyspecial.server.domain.visit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * SplitMix64 이식 명세.
 *
 * <p>C# 이식본은 이 케이스를 그대로 옮겨 전부 통과시켜야 한다.
 */
class SplitMix64Test {

	@Test
	@DisplayName("씨앗 0의 수열이 공개된 정본 벡터와 같다")
	void matchesReferenceVectors() {
		// SplitMix64의 널리 쓰이는 정본 구현이 내는 값이다.
		// 16진수로는 0xE220A8397B1DCDAF, 0x6E789E6AA1B965F4, 0x06C45D188009454F.
		long[] expected = {
			-2152535657050944081L,
			7960286522194355700L,
			487617019471545679L,
		};

		SplitMix64 random = new SplitMix64(0L);
		long[] actual = {random.nextLong(), random.nextLong(), random.nextLong()};

		assertArrayEquals(expected, actual);
	}

	@Test
	@DisplayName("같은 씨앗이면 같은 수열이 나온다")
	void isDeterministic() {
		long seed = -9089002578166466824L;

		SplitMix64 first = new SplitMix64(seed);
		SplitMix64 second = new SplitMix64(seed);

		for (int i = 0; i < 100; i++) {
			assertEquals(first.nextLong(), second.nextLong(), i + "번째 값이 갈렸다");
		}
	}

	@Test
	@DisplayName("nextInt는 0 이상 상한 미만을 낸다")
	void staysWithinBound() {
		SplitMix64 random = new SplitMix64(42L);

		for (int i = 0; i < 10_000; i++) {
			int value = random.nextInt(101);
			assertTrue(value >= 0 && value < 101, "범위를 벗어났다: " + value);
		}
	}

	@Test
	@DisplayName("상한이 0 이하면 거부한다")
	void rejectsNonPositiveBound() {
		SplitMix64 random = new SplitMix64(0L);

		assertThrows(IllegalArgumentException.class, () -> random.nextInt(0));
		assertThrows(IllegalArgumentException.class, () -> random.nextInt(-1));
	}
}
