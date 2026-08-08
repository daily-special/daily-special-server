package com.dailyspecial.server.application.port;

import com.dailyspecial.server.domain.relationship.Relationship;

/**
 * 관계 상태를 읽고 쓴다.
 *
 * <p>포트가 도메인 언어로 말한다 — 엔티티도 리포지토리도 여기 나오지 않는다. 저장 방식이
 * 바뀌어도 유스케이스는 그대로다.
 */
public interface RelationshipStore {

	/** 아직 아무 관계도 없으면 {@link Relationship#none()}. 없는 것과 0인 것을 구분하지 않는다. */
	Relationship find(String saveId, String guestId);

	void save(String saveId, String guestId, Relationship relationship);
}
