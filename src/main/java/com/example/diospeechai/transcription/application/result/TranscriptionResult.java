package com.example.diospeechai.transcription.application.result;

/**
 * Objeto de saída do caso de uso {@code TranscribeAudioUseCase}.
 *
 * <p>Carrega o resultado sem acoplamento com {@code ResponseEntity}
 * (HTTP) nem com eventos RabbitMQ. O adapter converte este resultado
 * para sua representação específica de saída.
 *
 * <p>{@code cached} indica se o resultado veio do Redis (true) ou
 * foi processado agora pelo Whisper (false).
 *
 * @param text          texto transcrito
 * @param fileSizeBytes tamanho do arquivo original em bytes
 * @param cached        true se servido do cache, false se processado agora
 */
public record TranscriptionResult(
        String text,
        long fileSizeBytes,
        boolean cached
) {
    /** Construtor de conveniência para resultado de cache miss (processado pelo Whisper). */
    public TranscriptionResult(String text, long fileSizeBytes) {
        this(text, fileSizeBytes, false);
    }

    /** Retorna uma cópia marcada como vinda do cache. */
    public TranscriptionResult asCached() {
        return new TranscriptionResult(text, fileSizeBytes, true);
    }
}