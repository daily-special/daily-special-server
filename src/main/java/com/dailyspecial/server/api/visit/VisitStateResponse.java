package com.dailyspecial.server.api.visit;

import com.dailyspecial.server.domain.visit.Condition;
import com.dailyspecial.server.domain.visit.Mood;
import com.dailyspecial.server.domain.visit.VisitSeed;
import com.dailyspecial.server.domain.visit.VisitState;

/**
 * 오늘 손님 상태 응답.
 *
 * <p>키는 `snake_case`로 나간다 — 전역 Jackson 설정이 record 성분 이름을 바꿔준다
 * (데이터 계약 1-1절).
 *
 * <p><b>열거 값을 도메인 enum 그대로 내보내지 않는다.</b> 도메인은 자바 관례대로 대문자지만
 * 계약 1-2절은 소문자 `snake_case`를 요구한다. 그 변환이 여기서 일어나므로 도메인은
 * Jackson을 몰라도 되고(ArchUnit이 그걸 강제한다), 자바 이름이 API로 새지도 않는다.
 *
 * <p>요청 식별자를 되돌려 담는 것은 응답만 보고도 무엇에 대한 답인지 알게 하려는 것이다.
 */
public record VisitStateResponse(
		String saveId,
		int dayNumber,
		String guestId,
		int hunger,
		String condition,
		String mood,
		int wallet) {

	public static VisitStateResponse of(VisitSeed seed, VisitState state) {
		return new VisitStateResponse(
				seed.saveId(),
				seed.dayNumber(),
				seed.guestId(),
				state.hunger(),
				slug(state.condition()),
				slug(state.mood()),
				state.wallet());
	}

	/** `default`를 두지 않는다 — 어휘가 늘면 컴파일이 깨져서 여기를 반드시 고치게 된다. */
	private static String slug(Condition condition) {
		return switch (condition) {
			case NORMAL -> "normal";
			case INJURED -> "injured";
			case TIRED -> "tired";
		};
	}

	private static String slug(Mood mood) {
		return switch (mood) {
			case GLOOMY -> "gloomy";
			case CALM -> "calm";
			case ELATED -> "elated";
		};
	}
}
