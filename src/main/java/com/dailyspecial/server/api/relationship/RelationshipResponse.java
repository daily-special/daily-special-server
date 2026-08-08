package com.dailyspecial.server.api.relationship;

import com.dailyspecial.server.application.relationship.RelationshipView;
import com.dailyspecial.server.domain.relationship.Disclosure;
import java.util.List;

/**
 * 관계 응답.
 *
 * <p><b>공개된 값을 담지 않는다. 무엇이 열렸는지만 담는다.</b> 취향 구간·식이 제약·선호 욕구는
 * 안 바뀌므로 계약이고 클라이언트 번들에 있다. 서버가 그것을 배달하면 숨은 페르소나가
 * 네트워크로 나가고, 서버가 고정 콘텐츠를 들고 있어야 한다.
 */
public record RelationshipResponse(
		String saveId, String guestId, int affinity, String tier, Disclosed disclosed) {

	public static RelationshipResponse of(String saveId, String guestId, RelationshipView view) {
		Disclosure disclosure = view.disclosure();

		return new RelationshipResponse(
				saveId,
				guestId,
				view.relationship().affinity(),
				view.tier().slug(),
				new Disclosed(
						disclosure.preferredNeeds(),
						disclosure.dietary(),
						disclosure.allAxes(),
						List.copyOf(disclosure.revealedAxes())));
	}

	/**
	 * @param allAxes 참이면 클라이언트는 {@code axes}를 무시하고 모든 축을 연다.
	 *     서버가 축 어휘를 몰라도 되게 하는 장치다
	 */
	public record Disclosed(
			boolean preferredNeeds, boolean dietary, boolean allAxes, List<String> axes) {}
}
