package com.example.diospeechai.analysis.domain.model;

import java.time.Instant;

/**
 * Entidade de domínio: resumo de uma transcrição gerado por LLM.
 *
 * <p>Zero import de framework — POJO puro.
 *
 * @param audioHash SHA-256 do áudio original (chave de correlação com a transcrição)
 * @param summary   texto do resumo gerado pelo LLM
 * @param model     modelo Ollama usado (ex: llama3.2:1b)
 * @param createdAt instante de geração
 */
public record TranscriptionSummary(
        String audioHash,
        String summary,
        String model,
        Instant createdAt
) {
    public static TranscriptionSummary of(String audioHash, String summary, String model) {
        return new TranscriptionSummary(audioHash, summary, model, Instant.now());
    }
}