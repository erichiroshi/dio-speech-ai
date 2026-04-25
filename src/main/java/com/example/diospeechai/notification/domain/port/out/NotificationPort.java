package com.example.diospeechai.notification.domain.port.out;

import com.example.diospeechai.notification.domain.model.NotificationChannel;
import com.example.diospeechai.notification.domain.model.NotificationRequest;

/**
 * PORT — contrato de envio de notificação.
 *
 * <p>Cada canal tem um adapter que implementa esta interface:
 * <ul>
 *   <li>{@code EmailNotificationAdapter} — v9.2.0</li>
 *   <li>{@code SmsNotificationAdapter} — v9.3.0</li>
 *   <li>{@code WhatsAppNotificationAdapter} — v9.3.0</li>
 * </ul>
 *
 * <p>O {@link com.example.diospeechai.notification.application.NotifyUseCase}
 * recebe uma lista de todos os adapters e delega para o correto com base
 * no {@link NotificationChannel} do request.
 */
public interface NotificationPort {

    /**
     * Envia a notificação para o destinatário.
     *
     * @param request dados da notificação
     */
    void send(NotificationRequest request);

    /**
     * Canal que este adapter suporta.
     * Usado pelo caso de uso para selecionar o adapter correto.
     */
    NotificationChannel channel();
}