package com.dailyspecial.server.infra.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dailyspecial.server.TestcontainersConfiguration;
import com.dailyspecial.server.application.port.RelationshipStore;
import com.dailyspecial.server.domain.relationship.Relationship;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * 진짜 Postgres에 붙는다. 인메모리로 대체하지 않는다 (규약 4절).
 *
 * <p>Flyway가 만든 스키마와 JPA 매핑이 어긋나면 <b>여기서 기동이 막힌다</b> —
 * {@code ddl-auto=validate}라 그것이 이 테스트의 첫 번째 값이다.
 */
@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaRelationshipStore.class})
class JpaRelationshipStoreTest {

	@Autowired private RelationshipStore store;

	@Test
	@DisplayName("처음 보는 손님은 관계가 없다 — 예외가 아니다")
	void unknownPairStartsAtNone() {
		assertEquals(Relationship.none(), store.find("save-1", "guest_dusty_patrol_01"));
	}

	@Test
	@DisplayName("관계와 축 힌트가 그대로 돌아온다")
	void roundTrips() {
		Relationship saved = new Relationship(42, Map.of("seasoning", 2, "heat", 1));

		store.save("save-1", "guest_pale_clerk_02", saved);

		assertEquals(saved, store.find("save-1", "guest_pale_clerk_02"));
	}

	@Test
	@DisplayName("다시 저장하면 덮어쓴다 — 행이 늘지 않는다")
	void updatesInPlace() {
		store.save("save-1", "guest_green_healer_06", new Relationship(10, Map.of("heat", 1)));
		store.save("save-1", "guest_green_healer_06", new Relationship(25, Map.of("heat", 3)));

		Relationship found = store.find("save-1", "guest_green_healer_06");

		assertEquals(25, found.affinity());
		assertEquals(3, found.hintsFor("heat"));
	}

	@Test
	@DisplayName("세이브가 다르면 다른 관계다")
	void savesAreIsolated() {
		store.save("save-1", "guest_bright_courier_03", new Relationship(70, Map.of()));

		assertEquals(0, store.find("save-2", "guest_bright_courier_03").affinity());
	}

	@Test
	@DisplayName("힌트를 지우면 자식 행도 사라진다")
	void hintsCanShrink() {
		store.save("save-9", "guest_iron_apprentice_07", new Relationship(5, Map.of("heat", 2)));
		store.save("save-9", "guest_iron_apprentice_07", new Relationship(5, Map.of()));

		assertTrue(store.find("save-9", "guest_iron_apprentice_07").axisHints().isEmpty());
	}
}
