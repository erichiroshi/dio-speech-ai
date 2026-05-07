package com.example.diospeechai.transcription.application.result;

/**
 * Output do caso de uso de transcrição.
 *
 * <p>v10.2.0: {@code audioHash} adicionado para que o controller possa
 * incluí-lo na resposta HTTP sem precisar recalcular o SHA-256.
 *
 * @param text          texto transcrito
 * @param fileSizeBytes tamanho do arquivo em bytes
 * @param audioHash     SHA-256 hex do conteúdo do áudio
 * @param cached        true se veio do cache, false se processado pelo Whisper
 */
public record TranscriptionResult(
        String  text,
        long    fileSizeBytes,
        String  audioHash,
        boolean cached
) {
    /** Cache miss — hash calculado pelo use case. */
    public TranscriptionResult(String text, long fileSizeBytes, String audioHash) {
        this(text, fileSizeBytes, audioHash, false);
    }

    /** Retorna cópia marcada como vinda do cache. */
    public TranscriptionResult asCached() {
        return new TranscriptionResult(text, fileSizeBytes, audioHash, true);
    }
}