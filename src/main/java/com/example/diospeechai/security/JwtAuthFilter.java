package com.example.diospeechai.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.nimbusds.jwt.JWTClaimsSet;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Filtro JWT stateless — extrai e valida o Bearer token a cada requisição.
 *
 * <p>Fluxo:
 * <ol>
 *   <li>Extrai o header {@code Authorization: Bearer <token>}</li>
 *   <li>Valida assinatura e expiração via {@link JwtService}</li>
 *   <li>Popula o {@code SecurityContext} com as roles do token</li>
 *   <li>Em caso de token inválido, limpa o contexto e deixa a requisição
 *       seguir — o Spring Security retornará 401 na autorização</li>
 * </ol>
 *
 * <p>Requisições sem header {@code Authorization} passam sem autenticação
 * (endpoints públicos são definidos no {@link SecurityConfig}).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            JWTClaimsSet claims = jwtService.validateToken(token);

            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) claims.getClaim("roles");

            List<SimpleGrantedAuthority> authorities = roles == null
                    ? List.of()
                    : roles.stream().map(SimpleGrantedAuthority::new).toList();

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);

            SecurityContextHolder.getContext().setAuthentication(auth);
            log.debug("JWT válido | sub={} | roles={}", claims.getSubject(), roles);

        } catch (JwtValidationException ex) {
            log.warn("JWT inválido | reason={}", ex.getMessage());
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(request, response);
    }
}