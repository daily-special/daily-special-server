package com.dailyspecial.server.api.relationship;

import com.dailyspecial.server.application.relationship.RelationshipService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 관계 조회와 방문 기록. */
@RestController
@RequestMapping("/api/v1/saves/{saveId}/guests/{guestId}")
class RelationshipController {

	private final RelationshipService service;

	RelationshipController(RelationshipService service) {
		this.service = service;
	}

	@GetMapping("/relationship")
	RelationshipResponse relationship(@PathVariable String saveId, @PathVariable String guestId) {
		return RelationshipResponse.of(saveId, guestId, service.view(saveId, guestId));
	}

	/**
	 * 방문 하나를 기록한다. <b>멱등하지 않다</b> — 같은 요청을 두 번 보내면 관계가 두 번 자란다.
	 *
	 * <p>하루에 한 번만 부르는 것은 지금 클라이언트의 책임이다. 재시도 안전이 필요해지면
	 * 날짜를 키로 받아 하루 한 번으로 막는다 — 지금은 그 요구가 없다.
	 */
	@PostMapping("/visits")
	RelationshipResponse recordVisit(
			@PathVariable String saveId,
			@PathVariable String guestId,
			@Valid @RequestBody RecordVisitRequest request) {

		return RelationshipResponse.of(
				saveId,
				guestId,
				service.recordVisit(
						saveId, guestId, request.satisfaction(), request.offAxesOrEmpty()));
	}
}
