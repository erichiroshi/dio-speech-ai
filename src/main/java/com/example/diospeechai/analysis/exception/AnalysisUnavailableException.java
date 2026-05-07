package com.example.diospeechai.analysis.exception;

/**
 * Lançada quando o serviço de LLM (Ollama) está indisponível.
 * Mapeada para HTTP 503 pelo {@code GlobalExceptionHandler}.
 */
public class AnalysisUnavailableException extends RuntimeException {

    public AnalysisUnavailableException(String message) {
        super(message);
    }

    public AnalysisUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}