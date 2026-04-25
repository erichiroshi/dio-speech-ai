package com.example.diospeechai.notification.adapter.in.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.diospeechai.notification.domain.model.NotificationChannel;
import com.example.diospeechai.notification.domain.model.NotificationRequest;
import com.example.diospeechai.notification.domain.port.in.NotifyPort;
import com.example.diospeechai.transcription.adapter.out.messaging.event.TranscriptionCompletedEvent;
import com.example.diospeechai.transcription.infrastructure.messaging.rabbit.RabbitMQConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Adapter de entrada — consome eventos de transcrição concluída e
 * dispara notificações pelos canais configurados.
 *
 * <p>Assina a queue {@code transcription.completed}, que está ligada à
 * exchange {@code transcription.events}. O serviço de transcrição publica
 * nessa exchange sem saber que este consumer existe.
 *
 * <p>O destinatário e o canal são lidos de propriedades de configuração
 * ({@code notification.default-recipient} e {@code notification.channel}).
 * Em uma evolução futura, o evento poderia carregar o destinatário e o canal
 * preferido do usuário.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TranscriptionCompletedConsumer {

    private final NotifyPort notifyPort;

    @Value("${notification.default-recipient:noreply@example.com}")
    private String defaultRecipient;

    @Value("${notification.channel:EMAIL}")
    private String defaultChannel;

    @RabbitListener(queues = RabbitMQConfig.COMPLETED_QUEUE, id = "transcription-completed-consumer")
    public void onTranscriptionCompleted(TranscriptionCompletedEvent event) {
        log.info("Evento recebido | transcriptionId={} | canal={}",
                event.transcriptionId(), defaultChannel);

        try {
            NotificationChannel channel = NotificationChannel.valueOf(defaultChannel.toUpperCase());

            NotificationRequest request = new NotificationRequest(
                    event.transcriptionId(),
                    defaultRecipient,
                    channel,
                    event.text()
            );

            notifyPort.notify(request);

        } catch (IllegalArgumentException ex) {
            log.error("Canal inválido na configuração | channel={} | error={}",
                    defaultChannel, ex.getMessage());
            // Lança para que a mensagem vá para DLQ após as tentativas
            throw ex;
        }
    }
}