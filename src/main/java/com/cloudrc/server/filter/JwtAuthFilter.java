package com.cloudrc.server.filter;

import com.cloudrc.server.service.JwtService;
import io.jsonwebtoken.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Get Authorization header
        String authHeader = request.getHeader("Authorization");

        // 2. No token — skip filter, Security will handle 401
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            try {
                filterChain.doFilter(request, response);
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
            return;
        }

        // 3. Extract token — remove "Bearer " prefix
        String token = authHeader.substring(7);

        // 4. Extract email from token
        String email = jwtService.extractEmail(token);

        // 5. If email found and no auth set yet in context
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // 6. Load user from DB
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            // 7. Validate token
            if (jwtService.isTokenValid(token)) {

                // 8. Create auth token and set in SecurityContext
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 9. Continue chain
        try {
            filterChain.doFilter(request, response);
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }
}