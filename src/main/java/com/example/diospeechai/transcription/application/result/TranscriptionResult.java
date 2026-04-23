package com.example.diospeechai.transcription.application.result;

import io.swagger.v3.oas.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Resposta da API de transcrição.
 *
 * <p>v2.4.0: campo {@code cached} adicionado. Quando {@code true}, indica que a
 * transcrição foi retornada do cache Redis (nenhuma chamada ao Whisper foi feita).
 * Quando ausente na resposta JSON, a transcrição foi processada agora.
 *
 * <p>{@code @JsonInclude(NON_NULL)} evita que {@code "cached": null} apareça nas
 * respostas de cache miss — o campo só aparece quando for {@code true}.
 */
@Schema(description = "Resultado da transcrição de áudio")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TranscriptionResult(

        @Schema(
            description = "Texto transcrito pelo Whisper",
            example = "testando o áudio para a gravação e teste da API"
        )
        String text,

        @Schema(
            description = "Tamanho do arquivo de áudio enviado, em bytes",
            example = "461842"
        )
        Long fileSizeBytes,

        @Schema(
            description = "Presente e `true` quando a resposta foi servida do cache Redis. "
                        + "Ausente quando o Whisper processou o áudio nesta requisição.",
            example = "true",
            nullable = true
        )
        Boolean cached

) {
    /** Construtor de conveniência para respostas sem cache (miss ou primeira chamada). */
    public TranscriptionResult(String text, Long fileSizeBytes) {
        this(text, fileSizeBytes, null);
    }

    /** Retorna uma cópia desta resposta marcada como vinda do cache. */
    public TranscriptionResult asCached() {
        return new TranscriptionResult(text, fileSizeBytes, true);
    }
}