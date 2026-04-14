package com.example.diospeechai.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades JWT lidas de {@code application.yml} (prefixo {@code jwt}).
 *
 * <p>
 * Uso em produção:
 * 
 * <pre>
 *   JWT_SECRET=<openssl rand -hex 32>
 *   JWT_EXPIRATION_HOURS=8
 * </pre>
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
		String secret, 
		long expirationHours
) {}