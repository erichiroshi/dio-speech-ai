package com.example.diospeechai.transcription.adapter.out.messaging.event;

import java.time.Instant;
import java.util.UUID;

import com.example.diospeechai.transcription.domain.model.Transcription;

/**
 * DTO do evento publicado no RabbitMQ após cada transcrição bem-sucedida.
 *
 * <p>Qualquer serviço que assine a exchange {@code transcription.events}
 * receberá este objeto serializado como JSON.
 *
 * <p>Consumidores possíveis: SMS, e-mail, WhatsApp, analytics, auditoria.
 * O serviço de transcrição não conhece nem se importa com quem vai consumir.
 *
 * <p>Localizado em {@code adapter/out/} porque é um objeto de saída do sistema —
 * a representação do evento para o mundo externo (fila), não para o domínio.
 *
 * @param transcriptionId  UUID da transcrição
 * @param audioHash        SHA-256 hex do áudio (permite deduplicação no consumer)
 * @param text             texto transcrito
 * @param fileSizeBytes    tamanho do arquivo em bytes
 * @param completedAt      instante de conclusão
 */
public record TranscriptionCompletedEvent(
        UUID    transcriptionId,
        String  audioHash,
        String  text,
        long    fileSizeBytes,
        Instant completedAt
) {
    /**
     * Factory method — converte a entidade de domínio {@link Transcription}
     * para o DTO de evento.
     */
    public static TranscriptionCompletedEvent from(Transcription transcription) {
        return new TranscriptionCompletedEvent(
                transcription.id(),
                transcription.audioHash(),
                transcription.text(),
                transcription.fileSizeBytes(),
                transcription.createdAt()
        );
    }
}