package com.example.diospeechai.notification.infrastructure.whatsapp;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades do adapter de WhatsApp.
 * <pre>
 *   NOTIFICATION_WHATSAPP_BASE_URL=https://graph.facebook.com
 *   NOTIFICATION_WHATSAPP_SEND_PATH=/v19.0/{phone-number-id}/messages
 *   NOTIFICATION_WHATSAPP_ACCESS_TOKEN=...
 * </pre>
 */
@ConfigurationProperties(prefix = "notification.whatsapp")
public record WhatsAppProperties(
        String baseUrl,
        String sendPath,
        String accessToken
) {}