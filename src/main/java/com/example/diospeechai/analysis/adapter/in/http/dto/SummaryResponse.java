package com.example.diospeechai.analysis.adapter.in.http.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de resposta HTTP da API de análise.
 *
 * <p>{@code cached=true} indica que o resumo veio do Redis e não foi
 * gerado novamente pelo LLM. Omitido quando {@code false}.
 */
@Schema(description = "Resumo gerado por LLM para a transcrição")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SummaryResponse(

        @Schema(description = "SHA-256 do áudio original", example = "a1b2c3...")
        String audioHash,

        @Schema(description = "Resumo gerado pelo LLM em 2 a 4 frases",
                example = "O áudio discute os resultados do projeto e as próximas etapas.")
        String summary,

        @Schema(description = "Modelo LLM usado", example = "llama3.2:1b")
        String model,

        @Schema(description = "true quando servido do cache. Ausente se gerado agora.",
                nullable = true)
        Boolean cached
) {
    public SummaryResponse(String audioHash, String summary, String model) {
        this(audioHash, summary, model, null);
    }
}