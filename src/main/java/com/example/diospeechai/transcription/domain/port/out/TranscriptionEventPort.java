package com.example.diospeechai.transcription.domain.port.out;

import com.example.diospeechai.transcription.domain.model.Transcription;

/**
 * PORT DE SAÍDA — contrato de publicação de eventos de transcrição.
 *
 * <p>Após cada transcrição bem-sucedida, o caso de uso publica um evento
 * por meio desta interface. Qualquer serviço interessado pode assinar
 * (SMS, e-mail, WhatsApp, analytics) — o domínio não sabe e não precisa saber.
 *
 * <p>Hoje: implementado por {@code NoOpTranscriptionEventAdapter} (placeholder)
 * enquanto o RabbitMQ não está configurado (Fase 8).
 * Na Fase 8: {@code RabbitTranscriptionEventAdapter} substitui o no-op.
 */
public interface TranscriptionEventPort {

    /**
     * Publica o evento de conclusão de transcrição.
     *
     * @param transcription a transcrição concluída
     */
    void publish(Transcription transcription);
}