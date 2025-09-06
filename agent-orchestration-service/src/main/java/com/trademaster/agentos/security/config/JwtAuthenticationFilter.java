package com.trademaster.agentos.security.config;

import com.trademaster.agentos.security.model.SecurityContext;
import com.trademaster.agentos.security.service.AuthenticationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JWT Authentication Filter - Processes JWT tokens for each request.
 */
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private AuthenticationService authenticationService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        try {
            String token = extractTokenFromRequest(request);
            
            if (StringUtils.hasText(token)) {
                // Build security context
                SecurityContext securityContext = SecurityContext.builder()
                    .correlationId(UUID.randomUUID().toString())
                    .token(token)
                    .ipAddress(request.getRemoteAddr())
                    .userAgent(request.getHeader("User-Agent"))
                    .build();
                
                // Validate token
                var result = authenticationService.validateToken(token);
                
                if (result.isSuccess()) {
                    SecurityContext validContext = result.orElse(null);
                    
                    if (validContext != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        // Create Spring Security authentication
                        var authorities = validContext.roles().stream()
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                            .collect(Collectors.toList());
                        
                        // Add permissions as authorities too
                        authorities.addAll(validContext.permissions().stream()
                            .map(SimpleGrantedAuthority::new)
                            .toList());
                        
                        UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(
                                validContext.userId(),
                                null,
                                authorities
                            );
                        
                        authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                        );
                        
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        
                        // Store correlation ID in request
                        request.setAttribute("correlationId", validContext.correlationId());
                        request.setAttribute("securityContext", validContext);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication", e);
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        // Check for token in query parameter (for WebSocket connections)
        String token = request.getParameter("token");
        if (StringUtils.hasText(token)) {
            return token;
        }
        
        return null;
    }
}