package com.example.diospeechai.notification.domain.model;

import java.util.UUID;

/**
 * Objeto de entrada do caso de uso de notificação.
 *
 * <p>Carrega todos os dados necessários para enviar uma notificação,
 * independente do canal. O adapter de cada canal extrai os campos
 * que precisa.
 *
 * @param transcriptionId  ID da transcrição que originou o evento
 * @param recipientContact destinatário: e-mail, número de telefone ou ID WhatsApp
 * @param channel          canal pelo qual a notificação deve ser enviada
 * @param transcribedText  texto transcrito — conteúdo principal da notificação
 */
public record NotificationRequest(
        UUID                transcriptionId,
        String              recipientContact,
        NotificationChannel channel,
        String              transcribedText
) {}