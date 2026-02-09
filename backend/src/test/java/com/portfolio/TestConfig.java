package com.portfolio;

import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration
public class TestConfig {
    // passwordEncoder는 SecurityConfig에서 제공하므로 중복 정의 제거
    // Spring Boot 3.3+ 에서 BeanDefinitionOverrideException 방지
}
