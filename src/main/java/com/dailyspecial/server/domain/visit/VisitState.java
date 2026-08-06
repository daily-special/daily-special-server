package com.dailyspecial.server.domain.visit;

/**
 * 손님이 오늘 어떤 상태로 오는가. 씨앗에서 결정적으로 생성된다.
 *
 * <p>저장하지 않는다 — 생성이 순수하고 결정적이라 같은 씨앗이면 언제든 같은 값이 나온다.
 * 테이블 하나와 일관성 문제 하나를 통째로 없애는 대신, 수치를 조정하면 과거 날짜의 상태도
 * 같이 바뀐다. 지난 날을 되돌아보는 기능이 생기면 그때 스냅샷을 뜬다.
 *
 * @param hunger    허기. {@link VisitNumbers}의 구간 안
 * @param condition 컨디션
 * @param mood      기분
 * @param wallet    오늘 쓸 수 있는 돈. 요리 고정가와 견줘 예산 적합을 판정한다
 */
public record VisitState(int hunger, Condition condition, Mood mood, int wallet) {

	public VisitState {
		if (condition == null) {
			throw new IllegalArgumentException("condition이 없다");
		}
		if (mood == null) {
			throw new IllegalArgumentException("mood가 없다");
		}
	}
}
