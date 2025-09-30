package org.haven.api.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = "app.security.enabled", havingValue = "true", matchIfMissing = true)
public class SecurityConfig {
    
    private final AppCorsProperties corsProperties;
    
    public SecurityConfig(AppCorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, ClientRegistrationRepository clientRegistrations) throws Exception {
        http
            .cors(Customizer.withDefaults())
            // NOTE: For BFF using session cookies, consider enabling CSRF with CookieCsrfTokenRepository.
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/actuator/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/h2-console/**",
                    "/error",
                    // OAuth2 login endpoints
                    "/oauth2/**",
                    "/auth/login"
                ).permitAll()
                .anyRequest().authenticated()
            )
            // Allow browser login via OIDC (BFF style)
            .oauth2Login(Customizer.withDefaults())
            // Also allow bearer JWT for API/use-cases that send tokens directly
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            // Configure OIDC logout to propagate to Keycloak
            .logout(logout -> logout
                .logoutUrl("/auth/logout")
                // Allow GET for logout to simplify local dev (avoid CSRF on POST)
                .logoutRequestMatcher(new org.springframework.security.web.util.matcher.AntPathRequestMatcher("/auth/logout", "GET"))
                .logoutSuccessHandler(oidcLogoutSuccessHandler(clientRegistrations))
            );

        return http.build();
    }

    private OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler(ClientRegistrationRepository clientRegistrations) {
        OidcClientInitiatedLogoutSuccessHandler handler = new OidcClientInitiatedLogoutSuccessHandler(clientRegistrations);
        // Post-logout redirect; adjust as needed for your frontend
        handler.setPostLogoutRedirectUri("{baseUrl}/");
        return handler;
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());
        configuration.setAllowedMethods(corsProperties.getAllowedMethods());
        configuration.setAllowedHeaders(corsProperties.getAllowedHeaders());
        configuration.setAllowCredentials(corsProperties.isAllowCredentials());
        configuration.setMaxAge(corsProperties.getMaxAge());
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
