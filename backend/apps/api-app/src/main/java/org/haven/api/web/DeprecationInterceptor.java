package org.haven.api.web;

import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Map;

/**
 * Interceptor to add deprecation headers for deprecated API endpoints
 */
@Component
public class DeprecationInterceptor implements HandlerInterceptor {

    // Mapping of deprecated endpoints to their migration information
    private static final Map<String, DeprecationInfo> DEPRECATED_ENDPOINTS = Map.of(
        "/cases/{id}/notes", new DeprecationInfo(
            "Case notes endpoint is deprecated. Use /api/v1/service-episodes for service documentation.",
            LocalDate.of(2025, 12, 31),
            "https://docs.haven.org/migration/service-episodes"
        ),
        "/cases/{id}/services", new DeprecationInfo(
            "Case services endpoint is deprecated. Use /api/v1/service-episodes for service management.",
            LocalDate.of(2025, 12, 31),
            "https://docs.haven.org/migration/service-episodes"
        )
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod handlerMethod) {
            Method method = handlerMethod.getMethod();

            // Check if method is deprecated
            if (method.isAnnotationPresent(Deprecated.class)) {
                addDeprecationHeaders(request, response);
            }

            // Check if endpoint path is in deprecated list
            String path = getCanonicalPath(request.getRequestURI());
            DeprecationInfo deprecationInfo = DEPRECATED_ENDPOINTS.get(path);
            if (deprecationInfo != null) {
                addDeprecationHeaders(response, deprecationInfo);
            }
        }

        return true;
    }

    private void addDeprecationHeaders(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("X-Deprecated", "true");
        response.setHeader("X-Deprecation-Date", LocalDate.now().toString());

        // Add default deprecation message if not already set
        if (response.getHeader("X-Deprecation-Message") == null) {
            response.setHeader("X-Deprecation-Message", "This endpoint is deprecated. Please check documentation for alternatives.");
        }
    }

    private void addDeprecationHeaders(HttpServletResponse response, DeprecationInfo info) {
        response.setHeader("X-Deprecated", "true");
        response.setHeader("X-Deprecation-Message", info.message());
        response.setHeader("X-Sunset", info.sunsetDate().toString());
        response.setHeader("X-Migration-Guide", info.migrationGuide());
        response.setHeader("X-Deprecation-Date", LocalDate.now().toString());
    }

    private String getCanonicalPath(String requestURI) {
        // Convert actual path to template pattern for matching
        // This is a simplified version - in production, you'd want more sophisticated path matching
        return requestURI.replaceAll("/cases/[^/]+/notes", "/cases/{id}/notes")
                        .replaceAll("/cases/[^/]+/services", "/cases/{id}/services");
    }

    private record DeprecationInfo(
        String message,
        LocalDate sunsetDate,
        String migrationGuide
    ) {}
}