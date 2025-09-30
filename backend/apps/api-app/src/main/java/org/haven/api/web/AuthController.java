package org.haven.api.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class AuthController {

    @GetMapping("/auth/login")
    public RedirectView login() {
        // Redirect to Spring Security's OAuth2 login entry point for the 'keycloak' registration
        return new RedirectView("/oauth2/authorization/keycloak");
    }

    @GetMapping("/auth/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("authenticated", false));
        }

        // Try to extract from OIDC user first (session-based login)
        if (authentication instanceof OAuth2AuthenticationToken oAuth2Token && oAuth2Token.getPrincipal() instanceof OidcUser oidcUser) {
            Map<String, Object> body = Map.of(
                "authenticated", true,
                "principalType", "oidc",
                "username", oidcUser.getPreferredUsername(),
                "name", oidcUser.getFullName(),
                "email", oidcUser.getEmail(),
                "roles", authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList(),
                "claims", oidcUser.getClaims()
            );
            return ResponseEntity.ok(body);
        }

        // Fallback: bearer token (resource server)
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            Map<String, Object> body = Map.of(
                "authenticated", true,
                "principalType", "jwt",
                "subject", jwt.getSubject(),
                "roles", authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList(),
                "claims", jwt.getClaims()
            );
            return ResponseEntity.ok(body);
        }

        // Generic principal
        Map<String, Object> body = Map.of(
            "authenticated", true,
            "principalType", authentication.getClass().getSimpleName(),
            "name", authentication.getName(),
            "roles", authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList())
        );
        return ResponseEntity.ok(body);
    }
}

