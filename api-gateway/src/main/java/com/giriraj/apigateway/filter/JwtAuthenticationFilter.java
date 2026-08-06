package com.giriraj.apigateway.filter;

import com.giriraj.apigateway.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    private static final String USER_ID_HEADER =
            "X-User-ID";

    private static final String USER_ROLE_HEADER =
            "X-User-Role";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if (isPublicRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authorizationHeader =
                request.getHeader(
                        HttpHeaders.AUTHORIZATION
                );

        if (authorizationHeader == null
                || !authorizationHeader.startsWith(
                "Bearer "
        )) {

            sendUnauthorized(response);
            return;
        }

        String token = authorizationHeader
                .substring(7)
                .trim();

        if (token.isEmpty()) {
            sendUnauthorized(response);
            return;
        }

        Claims claims;
        Object userIdClaim;
        String role;


        try {
            claims = jwtService.parseToken(token);

            userIdClaim =
                    claims.get("userId");

            role = claims.get(
                    "role",
                    String.class
            );

        } catch (JwtException
                 | IllegalArgumentException exception) {

            sendUnauthorized(response);
            return;
        }

        if (!(userIdClaim instanceof Number userId)
                || role == null
                || role.isBlank()) {

            sendUnauthorized(response);
            return;
        }

        /*
         * Product create, update and delete
         * operations require ADMIN role.
         */
        if (isProductWriteRequest(request)
                && !"ADMIN".equals(role)) {

            sendForbidden(response);
            return;
        }

        long authenticatedUserId =
                userId.longValue();

        /*
         * CUSTOMER can only GET or PUT their own profile.
         * ADMIN can access every User endpoint.
         */
        if (isUserRequestForbidden(
                request,
                authenticatedUserId,
                role
        )) {

            sendForbidden(response);
            return;
        }


        UserHeaderRequestWrapper wrappedRequest =
                new UserHeaderRequestWrapper(
                        request,
                        String.valueOf(
                                authenticatedUserId
                        ),
                        role
                );


        filterChain.doFilter(
                wrappedRequest,
                response
        );
    }

    private boolean isProductWriteRequest(
            HttpServletRequest request
    ) {

        String path = request.getRequestURI();
        String method = request.getMethod();

        boolean productPath =
                path.equals("/api/products")
                        || path.startsWith(
                        "/api/products/"
                );

        if (!productPath) {
            return false;
        }

        return HttpMethod.POST.matches(method)
                || HttpMethod.PUT.matches(method)
                || HttpMethod.PATCH.matches(method)
                || HttpMethod.DELETE.matches(method);
    }

    private boolean isUserRequestForbidden(
            HttpServletRequest request,
            long authenticatedUserId,
            String role
    ) {

        String path = request.getRequestURI();
        String method = request.getMethod();

        boolean userPath =
                path.equals("/api/users")
                        || path.startsWith(
                        "/api/users/"
                );

        if (!userPath) {
            return false;
        }


        if ("ADMIN".equals(role)) {
            return false;
        }


        if (path.equals("/api/users")
                || path.equals("/api/users/")) {

            return true;
        }


        if (HttpMethod.DELETE.matches(method)) {
            return true;
        }

        String userPathPrefix =
                "/api/users/";

        if (!path.startsWith(userPathPrefix)) {
            return true;
        }

        String requestedId =
                path.substring(
                        userPathPrefix.length()
                );

        if (requestedId.isBlank()
                || requestedId.contains("/")) {

            return true;
        }

        try {
            long requestedUserId =
                    Long.parseLong(requestedId);

            boolean allowedMethod =
                    HttpMethod.GET.matches(method)
                            || HttpMethod.PUT.matches(method);

            if (!allowedMethod) {
                return true;
            }

            return requestedUserId
                    != authenticatedUserId;

        } catch (NumberFormatException exception) {

            return true;
        }
    }

    private boolean isPublicRequest(
            HttpServletRequest request
    ) {

        String path = request.getRequestURI();
        String method = request.getMethod();


        if (HttpMethod.OPTIONS.matches(method)) {
            return true;
        }


        if (HttpMethod.GET.matches(method)
                && path.equals("/checkout.html")) {

            return true;
        }


        if (path.startsWith("/api/auth/")) {
            return true;
        }


        if (HttpMethod.GET.matches(method)
                && (path.equals("/api/products")
                || path.startsWith(
                "/api/products/"
        ))) {

            return true;
        }


        return path.equals("/actuator/health")
                || path.equals("/actuator/info");
    }

    private void sendUnauthorized(
            HttpServletResponse response
    ) throws IOException {

        response.setStatus(
                HttpServletResponse.SC_UNAUTHORIZED
        );

        response.setContentType(
                MediaType.APPLICATION_JSON_VALUE
        );

        response.getWriter().write(
                """
                {
                  "success": false,
                  "status": 401,
                  "error": "Unauthorized",
                  "message": "Valid authentication token is required"
                }
                """
        );
    }

    private void sendForbidden(
            HttpServletResponse response
    ) throws IOException {

        response.setStatus(
                HttpServletResponse.SC_FORBIDDEN
        );

        response.setContentType(
                MediaType.APPLICATION_JSON_VALUE
        );

        response.getWriter().write(
                """
                {
                  "success": false,
                  "status": 403,
                  "error": "Forbidden",
                  "message": "You do not have permission to access this resource"
                }
                """
        );
    }


    private static class UserHeaderRequestWrapper
            extends HttpServletRequestWrapper {

        private final String userId;
        private final String role;

        UserHeaderRequestWrapper(
                HttpServletRequest request,
                String userId,
                String role
        ) {

            super(request);
            this.userId = userId;
            this.role = role;
        }

        @Override
        public String getHeader(String name) {

            if (USER_ID_HEADER.equalsIgnoreCase(name)) {
                return userId;
            }

            if (USER_ROLE_HEADER.equalsIgnoreCase(name)) {
                return role;
            }

            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(
                String name
        ) {

            if (USER_ID_HEADER.equalsIgnoreCase(name)) {

                return Collections.enumeration(
                        List.of(userId)
                );
            }

            if (USER_ROLE_HEADER.equalsIgnoreCase(name)) {

                return Collections.enumeration(
                        List.of(role)
                );
            }

            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {

            Set<String> headerNames =
                    new TreeSet<>(
                            String.CASE_INSENSITIVE_ORDER
                    );

            Enumeration<String> existingHeaders =
                    super.getHeaderNames();

            while (existingHeaders.hasMoreElements()) {

                headerNames.add(
                        existingHeaders.nextElement()
                );
            }

            headerNames.add(USER_ID_HEADER);
            headerNames.add(USER_ROLE_HEADER);

            return Collections.enumeration(
                    headerNames
            );
        }
    }
}
