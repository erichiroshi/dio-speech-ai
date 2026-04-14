package com.example.diospeechai.transcription.service;

import java.time.Duration;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

import com.example.diospeechai.transcription.dto.WhisperResponse;
import com.example.diospeechai.transcription.exception.ServiceUnavailableException;
import com.example.diospeechai.transcription.exception.TranscriptionException;

import lombok.extern.slf4j.Slf4j;

/**
 * Cliente HTTP para o Speaches protegido por Retry + CircuitBreaker.
 *
 * <p>v3.2.0: {@code @Retry} é adicionado acima de {@code @CircuitBreaker}.
 * A ordem das anotações define a cadeia de decoração (de dentro para fora):
 * <pre>
 *   Retry ( CircuitBreaker ( SpeechToTextClient.transcribe() ) )
 * </pre>
 *
 * <p>Fluxo de falha:
 * <ol>
 *   <li>Chamada falha com {@link TranscriptionException} (erro de I/O)</li>
 *   <li>Retry tenta novamente com backoff exponencial (500ms → 1s → 2s)</li>
 *   <li>Se ainda falhar após 3 tentativas, o CircuitBreaker registra a falha</li>
 *   <li>Quando o CB abre, o fallback lança {@link ServiceUnavailableException} → 503</li>
 * </ol>
 *
 * <p>Retry NÃO ocorre para {@link ServiceUnavailableException} (CB aberto)
 * nem para {@link IllegalArgumentException} (erros de validação).
 */
@Component
@Slf4j
public class SpeechToTextClient {

    private final WebClient webClient;
    
    @Value("${whisper.model}")
    private String model;
    
	public SpeechToTextClient(WebClient webClient, 
							  @Value("${whisper.base-url}") String baseUrl) {

        this.webClient = webClient.mutate()
				.baseUrl(baseUrl)
				.build();
	}
	
    @Retry(name = "whisper", fallbackMethod = "fallback")
    @CircuitBreaker(name = "whisper", fallbackMethod = "fallback")
    public WhisperResponse transcribe(MultipartFile file) {
    	
        // Registra o modelo no MDC — visível nos logs do TranscriptionService também
        MDC.put("whisperModel", model);
 
        log.debug("Chamando Speaches | model={} | file={}", model, file.getOriginalFilename());
        
        MultipartBodyBuilder body = new MultipartBodyBuilder();
        body.part("file", file.getResource());
        body.part("model", model);
        
        try {
            WhisperResponse response = webClient.post()
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

            log.debug("Resposta do Speaches recebida | chars={}", response.text().length());
            
			return response;

		} catch (TranscriptionException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new TranscriptionException("Falha na comunicação com Whisper", ex);
		}
	}
    
    /**
     * Fallback invocado quando o CircuitBreaker está OPEN ou quando
     * todas as tentativas falharam.
     *
     * <p>A assinatura deve ter os mesmos parâmetros do método principal
     * mais um parâmetro {@code Exception} no final.
     * @throws ServiceUnavailableException 
     */
    WhisperResponse fallback(MultipartFile file, Exception ex) {
        log.warn("Fallback ativado | cause={}", ex.getMessage(), file);
        throw new ServiceUnavailableException(
                "Serviço de transcrição temporariamente indisponível. Tente novamente em instantes.");
    }
}