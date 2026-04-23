package com.example.diospeechai.transcription.infrastructure.messaging;

import org.springframework.stereotype.Component;

import com.example.diospeechai.transcription.domain.model.Transcription;
import com.example.diospeechai.transcription.domain.port.out.TranscriptionEventPort;

import lombok.extern.slf4j.Slf4j;

/**
 * Implementação no-op do {@link TranscriptionEventPort}.
 *
 * <p>Placeholder até a Fase 8 (RabbitMQ). Apenas loga o evento sem
 * publicar em nenhuma fila. Permite que o {@code TranscribeAudioUseCase}
 * chame {@code eventPort.publish()} desde já, sem depender do RabbitMQ.
 *
 * <p>Na Fase 8, este bean é substituído por {@code RabbitTranscriptionEventAdapter}
 * via {@code @Primary} ou remoção deste arquivo.
 */
@Slf4j
@Component
public class NoOpTranscriptionEventAdapter implements TranscriptionEventPort {

    @Override
    public void publish(Transcription transcription) {
        log.debug("Evento de transcrição (no-op) | id={} | hash={} | texto='{}' | createdAt={}",
                transcription.id(), transcription.audioHash(), transcription.text(), transcription.createdAt());
        // Fase 8: substituir por publicação real no RabbitMQ
    }
}