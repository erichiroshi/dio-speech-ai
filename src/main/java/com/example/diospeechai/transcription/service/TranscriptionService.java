package com.example.diospeechai.transcription.service;

import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.diospeechai.transcription.dto.TranscriptionResponse;
import com.example.diospeechai.transcription.dto.WhisperResponse;
import com.example.diospeechai.transcription.exception.TranscriptionException;
import com.example.diospeechai.transcription.metrics.TranscriptionMetrics;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class TranscriptionService {

    private final SpeechToTextClient client;
    private final TranscriptionMetrics metrics;
    
    private static final Set<String> ALLOWED_TYPES = Set.of(
    	    "audio/wav",
    	    "audio/mpeg",
    	    "audio/wave",
            "audio/x-wav"
    	);

    public TranscriptionResponse transcribe(MultipartFile file) {

        validate(file);
        
        // Registra tamanho do arquivo antes de processar
        metrics.recordFileSize(file.getSize());
    	
		try {
			// Timer envolve apenas a chamada ao Whisper, não a validação
			WhisperResponse whisper = metrics.recordWhisperCall(() -> client.transcribe(file));

			metrics.recordSuccess();

			return new TranscriptionResponse(whisper.text(), file.getSize());

		} catch (RuntimeException ex) {
			metrics.recordError();
			throw ex;
		}
	}

	   private void validate(MultipartFile file) {
		   
        if (file == null || file.isEmpty()) {
            throw new TranscriptionException("Arquivo vazio ou ausente");
        }		   

    	String contentType = file.getContentType();
    	
		if (contentType == null) {
            throw new TranscriptionException("Content-Type do arquivo não informado");
		}
    	
        if (file.isEmpty()) {
            throw new TranscriptionException("Arquivo vazio");
        }
        
		if (!ALLOWED_TYPES.contains(contentType.toLowerCase())) {
			throw new TranscriptionException(
					"Tipo de arquivo inválido: '%s'. Tipos aceitos: %s"
						.formatted(contentType, ALLOWED_TYPES)
			);
		}
	}
}