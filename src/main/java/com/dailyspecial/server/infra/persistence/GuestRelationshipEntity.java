package com.dailyspecial.server.infra.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 관계의 저장 모양. <b>도메인 {@code Relationship}과 다른 클래스다</b> (규약 1절).
 *
 * <p>붙여두면 스키마를 바꿀 때 규칙이 흔들리고, 규칙을 검증하려고 컨테이너를 띄우게 된다.
 * 값을 옮기는 매퍼가 생기는 것이 그 대가다.
 *
 * <p>스키마는 Flyway가 소유하고 JPA는 검증만 한다({@code ddl-auto=validate}). 여기 매핑이
 * 마이그레이션과 어긋나면 <b>기동에서 막힌다</b>.
 */
@Entity
@Table(name = "guest_relationship")
class GuestRelationshipEntity {

	@EmbeddedId private Key key;

	@Column(name = "affinity", nullable = false)
	private int affinity;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(
			name = "guest_relationship_axis_hint",
			joinColumns = {
				@JoinColumn(name = "save_id", referencedColumnName = "save_id"),
				@JoinColumn(name = "guest_id", referencedColumnName = "guest_id")
			})
	@MapKeyColumn(name = "axis")
	@Column(name = "hints", nullable = false)
	private Map<String, Integer> axisHints = new LinkedHashMap<>();

	protected GuestRelationshipEntity() {
		// JPA용
	}

	GuestRelationshipEntity(String saveId, String guestId) {
		this.key = new Key(saveId, guestId);
	}

	int affinity() {
		return affinity;
	}

	Map<String, Integer> axisHints() {
		return axisHints;
	}

	void apply(int affinity, Map<String, Integer> hints) {
		this.affinity = affinity;
		// 새 맵으로 갈아끼우지 않는다 — 하이버네이트가 추적하는 컬렉션을 놓친다.
		this.axisHints.clear();
		this.axisHints.putAll(hints);
	}

	/** 세이브 × 손님이 곧 식별자다. 대리키를 두지 않는다 — 자연키가 이미 유일하다. */
	@Embeddable
	static class Key implements java.io.Serializable {

		@Column(name = "save_id", nullable = false, length = 64)
		private String saveId;

		@Column(name = "guest_id", nullable = false, length = 64)
		private String guestId;

		protected Key() {
			// JPA용
		}

		Key(String saveId, String guestId) {
			this.saveId = saveId;
			this.guestId = guestId;
		}

		@Override
		public boolean equals(Object other) {
			return other instanceof Key that
					&& java.util.Objects.equals(saveId, that.saveId)
					&& java.util.Objects.equals(guestId, that.guestId);
		}

		@Override
		public int hashCode() {
			return java.util.Objects.hash(saveId, guestId);
		}
	}
}
