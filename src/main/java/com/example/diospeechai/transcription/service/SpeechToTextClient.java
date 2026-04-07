package com.example.diospeechai.transcription.service;

import java.time.Duration;

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

@Component
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
    	
        MultipartBodyBuilder body = new MultipartBodyBuilder();
        body.part("file", file.getResource());
        body.part("model", model);
        
        try {
            return webClient.post()
                .uri("/v1/audio/transcriptions")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(body.build()))
                .retrieve()
                .onStatus(
                    HttpStatusCode::isError,
                    response -> response.bodyToMono(String.class)
                        .map(msg -> new TranscriptionException("Erro Whisper: " + msg))
                )
                .bodyToMono(WhisperResponse.class)
                .block(Duration.ofSeconds(30));

        } catch (Exception ex) {
            throw new TranscriptionException("Falha na comunicação com Whisper", ex);
        }
    }
}