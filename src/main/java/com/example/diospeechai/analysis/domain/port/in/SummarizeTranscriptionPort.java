package com.example.diospeechai.analysis.domain.port.in;

import com.example.diospeechai.analysis.application.command.SummarizeCommand;
import com.example.diospeechai.analysis.application.result.SummaryResult;

/**
 * PORT DE ENTRADA — contrato do caso de uso de sumarização.
 *
 * <p>Injetado pelo {@code TranscriptionAnalysisController}.
 * O controller não conhece Ollama, Redis nem Spring AI.
 */
public interface SummarizeTranscriptionPort {

    /**
     * Gera um resumo para o texto transcrito.
     *
     * @param command dados da transcrição a resumir
     * @return resumo gerado pelo LLM
     */
    SummaryResult summarize(SummarizeCommand command);
}