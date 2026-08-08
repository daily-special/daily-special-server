package com.dailyspecial.server.application.relationship;

import com.dailyspecial.server.application.port.GuestCatalog;
import com.dailyspecial.server.application.port.RelationshipStore;
import com.dailyspecial.server.application.visit.UnknownGuestException;
import com.dailyspecial.server.domain.relationship.Relationship;
import com.dailyspecial.server.domain.relationship.RelationshipRules;
import java.util.Collection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관계를 읽고, 방문 결과로 갱신한다.
 *
 * <p><b>만족도를 여기서 계산하지 않는다.</b> 엔진은 클라이언트가 소유한다 — 서버가 같이
 * 가지면 두 벌이 되고 두 벌은 갈라진다. 이쪽은 <b>클라가 계산한 것</b>을 받아
 * <b>상태 진행</b>만 정한다.
 */
@Service
public class RelationshipService {

	private final RelationshipStore store;
	private final GuestCatalog guests;
	private final RelationshipRules rules;

	public RelationshipService(
			RelationshipStore store, GuestCatalog guests, RelationshipRules rules) {
		this.store = store;
		this.guests = guests;
		this.rules = rules;
	}

	@Transactional(readOnly = true)
	public RelationshipView view(String saveId, String guestId) {
		requireKnownGuest(guestId);

		return viewOf(store.find(saveId, guestId));
	}

	/** 방문 하나가 끝났다. 관계가 자라거나 깎이고, 무언가 열릴 수 있다. */
	@Transactional
	public RelationshipView recordVisit(
			String saveId, String guestId, double satisfaction, Collection<String> offAxes) {
		requireKnownGuest(guestId);

		Relationship updated =
				rules.afterVisit(store.find(saveId, guestId), satisfaction, offAxes);
		store.save(saveId, guestId, updated);

		return viewOf(updated);
	}

	private RelationshipView viewOf(Relationship relationship) {
		return new RelationshipView(
				relationship, rules.tierOf(relationship), rules.disclose(relationship));
	}

	/**
	 * 콘텐츠 스냅샷에 없는 손님이면 막는다.
	 *
	 * <p>안 막으면 오타 하나가 조용히 새 관계 행을 만든다. 그 행은 아무 손님에게도 안 붙어서
	 * 영원히 발견되지 않는다.
	 */
	private void requireKnownGuest(String guestId) {
		if (guests.findTraits(guestId).isEmpty()) {
			throw new UnknownGuestException(guestId);
		}
	}
}
