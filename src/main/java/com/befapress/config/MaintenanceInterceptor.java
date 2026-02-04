package com.befapress.config;

import com.befapress.service.SettingsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class MaintenanceInterceptor implements HandlerInterceptor {

    private final SettingsService settingsService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String path = request.getRequestURI();

        // Allow critical endpoints
        if (path.startsWith("/api/v1/auth") ||
                path.startsWith("/api/v1/admin") ||
                path.startsWith("/api/v1/public/settings") ||
                path.contains("swagger") ||
                path.contains("api-docs")) {
            return true;
        }

        // Check maintenance mode
        if (settingsService.getSettings().getMaintenanceMode()) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setContentType("application/json");
            response.getWriter()
                    .write("{\"message\": \"Site is currently under maintenance.\", \"maintenance\": true}");
            return false;
        }

        return true;
    }
}
