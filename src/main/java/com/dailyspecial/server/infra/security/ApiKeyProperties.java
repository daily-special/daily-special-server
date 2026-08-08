package com.dailyspecial.server.infra.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param key 비어 있으면 자물쇠를 걸지 않는다. 로컬과 테스트가 그 상태로 돈다
 */
@ConfigurationProperties(prefix = "daily-special.api")
public record ApiKeyProperties(String key) {}
