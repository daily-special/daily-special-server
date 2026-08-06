package com.dailyspecial.server.domain.visit;

/** 손님의 오늘 컨디션. 어휘는 설계 문서가 소유한다 — 정상 · 부상 · 피로. */
public enum Condition {
	/** 정상 */
	NORMAL,
	/** 부상 */
	INJURED,
	/** 피로 */
	TIRED
}
