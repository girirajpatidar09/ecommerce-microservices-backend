package com.giriraj.apigateway.filter;

import com.giriraj.apigateway.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private Claims claims;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {

        filter = new JwtAuthenticationFilter(
                jwtService
        );
    }

    @Test
    void publicAuthRequest_shouldNotRequireToken()
            throws Exception {

        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        "POST",
                        "/api/auth/login"
                );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                filterChain
        );

        verify(filterChain).doFilter(
                same(request),
                same(response)
        );

        verifyNoInteractions(jwtService);

        assertEquals(
                200,
                response.getStatus()
        );
    }

    @Test
    void protectedRequest_withoutToken_shouldReturn401()
            throws Exception {

        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        "GET",
                        "/api/cart"
                );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                filterChain
        );

        assertEquals(
                401,
                response.getStatus()
        );

        assertTrue(
                response.getContentAsString()
                        .contains("Unauthorized")
        );

        verifyNoInteractions(
                jwtService,
                filterChain
        );
    }

    @Test
    void protectedRequest_withInvalidToken_shouldReturn401()
            throws Exception {

        MockHttpServletRequest request =
                authenticatedRequest(
                        "GET",
                        "/api/cart",
                        "bad-token"
                );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        when(jwtService.parseToken("bad-token"))
                .thenThrow(
                        new MalformedJwtException(
                                "Invalid JWT"
                        )
                );

        filter.doFilter(
                request,
                response,
                filterChain
        );

        assertEquals(
                401,
                response.getStatus()
        );

        verifyNoInteractions(filterChain);
    }

    @Test
    void customer_productWrite_shouldReturn403()
            throws Exception {

        MockHttpServletRequest request =
                authenticatedRequest(
                        "POST",
                        "/api/products",
                        "valid-token"
                );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        stubValidToken("CUSTOMER");

        filter.doFilter(
                request,
                response,
                filterChain
        );

        assertEquals(
                403,
                response.getStatus()
        );

        verifyNoInteractions(filterChain);
    }

    @Test
    void admin_productWrite_shouldBeAllowedAndUseJwtHeaders()
            throws Exception {

        MockHttpServletRequest request =
                authenticatedRequest(
                        "POST",
                        "/api/products",
                        "valid-token"
                );

        /*
         * Fake identity supplied by client.
         */
        request.addHeader(
                "X-User-ID",
                "999"
        );

        request.addHeader(
                "X-User-Role",
                "CUSTOMER"
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        stubValidToken("ADMIN");

        filter.doFilter(
                request,
                response,
                filterChain
        );

        HttpServletRequest forwardedRequest =
                captureForwardedRequest(response);

        assertEquals(
                "1",
                forwardedRequest.getHeader(
                        "X-User-ID"
                )
        );

        assertEquals(
                "ADMIN",
                forwardedRequest.getHeader(
                        "X-User-Role"
                )
        );

        assertEquals(
                200,
                response.getStatus()
        );
    }

    @Test
    void customer_ownUserProfile_shouldBeAllowed()
            throws Exception {

        MockHttpServletRequest request =
                authenticatedRequest(
                        "GET",
                        "/api/users/1",
                        "valid-token"
                );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        stubValidToken("CUSTOMER");

        filter.doFilter(
                request,
                response,
                filterChain
        );

        HttpServletRequest forwardedRequest =
                captureForwardedRequest(response);

        assertEquals(
                "1",
                forwardedRequest.getHeader(
                        "X-User-ID"
                )
        );
    }

    @Test
    void customer_otherUserProfile_shouldReturn403()
            throws Exception {

        MockHttpServletRequest request =
                authenticatedRequest(
                        "GET",
                        "/api/users/2",
                        "valid-token"
                );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        stubValidToken("CUSTOMER");

        filter.doFilter(
                request,
                response,
                filterChain
        );

        assertEquals(
                403,
                response.getStatus()
        );

        verifyNoInteractions(filterChain);
    }

    @Test
    void admin_userList_shouldBeAllowed()
            throws Exception {

        MockHttpServletRequest request =
                authenticatedRequest(
                        "GET",
                        "/api/users",
                        "valid-token"
                );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        stubValidToken("ADMIN");

        filter.doFilter(
                request,
                response,
                filterChain
        );

        HttpServletRequest forwardedRequest =
                captureForwardedRequest(response);

        assertEquals(
                "ADMIN",
                forwardedRequest.getHeader(
                        "X-User-Role"
                )
        );
    }

    private MockHttpServletRequest authenticatedRequest(
            String method,
            String path,
            String token
    ) {

        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        method,
                        path
                );

        request.addHeader(
                HttpHeaders.AUTHORIZATION,
                "Bearer " + token
        );

        return request;
    }

    private void stubValidToken(String role) {

        when(jwtService.parseToken(
                "valid-token"
        )).thenReturn(claims);

        when(claims.get("userId"))
                .thenReturn(1L);

        when(claims.get(
                "role",
                String.class
        )).thenReturn(role);
    }

    private HttpServletRequest captureForwardedRequest(
            MockHttpServletResponse response
    ) throws Exception {

        ArgumentCaptor<ServletRequest> requestCaptor =
                ArgumentCaptor.forClass(
                        ServletRequest.class
                );

        verify(filterChain).doFilter(
                requestCaptor.capture(),
                same(response)
        );

        return (HttpServletRequest)
                requestCaptor.getValue();
    }
}