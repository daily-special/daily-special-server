package com.dailyspecial.server.domain.relationship;

import java.util.Set;
import java.util.TreeSet;

/**
 * 플레이어가 이 손님에 대해 <b>알게 된 것</b>.
 *
 * <p><b>값이 아니라 플래그다.</b> 서버는 "무엇이 열렸는지"만 말하고, 실제 값(취향 구간·식이
 * 제약·선호 욕구)은 클라이언트가 자기 번들에서 읽는다.
 *
 * <p>이렇게 나눈 이유가 둘이다. 숨은 페르소나가 네트워크로 나가지 않고, 서버가 이름·소개·
 * 구간 같은 고정 콘텐츠를 들고 있을 필요도 없어진다 — 서버는 상태만 본다.
 *
 * @param preferredNeeds 평소 성향이 열렸는가
 * @param dietary        식이 제약이 열렸는가
 * @param allAxes        <b>모든</b> 축의 이상 구간이 열렸는가. 단골이면 참이다
 * @param revealedAxes   피드백으로 개별 해금된 축. {@code allAxes}가 참이면 클라는 이것을 무시하고 전부 연다
 */
public record Disclosure(
		boolean preferredNeeds, boolean dietary, boolean allAxes, Set<String> revealedAxes) {

	public Disclosure {
		if (revealedAxes == null) {
			throw new IllegalArgumentException("revealedAxes가 없다");
		}
		// 순서를 고정한다 — 응답이 실행마다 흔들리면 안 된다.
		revealedAxes = Set.copyOf(new TreeSet<>(revealedAxes));
	}

	/**
	 * 축 목록을 서버가 들고 있지 않은 것이 의도다.
	 *
	 * <p>축 어휘는 계약이 소유하고 클라이언트 번들에 있다. 서버가 그것까지 알면 어휘가 늘 때
	 * 두 곳을 고쳐야 하고, 여기서는 알 필요가 없다 — "전부 열렸다"만 말하면 된다.
	 */
	public boolean revealsAxis(String axis) {
		return allAxes || revealedAxes.contains(axis);
	}
}
