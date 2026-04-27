package com.example.diospeechai.notification.domain.model;

/**
 * Canais de notificação suportados.
 *
 * <p>Cada canal tem um adapter de infraestrutura correspondente
 * que implementa {@link com.example.diospeechai.notification.domain.port.out.NotificationPort}.
 */
public enum NotificationChannel {
    EMAIL,
    SMS,
    WHATSAPP,
    NO_OP
}