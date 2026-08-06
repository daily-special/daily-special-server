package com.dailyspecial.server.infra.config;

import com.dailyspecial.server.domain.visit.VisitNumbers;
import com.dailyspecial.server.domain.visit.VisitStateGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 도메인 객체를 스프링 컨테이너에 올린다.
 *
 * <p>도메인 클래스에 `@Component`를 붙이지 않는 것이 요점이다. 그러면 도메인이 스프링을 알게
 * 되고, 규약 1절과 ArchUnit이 그것을 막는다. 조립은 바깥에서 한다.
 */
@Configuration
class DomainConfiguration {

	@Bean
	VisitNumbers visitNumbers() {
		return VisitNumbers.defaults();
	}

	@Bean
	VisitStateGenerator visitStateGenerator(VisitNumbers visitNumbers) {
		return new VisitStateGenerator(visitNumbers);
	}
}
