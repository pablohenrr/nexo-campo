package com.nexocampo.modules.identity;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            autenticarPorToken(token, request);
        }
        filterChain.doFilter(request, response);
    }

    private void autenticarPorToken(String token, HttpServletRequest request) {
        if (!jwtService.isValid(token)) {
            return;
        }
        userRepository.findById(jwtService.extractUserId(token)).ifPresent(user -> {
            var authentication = new UsernamePasswordAuthenticationToken(
                    user,
                    null,
                    java.util.List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        });
    }
}
