package com.example.diospeechai.transcription.adapter.in.http.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de resposta HTTP da API de transcrição.
 *
 * <p>v10.2.0: campo {@code audioHash} adicionado para permitir que o cliente
 * use o hash diretamente em {@code POST /api/transcriptions/{audioHash}/analysis}
 * sem precisar calculá-lo independentemente.
 */
@Schema(description = "Resultado da transcrição de áudio")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TranscriptionResponse(

        @Schema(description = "Texto transcrito pelo Whisper",
                example = "testando o áudio para a gravação e teste da API")
        String text,

        @Schema(description = "Tamanho do arquivo de áudio enviado, em bytes",
                example = "461842")
        Long fileSizeBytes,

        @Schema(description = "SHA-256 do conteúdo do áudio. Use para chamar /analysis.",
                example = "a1b2c3d4e5f6...")
        String audioHash,

        @Schema(description = "true quando servido do cache Redis. Ausente em cache miss.",
                nullable = true)
        Boolean cached

) {
    /** Cache miss sem audioHash (compatibilidade retroativa). */
    public TranscriptionResponse(String text, Long fileSizeBytes) {
        this(text, fileSizeBytes, null, null);
    }

    /** Cache miss com audioHash. */
    public TranscriptionResponse(String text, Long fileSizeBytes, String audioHash) {
        this(text, fileSizeBytes, audioHash, null);
    }
}