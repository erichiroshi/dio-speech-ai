package com.example.diospeechai.notification.infrastructure.email;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades do adaptador de e-mail.
 *
 * <p>Lidas de {@code application.yml} (prefixo {@code notification.email}).
 *
 * <p>Variáveis de ambiente:
 * <pre>
 *   NOTIFICATION_EMAIL_FROM=noreply@meudominio.com
 * </pre>
 */
@ConfigurationProperties(prefix = "notification.email")
public record EmailProperties(
        String from
) {}