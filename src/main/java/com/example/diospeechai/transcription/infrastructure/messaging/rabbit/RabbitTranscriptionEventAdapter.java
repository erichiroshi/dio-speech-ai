package com.example.diospeechai.transcription.infrastructure.messaging.rabbit;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.example.diospeechai.transcription.adapter.out.messaging.event.TranscriptionCompletedEvent;
import com.example.diospeechai.transcription.domain.model.Transcription;
import com.example.diospeechai.transcription.domain.port.out.TranscriptionEventPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Adapter de saída — implementa {@link TranscriptionEventPort} via RabbitMQ.
 *
 * <p>Publica um {@link TranscriptionCompletedEvent} na exchange
 * {@code transcription.events} com routing key {@code transcription.completed}
 * após cada transcrição bem-sucedida.
 *
 * <p>{@code @Primary} garante que este bean tem precedência sobre o
 * {@code NoOpTranscriptionEventAdapter} (placeholder da Fase 7) sem necessidade
 * de remover o no-op — útil para manter testes que não sobem RabbitMQ.
 *
 * <p>Qualquer serviço que assine a exchange receberá o evento:
 * <ul>
 *   <li>Serviço de e-mail (Fase 9)</li>
 *   <li>Serviço de SMS (Fase 9)</li>
 *   <li>Serviço de WhatsApp (Fase 9)</li>
 *   <li>Qualquer consumer futuro</li>
 * </ul>
 */
@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class RabbitTranscriptionEventAdapter implements TranscriptionEventPort {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(Transcription transcription) {
        TranscriptionCompletedEvent event = TranscriptionCompletedEvent.from(transcription);

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EVENTS_EXCHANGE,
                    RabbitMQConfig.COMPLETED_ROUTING_KEY,
                    event
            );
            log.info("Evento publicado | exchange={} | id={} | hash={}",
                    RabbitMQConfig.EVENTS_EXCHANGE,
                    event.transcriptionId(),
                    event.audioHash());
        } catch (Exception ex) {
            // Falha de mensageria não deve derrubar a transcrição já concluída
            log.error("Falha ao publicar evento RabbitMQ | id={} | error={}",
                    event.transcriptionId(), ex.getMessage());
        }
    }
}