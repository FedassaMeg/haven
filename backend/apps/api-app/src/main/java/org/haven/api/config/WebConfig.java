package org.haven.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    // CORS is now configured in SecurityConfig using CorsConfigurationSource
    // This ensures CORS is applied before Spring Security filters
}