package com.dailyspecial.server.api.relationship;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dailyspecial.server.api.ApiExceptionHandler;
import com.dailyspecial.server.application.relationship.RelationshipService;
import com.dailyspecial.server.application.relationship.RelationshipView;
import com.dailyspecial.server.application.visit.UnknownGuestException;
import com.dailyspecial.server.domain.relationship.Disclosure;
import com.dailyspecial.server.domain.relationship.Relationship;
import com.dailyspecial.server.domain.relationship.Tier;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** API 표면만 본다 — 키 표기, 어휘, 상태 코드. 규칙은 도메인 테스트가 이미 고정했다. */
@WebMvcTest(RelationshipController.class)
@Import(ApiExceptionHandler.class)
class RelationshipControllerTest {

	private static final String BASE = "/api/v1/saves/save-1/guests/guest_dusty_patrol_01";

	@Autowired private MockMvc mockMvc;

	@MockitoBean private RelationshipService service;

	private static RelationshipView view() {
		return new RelationshipView(
				new Relationship(42, Map.of("seasoning", 3)),
				Tier.FAMILIAR,
				new Disclosure(true, false, false, Set.of("seasoning")));
	}

	@Test
	@DisplayName("조회 — 키는 snake_case, 단계는 소문자")
	void reads() throws Exception {
		given(service.view(anyString(), anyString())).willReturn(view());

		mockMvc
				.perform(get(BASE + "/relationship"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.save_id").value("save-1"))
				.andExpect(jsonPath("$.guest_id").value("guest_dusty_patrol_01"))
				.andExpect(jsonPath("$.affinity").value(42))
				.andExpect(jsonPath("$.tier").value("familiar"))
				.andExpect(jsonPath("$.disclosed.preferred_needs").value(true))
				.andExpect(jsonPath("$.disclosed.dietary").value(false))
				.andExpect(jsonPath("$.disclosed.all_axes").value(false))
				.andExpect(jsonPath("$.disclosed.axes[0]").value("seasoning"));
	}

	@Test
	@DisplayName("응답에 취향 값이 실리지 않는다 — 열렸는지만 실린다")
	void neverLeaksHiddenPersona() throws Exception {
		given(service.view(anyString(), anyString())).willReturn(view());

		mockMvc
				.perform(get(BASE + "/relationship"))
				.andExpect(jsonPath("$.disclosed.ideal_ranges").doesNotExist())
				.andExpect(jsonPath("$.ideal_ranges").doesNotExist())
				.andExpect(jsonPath("$.name").doesNotExist());
	}

	@Test
	@DisplayName("방문 기록 — 만족도와 어긋난 축을 받는다")
	void recordsVisit() throws Exception {
		given(service.recordVisit(anyString(), anyString(), anyDouble(), any())).willReturn(view());

		mockMvc
				.perform(
						post(BASE + "/visits")
								.contentType(MediaType.APPLICATION_JSON)
								.content("{\"satisfaction\": 0.82, \"off_axes\": [\"seasoning\"]}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.affinity").value(42));
	}

	@Test
	@DisplayName("만족도가 0~1 밖이면 400")
	void rejectsSatisfactionOutOfRange() throws Exception {
		mockMvc
				.perform(
						post(BASE + "/visits")
								.contentType(MediaType.APPLICATION_JSON)
								.content("{\"satisfaction\": 1.4}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("invalid_request"));
	}

	@Test
	@DisplayName("만족도가 빠지면 400")
	void rejectsMissingSatisfaction() throws Exception {
		mockMvc
				.perform(post(BASE + "/visits").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("invalid_request"));
	}

	@Test
	@DisplayName("모르는 손님은 404")
	void unknownGuestIsNotFound() throws Exception {
		willThrow(new UnknownGuestException("guest_nope")).given(service).view(anyString(), anyString());

		mockMvc
				.perform(get(BASE + "/relationship"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("unknown_guest"));
	}
}
