package com.dailyspecial.server.domain.visit;

/**
 * SplitMix64 난수기. 결정적 생성에 쓴다.
 *
 * <p><b>왜 직접 구현하는가 (이식 명세)</b> — 자바의 {@code SplittableRandom}이나
 * {@code Random}을 쓰면 C#에서 같은 수열을 못 만든다. SplitMix64는 상수까지 공개된
 * 알고리즘이라 어느 언어로든 열 줄이면 같은 값이 나온다.
 *
 * <pre>
 *   state += 0x9E3779B97F4A7C15
 *   z = state
 *   z = (z XOR (z 논리우측 30)) * 0xBF58476D1CE4E5B9
 *   z = (z XOR (z 논리우측 27)) * 0x94D049BB133111EB
 *   결과 = z XOR (z 논리우측 31)
 * </pre>
 *
 * <p>곱셈은 64비트 오버플로를 그대로 버린다(C#에서는 {@code unchecked} 문맥).
 * 우측 시프트는 부호를 채우지 않는 <b>논리</b> 시프트다 — C#에서는 {@code ulong}으로 다뤄야 한다.
 */
public final class SplitMix64 {

	private static final long GOLDEN_GAMMA = 0x9E3779B97F4A7C15L;
	private static final long MIX_1 = 0xBF58476D1CE4E5B9L;
	private static final long MIX_2 = 0x94D049BB133111EBL;

	private long state;

	public SplitMix64(long seed) {
		this.state = seed;
	}

	public long nextLong() {
		long z = (state += GOLDEN_GAMMA);
		z = (z ^ (z >>> 30)) * MIX_1;
		z = (z ^ (z >>> 27)) * MIX_2;
		return z ^ (z >>> 31);
	}

	/**
	 * {@code [0, boundExclusive)} 범위의 값을 뽑는다.
	 *
	 * <p>거부 표집(rejection sampling) 없이 나머지 연산만 쓴다. 그 때문에 아주 작은 편향이
	 * 남지만, 여기서 쓰는 상한은 기껏해야 수백이라 편향이 관측되지 않는다. 대신 **이식이
	 * 단순해진다** — 거부 표집은 언어마다 구현이 갈리는 자리다.
	 */
	public int nextInt(int boundExclusive) {
		if (boundExclusive <= 0) {
			throw new IllegalArgumentException("상한은 양수여야 한다: " + boundExclusive);
		}
		return (int) Math.floorMod(nextLong(), (long) boundExclusive);
	}
}
