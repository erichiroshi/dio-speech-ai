package com.example.diospeechai.transcription.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Resposta da API de transcrição.
 *
 * <p>
 * v2.4.0: campo {@code cached} adicionado. Quando {@code true}, indica que a
 * transcrição foi retornada do cache Redis (nenhuma chamada ao Whisper foi
 * feita). Quando {@code false} ou ausente na resposta JSON, a transcrição foi
 * processada agora.
 *
 * <p>
 * {@code @JsonInclude(NON_NULL)} evita que {@code "cached": null} apareça nas
 * respostas de cache miss — o campo só aparece quando for {@code true}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TranscriptionResponse(
		String text, 
		Long fileSizeBytes, 
		Boolean cached
) {
	/**
	 * Construtor de conveniência para respostas sem cache (miss ou primeira
	 * chamada).
	 */
	public TranscriptionResponse(String text, Long fileSizeBytes) {
		this(text, fileSizeBytes, null);
	}

	/** Retorna uma cópia desta resposta marcada como vinda do cache. */
	public TranscriptionResponse asCached() {
		return new TranscriptionResponse(text, fileSizeBytes, true);
	}
}