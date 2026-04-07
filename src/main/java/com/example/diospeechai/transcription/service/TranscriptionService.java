package com.example.diospeechai.transcription.service;

import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.diospeechai.transcription.dto.TranscriptionResponse;
import com.example.diospeechai.transcription.dto.WhisperResponse;

@Service
public class TranscriptionService {

    private final SpeechToTextClient client;
    
    private static final Set<String> ALLOWED_TYPES = Set.of(
    	    "audio/wav",
    	    "audio/mpeg",
    	    "audio/wave"
    	);

    public TranscriptionService(SpeechToTextClient client) {
        this.client = client;
    }

    public TranscriptionResponse transcribe(MultipartFile file) {

        validate(file);
    	
        long start = System.currentTimeMillis();
        
        WhisperResponse whisper = client.transcribe(file);

        long end = System.currentTimeMillis();

        return new TranscriptionResponse(
                whisper.text(),
                (end - start),
                file.getSize()
        );
    }
    
    private void validate(MultipartFile file) {

    	String contentType = file.getContentType();
    	
		if (contentType == null) {
			throw new IllegalArgumentException("Arquivo não pode ser nulo");
		}
    	
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo vazio");
        }
        
		if (!ALLOWED_TYPES.contains(contentType.toLowerCase())) {
			throw new IllegalArgumentException("Tipo de arquivo inválido");
		}
	}
}