package com.dailyspecial.server.api.visit;

import com.dailyspecial.server.domain.visit.Condition;
import com.dailyspecial.server.domain.visit.Mood;
import com.dailyspecial.server.domain.visit.Need;
import com.dailyspecial.server.domain.visit.TodayVisit;
import com.dailyspecial.server.domain.visit.VisitSeed;
import com.dailyspecial.server.domain.visit.VisitState;
import java.util.List;

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
		int wallet,
		List<String> needs) {

	public static VisitStateResponse of(VisitSeed seed, TodayVisit visit) {
		VisitState state = visit.state();

		return new VisitStateResponse(
				seed.saveId(),
				seed.dayNumber(),
				seed.guestId(),
				state.hunger(),
				slug(state.condition()),
				slug(state.mood()),
				state.wallet(),
				visit.needs().stream().map(Need::slug).toList());
	}

	/**
	 * `default`를 두지 않는다 — 어휘가 늘면 컴파일이 깨져서 여기를 반드시 고치게 된다.
	 *
	 * <p>{@link Need}는 반대로 표기를 열거 안에 들고 있다. 욕구는 콘텐츠 JSON에서
	 * <b>읽어 들이기도</b> 하므로 왕복이 필요하고, 표기가 두 곳에 있으면 그 왕복이 깨진다.
	 * 컨디션·기분은 읽어 들일 일이 없어 이쪽 방식이 더 안전하다.
	 */
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
