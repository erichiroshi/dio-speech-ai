package com.example.diospeechai.analysis.application.result;

/**
 * Output do caso de uso de sumarização.
 *
 * <p>Sem acoplamento com {@code ResponseEntity} ou qualquer objeto HTTP.
 * O adapter converte para o DTO de resposta na fronteira HTTP.
 *
 * @param audioHash SHA-256 do áudio original
 * @param summary   resumo gerado pelo LLM
 * @param model     modelo usado (ex: llama3.2:1b)
 * @param cached    true se veio do cache Redis, false se gerado agora
 */
public record SummaryResult(
        String audioHash,
        String summary,
        String model,
        boolean cached
) {
    public SummaryResult(String audioHash, String summary, String model) {
        this(audioHash, summary, model, false);
    }

    public SummaryResult asCached() {
        return new SummaryResult(audioHash, summary, model, true);
    }
}