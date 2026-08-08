package com.dailyspecial.server.infra.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 자물쇠를 조립한다. 필터 자체는 스프링을 거의 모르고, 여기서만 컨테이너에 올라간다. */
@Configuration
@EnableConfigurationProperties(ApiKeyProperties.class)
class SecurityConfiguration {

	@Bean
	ApiKeyFilter apiKeyFilter(ApiKeyProperties properties) {
		return new ApiKeyFilter(properties);
	}
}
