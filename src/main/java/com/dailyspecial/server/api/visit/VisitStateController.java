package com.dailyspecial.server.api.visit;

import com.dailyspecial.server.application.visit.VisitStateQuery;
import com.dailyspecial.server.domain.visit.VisitSeed;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 오늘 손님 상태 조회.
 *
 * <p>읽기 전용이고 부수 효과가 없다. 같은 경로는 언제 불러도 같은 답을 낸다 —
 * 상태를 저장하지 않고 씨앗에서 다시 계산하기 때문이다.
 */
@RestController
@RequestMapping("/api/v1")
class VisitStateController {

	private final VisitStateQuery query;

	VisitStateController(VisitStateQuery query) {
		this.query = query;
	}

	@GetMapping("/saves/{saveId}/days/{dayNumber}/guests/{guestId}/visit-state")
	VisitStateResponse visitState(
			@PathVariable String saveId, @PathVariable int dayNumber, @PathVariable String guestId) {

		VisitSeed seed = new VisitSeed(saveId, dayNumber, guestId);

		return VisitStateResponse.of(seed, query.today(seed));
	}
}
