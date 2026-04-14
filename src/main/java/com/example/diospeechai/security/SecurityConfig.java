package com.example.diospeechai.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;

/**
 * Configuração de segurança stateless com JWT.
 *
 * <p>Política de acesso:
 * <ul>
 *   <li>{@code GET  /actuator/health}     — público (healthcheck do Docker)</li>
 *   <li>{@code GET  /actuator/prometheus} — público (scraping do Prometheus)</li>
 *   <li>{@code POST /auth/token}          — público (obter token)</li>
 *   <li>{@code POST /api/transcriptions}  — requer {@code ROLE_USER}</li>
 *   <li>Qualquer outra rota              — requer autenticação</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @SuppressWarnings("unused")
	@Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) {
        return http
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .csrf(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth
                        // Endpoints públicos
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/prometheus").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/token").permitAll()
                        // API de transcrição — requer ROLE_USER
                        .requestMatchers(HttpMethod.POST, "/api/transcriptions").hasRole("USER")
                        // Tod-o o restante requer autenticação
                        .anyRequest().authenticated()
                )

                // Filtro JWT antes do filtro padrão de username/password
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

                // Resposta 401 sem redirecionamento (API REST, não web app)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/problem+json");
							response.getWriter().write("""
										{"type":"https://api.diospeechai/errors/unauthorized",
										"title":"Unauthorized",
										"status":401,
										"detail":"Token JWT ausente ou inválido"}
										"""
									);
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType("application/problem+json");
                            response.getWriter().write("""
                                    {"type":"https://api.diospeechai/errors/forbidden",
                                     "title":"Forbidden",
                                     "status":403,
                                     "detail":"Acesso negado — permissão insuficiente"}
                                    """);
                        })
                )

                .build();
    }
}