package org.haven.api.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for API interceptors and handlers
 */
@Configuration
public class WebInterceptorConfig implements WebMvcConfigurer {

    private final DeprecationInterceptor deprecationInterceptor;

    public WebInterceptorConfig(DeprecationInterceptor deprecationInterceptor) {
        this.deprecationInterceptor = deprecationInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Add deprecation interceptor for all API endpoints
        registry.addInterceptor(deprecationInterceptor)
                .addPathPatterns("/api/**", "/cases/**");
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}