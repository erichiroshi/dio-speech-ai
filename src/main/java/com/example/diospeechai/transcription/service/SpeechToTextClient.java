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

import com.example.diospeechai.transcription.dto.WhisperResponse;
import com.example.diospeechai.transcription.exception.ServiceUnavailableException;
import com.example.diospeechai.transcription.exception.TranscriptionException;

import lombok.extern.slf4j.Slf4j;

/**
 * Cliente HTTP para o Speaches (Whisper) protegido por CircuitBreaker.
 *
 * <p>v3.1.0: {@code @CircuitBreaker(name = "whisper")} envolve a chamada HTTP.
 * Quando o circuito está OPEN, o fallback lança {@link ServiceUnavailableException}
 * que é mapeada para HTTP 503 pelo {@code GlobalExceptionHandler}.
 *
 * <p>O CircuitBreaker abre quando ≥ 50% das últimas 10 chamadas falham
 * (configurável em {@code application.yml → resilience4j.circuitbreaker.instances.whisper}).
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
    public WhisperResponse fallback(MultipartFile file, Exception ex) {
		log.warn("CircuitBreaker OPEN — Whisper indisponível | cause={}", file, ex.getMessage(), file);
        throw new ServiceUnavailableException(
                "Serviço de transcrição temporariamente indisponível. Tente novamente em instantes.");
    }
}