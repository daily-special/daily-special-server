package com.dailyspecial.server.domain.visit;

import java.util.List;

/**
 * 손님의 오늘 방문 전체 — 상태와 그 상태가 낳은 욕구.
 *
 * <p>상태와 욕구를 한 record로 합치지 않은 이유는 <b>욕구가 상태에서 유도되기 때문</b>이다.
 * 합쳐두면 둘 중 어느 것이 원인이고 어느 것이 결과인지 흐려지고, 상태만 필요한 자리에서도
 * 욕구 규칙을 끌고 다니게 된다.
 *
 * @param state 허기·컨디션·기분·지갑
 * @param needs 이번 방문의 우선 욕구. 최소 1개, 최대는 설정이 정한다
 */
public record TodayVisit(VisitState state, List<Need> needs) {

	public TodayVisit {
		if (state == null) {
			throw new IllegalArgumentException("state가 없다");
		}
		if (needs == null || needs.isEmpty()) {
			throw new IllegalArgumentException("욕구가 하나도 없다 — 손님은 무언가를 원한다");
		}
		needs = List.copyOf(needs);
	}
}
