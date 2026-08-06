package com.dailyspecial.server.api.visit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dailyspecial.server.api.ApiExceptionHandler;
import com.dailyspecial.server.application.visit.UnknownGuestException;
import com.dailyspecial.server.application.visit.VisitStateQuery;
import com.dailyspecial.server.domain.visit.Condition;
import com.dailyspecial.server.domain.visit.Mood;
import com.dailyspecial.server.domain.visit.Need;
import com.dailyspecial.server.domain.visit.TodayVisit;
import com.dailyspecial.server.domain.visit.VisitState;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * API 표면만 본다 — 키 표기, 열거 어휘, 상태 코드.
 *
 * <p>계산은 도메인 테스트가 이미 고정했으므로 여기서 다시 보지 않는다.
 */
@WebMvcTest(VisitStateController.class)
@Import(ApiExceptionHandler.class)
class VisitStateControllerTest {

	private static final String PATH = "/api/v1/saves/save-1/days/1/guests/guest_rolf/visit-state";

	@Autowired private MockMvc mockMvc;

	@MockitoBean private VisitStateQuery query;

	@Test
	@DisplayName("키는 snake_case, 열거 값은 소문자로 나간다")
	void speaksTheContractVocabulary() throws Exception {
		given(query.today(any()))
				.willReturn(
						new TodayVisit(
								new VisitState(71, Condition.TIRED, Mood.ELATED, 24),
								List.of(Need.RESTORATIVE, Need.MILD)));

		mockMvc
				.perform(get(PATH))
				.andExpect(status().isOk())
				// 요청 식별자를 되돌려 담는다
				.andExpect(jsonPath("$.save_id").value("save-1"))
				.andExpect(jsonPath("$.day_number").value(1))
				.andExpect(jsonPath("$.guest_id").value("guest_rolf"))
				// 상태
				.andExpect(jsonPath("$.hunger").value(71))
				.andExpect(jsonPath("$.wallet").value(24))
				// 자바 enum 이름(TIRED·ELATED)이 새어 나오면 안 된다
				.andExpect(jsonPath("$.condition").value("tired"))
				.andExpect(jsonPath("$.mood").value("elated"))
				// 욕구도 계약 어휘로 나간다
				.andExpect(jsonPath("$.needs[0]").value("restorative"))
				.andExpect(jsonPath("$.needs[1]").value("mild"));
	}

	@Test
	@DisplayName("모르는 손님은 404 unknown_guest")
	void unknownGuestIsNotFound() throws Exception {
		willThrow(new UnknownGuestException("guest_rolf")).given(query).today(any());

		mockMvc
				.perform(get(PATH))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("unknown_guest"));
	}

	@Test
	@DisplayName("0 이하 날짜는 400 invalid_request")
	void nonPositiveDayIsRejected() throws Exception {
		// VisitSeed의 불변식이 컨트롤러에서 걸린다 — 유스케이스까지 가지 않는다
		mockMvc
				.perform(get("/api/v1/saves/save-1/days/0/guests/guest_rolf/visit-state"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("invalid_request"));
	}

	@Test
	@DisplayName("날짜 자리에 숫자가 아닌 값이 오면 400")
	void nonNumericDayIsRejected() throws Exception {
		mockMvc
				.perform(get("/api/v1/saves/save-1/days/tomorrow/guests/guest_rolf/visit-state"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("invalid_request"));
	}
}
