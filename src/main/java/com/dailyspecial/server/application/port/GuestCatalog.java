package com.dailyspecial.server.application.port;

import com.dailyspecial.server.domain.visit.GuestTraits;
import java.util.Optional;

/**
 * 손님 페르소나에서 <b>생성기가 필요로 하는 성향만</b> 꺼내온다.
 *
 * <p>페르소나 전체를 돌려주지 않는 것이 의도다. 이름·소개·말투·취향 구간은 데이터 계약이
 * 소유하고 클라이언트가 번들로 읽는다. 서버가 그것까지 들고 있으면 배달하고 싶어지고,
 * 그 순간 경계가 무너진다.
 */
public interface GuestCatalog {

	/** 모르는 손님이면 빈 값. 부르는 쪽이 404로 옮긴다. */
	Optional<GuestTraits> findTraits(String guestId);
}
