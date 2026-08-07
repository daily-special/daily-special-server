package com.dailyspecial.server.domain.relationship;

import java.util.Map;
import java.util.TreeMap;

/**
 * 한 세이브에서 한 손님과 쌓인 관계.
 *
 * <p>이것은 <b>플레이 중에 바뀌는 상태</b>라 서버가 소유한다 (데이터 계약 5절). 손님의 이름·
 * 취향 구간 같은 것은 안 바뀌므로 계약이고 클라이언트 번들에 있다.
 *
 * @param affinity  0 이상, 설정한 상한 이하
 * @param axisHints 축별로 어긋난 요리를 몇 번 받았는가. 축 키는 계약 어휘를 그대로 쓴다
 */
public record Relationship(int affinity, Map<String, Integer> axisHints) {

	public Relationship {
		if (affinity < 0) {
			throw new IllegalArgumentException("관계는 음수가 될 수 없다: " + affinity);
		}
		if (axisHints == null) {
			throw new IllegalArgumentException("axisHints가 없다");
		}
		for (Map.Entry<String, Integer> entry : axisHints.entrySet()) {
			if (entry.getKey() == null || entry.getKey().isBlank()) {
				throw new IllegalArgumentException("축 키가 비어 있다");
			}
			if (entry.getValue() == null || entry.getValue() < 0) {
				throw new IllegalArgumentException(
						"축 힌트가 음수다: %s=%s".formatted(entry.getKey(), entry.getValue()));
			}
		}
		// 키 순서를 고정한다. 응답과 테스트가 실행마다 흔들리면 안 된다.
		axisHints = Map.copyOf(new TreeMap<>(axisHints));
	}

	/** 아직 아무 관계도 없는 손님. */
	public static Relationship none() {
		return new Relationship(0, Map.of());
	}

	public int hintsFor(String axis) {
		return axisHints.getOrDefault(axis, 0);
	}
}
