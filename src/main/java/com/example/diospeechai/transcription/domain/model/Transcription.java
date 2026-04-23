package com.example.diospeechai.transcription.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidade central do domínio de transcrição.
 *
 * <p>Representa uma transcrição realizada: o áudio que entrou,
 * o hash que identifica seu conteúdo e o texto que saiu.
 *
 * <p><strong>Regra de design:</strong> zero import de framework aqui.
 * Esta classe não conhece Spring, Redis, Whisper nem RabbitMQ.
 * É POJO puro — testável sem contexto, sem container, sem infraestrutura.
 */
public record Transcription(

        /** Identificador único desta transcrição. */
        UUID id,

        /**
         * SHA-256 (hex) do conteúdo binário do arquivo de áudio.
         * Dois arquivos com conteúdo idêntico produzem o mesmo hash —
         * base da estratégia de cache por conteúdo.
         */
        String audioHash,

        /** Texto transcrito pelo Whisper. */
        String text,

        /** Tamanho do arquivo original em bytes. */
        long fileSizeBytes,

        /** Instante em que a transcrição foi concluída. */
        Instant createdAt

) {
    /**
     * Factory method — cria uma transcrição com id e timestamp gerados automaticamente.
     *
     * @param audioHash  SHA-256 hex do conteúdo do áudio
     * @param text       texto transcrito
     * @param sizeBytes  tamanho do arquivo em bytes
     */
    public static Transcription of(String audioHash, String text, long sizeBytes) {
		return new Transcription(UUID.randomUUID(), audioHash, text, sizeBytes, Instant.now());
	}
}