package com.example.diospeechai.transcription.exception;

/**
 * Lançada quando o CircuitBreaker do Whisper está OPEN.
 * Mapeada para HTTP 503 pelo {@link GlobalExceptionHandler}.
 */
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message) {
        super(message);
    }

    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}