package com.example.diospeechai.transcription.infrastructure.speechtotext.whisper;

import java.time.Duration;

import org.slf4j.MDC;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

import com.example.diospeechai.transcription.domain.port.out.SpeechToTextPort;
import com.example.diospeechai.transcription.exception.ServiceUnavailableException;
import com.example.diospeechai.transcription.exception.TranscriptionException;

import lombok.extern.slf4j.Slf4j;

/**
 * Adapter de saída — implementa {@link SpeechToTextPort} via Speaches (Whisper).
 *
 * <p>Protegido por Retry + CircuitBreaker (Resilience4j).
 * Cadeia de decoração (de dentro para fora):
 * <pre>
 *   Retry ( CircuitBreaker ( WhisperAdapter.transcribe() ) )
 * </pre>
 *
 * <p>Fluxo de falha:
 * <ol>
 *   <li>Chamada falha → {@link TranscriptionException}</li>
 *   <li>Retry: 3 tentativas com backoff exponencial (500ms → 1s → 2s)</li>
 *   <li>CircuitBreaker abre → fallback lança {@link ServiceUnavailableException} → 503</li>
 * </ol>
 *
 * <p>Para trocar Whisper por outro provedor: criar novo adapter que implemente
 * {@link SpeechToTextPort} e anotar com {@code @Primary} ou
 * {@code @ConditionalOnProperty}. O domínio não muda.
 */
@Slf4j
@Component
@EnableConfigurationProperties(WhisperProperties.class)
public class WhisperAdapter implements SpeechToTextPort {

	private final WebClient webClient;
	private final WhisperProperties properties;

    public WhisperAdapter(WebClient webClient, WhisperProperties properties) {
        this.properties = properties;
        this.webClient  = webClient.mutate()
                .baseUrl(properties.baseUrl())
                .build();
    }

    @Override
    @Retry(name = "whisper", fallbackMethod = "fallback")
    @CircuitBreaker(name = "whisper", fallbackMethod = "fallback")
    public String transcribe(byte[] audioBytes) {

        MDC.put("whisperModel", properties.model());
        log.debug("Chamando Speaches | model={} | bytes={}", properties.model(), audioBytes.length);

        MultipartBodyBuilder body = new MultipartBodyBuilder();
        body.part("file",  audioBytes);
        body.part("model", properties.model());

        try {
            var response = webClient.post()
                    .uri("/v1/audio/transcriptions")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(body.build()))
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            resp -> resp.bodyToMono(String.class)
                                    .map(msg -> new TranscriptionException("Erro Whisper: " + msg))
                    )
                    .bodyToMono(WhisperResponse.class)
                    .block(Duration.ofSeconds(30));

            log.debug("Resposta Speaches | chars={}", response.text().length());
            return response.text();

        } catch (TranscriptionException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new TranscriptionException("Falha na comunicação com Whisper", ex);
        }
    }

    /**
     * Fallback — CircuitBreaker OPEN ou todas as tentativas de Retry esgotadas.
     * Assinatura exige os mesmos parâmetros do método principal + {@code Exception}.
     */
    String fallback(byte[] audioBytes, Exception ex) {
        log.warn("Fallback Whisper ativado | cause={}", ex.getMessage());
        throw new ServiceUnavailableException(
                "Serviço de transcrição temporariamente indisponível. Tente novamente em instantes.");
    }

    /** DTO interno para deserializar a resposta do Speaches. */
    private record WhisperResponse(String text) {}
}