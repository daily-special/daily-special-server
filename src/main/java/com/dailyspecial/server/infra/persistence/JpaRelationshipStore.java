package com.dailyspecial.server.infra.persistence;

import com.dailyspecial.server.application.port.RelationshipStore;
import com.dailyspecial.server.domain.relationship.Relationship;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * 관계를 Postgres에 담는다. <b>도메인과 저장 모양 사이를 옮기는 것이 이 클래스의 전부다.</b>
 */
@Component
class JpaRelationshipStore implements RelationshipStore {

	private final GuestRelationshipRepository repository;

	JpaRelationshipStore(GuestRelationshipRepository repository) {
		this.repository = repository;
	}

	@Override
	public Relationship find(String saveId, String guestId) {
		return repository
				.findById(new GuestRelationshipEntity.Key(saveId, guestId))
				.map(entity -> new Relationship(entity.affinity(), entity.axisHints()))
				// 없는 것과 0인 것을 구분하지 않는다. 첫 방문 전의 손님은 그냥 관계가 없는 것이다.
				.orElseGet(Relationship::none);
	}

	@Override
	public void save(String saveId, String guestId, Relationship relationship) {
		GuestRelationshipEntity entity =
				Optional.ofNullable(
								repository
										.findById(new GuestRelationshipEntity.Key(saveId, guestId))
										.orElse(null))
						.orElseGet(() -> new GuestRelationshipEntity(saveId, guestId));

		entity.apply(relationship.affinity(), relationship.axisHints());
		repository.save(entity);
	}
}
