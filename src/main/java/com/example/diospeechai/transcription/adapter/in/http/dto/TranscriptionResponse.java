package com.example.diospeechai.transcription.adapter.in.http.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de resposta HTTP da API de transcrição.
 *
 * <p>v7.5.0: movido de {@code transcription/dto/} para {@code adapter/in/http/dto/}.
 * É uma representação HTTP do resultado — não faz parte do domínio.
 *
 * <p>{@code @JsonInclude(NON_NULL)} evita que {@code "cached": null} apareça
 * nas respostas de cache miss — o campo só aparece quando {@code true}.
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

        @Schema(description = "true quando servido do cache Redis. Ausente em cache miss.",
                example = "true", nullable = true)
        Boolean cached

) {
    /** Construtor de conveniência para cache miss. */
    public TranscriptionResponse(String text, Long fileSizeBytes) {
        this(text, fileSizeBytes, null);
    }
}