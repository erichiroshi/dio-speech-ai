package com.example.diospeechai.transcription.infrastructure.speechtotext.whisper;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades do serviço Whisper via Speaches.
 *
 * <p>Lidas de {@code application.yml} (prefixo {@code whisper}).
 *
 * <p>Variáveis de ambiente:
 * <pre>
 *   WHISPER_BASE_URL=http://speaches:8000
 *   WHISPER_MODEL=Systran/faster-whisper-small
 * </pre>
 */
@ConfigurationProperties(prefix = "whisper")
public record WhisperProperties(
        String baseUrl,
        String model
) {}