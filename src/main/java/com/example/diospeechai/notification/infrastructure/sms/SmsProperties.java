package com.example.diospeechai.notification.infrastructure.sms;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades do adapter de SMS.
 *
 * <p>Compatível com qualquer provedor REST (Twilio, Vonage, AWS SNS):
 * <pre>
 *   NOTIFICATION_SMS_BASE_URL=https://api.vonage.com
 *   NOTIFICATION_SMS_SEND_PATH=/v1/sms
 *   NOTIFICATION_SMS_API_KEY=...
 *   NOTIFICATION_SMS_SENDER_ID=DioSpeech
 * </pre>
 */
@ConfigurationProperties(prefix = "notification.sms")
public record SmsProperties(
        String baseUrl,
        String sendPath,
        String apiKey,
        String senderId
) {}