package com.dailyspecial.server.domain.visit;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 오늘 상태를 뽑는 씨앗. 같은 세이브의 같은 날 같은 손님이면 언제나 같은 값이 나온다.
 *
 * <p><b>알고리즘 (이식 명세)</b> — 클라이언트가 C#으로 같은 값을 재현해야 한다.
 *
 * <pre>
 *   text   = saveId + ":" + dayNumber + ":" + guestId
 *   digest = SHA-256(UTF-8(text))
 *   seed   = digest의 앞 8바이트를 빅엔디언 부호 있는 64비트로 읽은 값
 * </pre>
 *
 * <p>{@code String.hashCode()}나 언어 기본 난수기의 씨앗을 쓰지 않는 이유가 여기 있다.
 * 그것들은 언어마다 다르거나 명세가 없어서, 같은 손님이 서버와 클라에서 다르게 나온다.
 * SHA-256은 어느 언어에서든 같은 값을 낸다.
 *
 * @param saveId    세이브 식별자
 * @param dayNumber 게임 내 날짜. 1부터 센다
 * @param guestId   손님 식별자. 데이터 계약의 슬러그를 그대로 쓴다
 */
public record VisitSeed(String saveId, int dayNumber, String guestId) {

	public VisitSeed {
		requireText(saveId, "saveId");
		requireText(guestId, "guestId");
		if (dayNumber < 1) {
			throw new IllegalArgumentException("dayNumber는 1 이상이어야 한다: " + dayNumber);
		}
	}

	/** 씨앗 문자열을 64비트 값으로 접는다. 위 알고리즘 그대로다. */
	public long toSeed() {
		byte[] digest = sha256(saveId + ":" + dayNumber + ":" + guestId);
		long seed = 0L;
		for (int i = 0; i < Long.BYTES; i++) {
			seed = (seed << 8) | (digest[i] & 0xFFL);
		}
		return seed;
	}

	private static byte[] sha256(String text) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException cause) {
			// SHA-256은 모든 JVM이 반드시 제공한다. 여기 오면 실행 환경이 깨진 것이다.
			throw new IllegalStateException("SHA-256을 쓸 수 없다", cause);
		}
	}

	private static void requireText(String value, String name) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(name + "이(가) 비어 있다");
		}
	}
}
