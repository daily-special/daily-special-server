package com.dailyspecial.server.infra.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 공유 비밀 하나로 API를 막는다.
 *
 * <p><b>이것은 인증이 아니라 자물쇠다.</b> 누가 부르는지 구별하지 않고 "부를 자격이 있는가"만
 * 본다. 플레이어 식별과 진짜 인증은 서버 설계 4-2절대로 나중에 붙인다 — 지금 Spring Security를
 * 넣으면 프로토타입 속도가 눈에 띄게 떨어진다.
 *
 * <p>그럼에도 이것이 필요한 이유는, 이 서버가 공개 인터넷에 올라가는 순간 <b>URL만 알면 누구나
 * 남의 관계를 읽고 바꿀 수 있기</b> 때문이다. 저장소가 public이라 엔드포인트도 다 공개돼 있다.
 *
 * <p>키를 비워두면 필터가 통째로 꺼진다 — 로컬 개발과 테스트가 키 없이 돌아야 한다.
 *
 * <p><b>{@code @Component}를 붙이지 않는다.</b> 붙이면 {@code @WebMvcTest} 슬라이스가 이 필터를
 * 끌어가고, 컨트롤러 테스트마다 배포 설정을 알아야 한다. 조립은 {@link SecurityConfiguration}이 한다.
 */
public class ApiKeyFilter extends OncePerRequestFilter {

	static final String HEADER = "X-Api-Key";

	private final byte[] expected;

	public ApiKeyFilter(ApiKeyProperties properties) {
		this.expected =
				properties.key() == null || properties.key().isBlank()
						? null
						: properties.key().getBytes(StandardCharsets.UTF_8);
	}

	/** 헬스 체크는 열어둔다. 로드밸런서나 배포 스크립트가 키를 알 이유가 없다. */
	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return expected == null || request.getRequestURI().startsWith("/actuator/health");
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {

		if (!matches(request.getHeader(HEADER))) {
			response.setStatus(HttpStatus.UNAUTHORIZED.value());
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			response.setCharacterEncoding(StandardCharsets.UTF_8.name());
			response.getWriter().write("{\"error\":\"unauthorized\",\"message\":\"API 키가 없거나 틀렸다\"}");
			return;
		}

		chain.doFilter(request, response);
	}

	/**
	 * 길이와 내용을 <b>상수 시간</b>으로 비교한다.
	 *
	 * <p>{@code equals}로 비교하면 첫 글자부터 순서대로 보다가 다르면 즉시 빠져나와, 응답 시간
	 * 차이로 키를 한 글자씩 알아낼 수 있다. 자물쇠 하나뿐인 서버라 그 자물쇠는 제대로 걸어야 한다.
	 */
	private boolean matches(String provided) {
		if (provided == null) {
			return false;
		}
		return MessageDigest.isEqual(expected, provided.getBytes(StandardCharsets.UTF_8));
	}
}
