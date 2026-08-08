package com.dailyspecial.server.infra.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * 자물쇠가 실제로 <b>잠기는지</b> 본다.
 *
 * <p>"키를 넣으면 열린다"만 확인하면 한 번도 잠긴 적이 없어도 초록이 뜬다.
 */
class ApiKeyFilterTest {

	private static final String KEY = "s3cret-key";

	private final ApiKeyFilter filter = new ApiKeyFilter(new ApiKeyProperties(KEY));

	@Test
	@DisplayName("키가 맞으면 통과시킨다")
	void passesWithTheRightKey() throws Exception {
		MockHttpServletRequest request = get("/api/v1/saves/s/guests/g/relationship");
		request.addHeader(ApiKeyFilter.HEADER, KEY);
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = Mockito.mock(FilterChain.class);

		filter.doFilter(request, response, chain);

		Mockito.verify(chain).doFilter(request, response);
		assertEquals(200, response.getStatus());
	}

	@Test
	@DisplayName("키가 없거나 틀리면 401이고 체인을 타지 않는다")
	void blocksWithoutTheRightKey() throws Exception {
		for (String provided : new String[] {null, "", "wrong", KEY + "x", KEY.toUpperCase()}) {
			MockHttpServletRequest request = get("/api/v1/saves/s/guests/g/relationship");
			if (provided != null) {
				request.addHeader(ApiKeyFilter.HEADER, provided);
			}
			MockHttpServletResponse response = new MockHttpServletResponse();
			FilterChain chain = Mockito.mock(FilterChain.class);

			filter.doFilter(request, response, chain);

			assertEquals(401, response.getStatus(), "키=" + provided);
			Mockito.verifyNoInteractions(chain);
		}
	}

	@Test
	@DisplayName("헬스 체크는 키 없이 열린다")
	void healthIsOpen() {
		// 배포 스크립트나 로드밸런서가 키를 알 이유가 없다.
		assertTrue(filter.shouldNotFilter(get("/actuator/health")));
		assertTrue(filter.shouldNotFilter(get("/actuator/health/readiness")));
		assertFalse(filter.shouldNotFilter(get("/api/v1/saves/s/guests/g/relationship")));
	}

	@Test
	@DisplayName("키를 비워두면 자물쇠를 걸지 않는다 — 로컬과 테스트가 그 상태다")
	void disabledWhenNoKeyConfigured() {
		for (String key : new String[] {null, "", "  "}) {
			ApiKeyFilter open = new ApiKeyFilter(new ApiKeyProperties(key));

			assertTrue(open.shouldNotFilter(get("/api/v1/saves/s/guests/g/relationship")), "키=" + key);
		}
	}

	private static MockHttpServletRequest get(String uri) {
		return new MockHttpServletRequest("GET", uri);
	}
}
