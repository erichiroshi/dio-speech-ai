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

import com.example.diospeechai.transcription.dto.WhisperResponse;
import com.example.diospeechai.transcription.exception.TranscriptionException;

import lombok.extern.slf4j.Slf4j;

/**
 * Cliente HTTP para o serviço Speaches (Whisper).
 *
 * <p>Responsabilidade única: fazer a chamada HTTP multipart para
 * {@code POST /v1/audio/transcriptions} e mapear erros para
 * {@link TranscriptionException}.
 *
 * <p>Popula o campo MDC {@code whisperModel} para que o modelo utilizado
 * apareça nos logs estruturados de cada transcrição.
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
}