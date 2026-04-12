package com.example.diospeechai.transcription.service;

import java.util.Set;

import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.diospeechai.transcription.cache.CacheService;
import com.example.diospeechai.transcription.dto.TranscriptionResponse;
import com.example.diospeechai.transcription.dto.WhisperResponse;
import com.example.diospeechai.transcription.exception.TranscriptionException;
import com.example.diospeechai.transcription.metrics.TranscriptionMetrics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Orquestra o fluxo de transcrição com cache:
 * validação → MDC → check cache → (miss) Whisper → store cache → resposta.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TranscriptionService {
	
    private final SpeechToTextClient client;
    private final TranscriptionMetrics metrics;
    private final CacheService cacheService;
    
    private static final Set<String> ALLOWED_TYPES = Set.of(
    	    "audio/wav",
    	    "audio/mpeg",
    	    "audio/wave",
            "audio/x-wav"
    	);

    public TranscriptionResponse transcribe(MultipartFile file) {

        validate(file);
        
        // ── Enriquecer MDC com campos de negócio ──────────────────────────────
        // Esses campos ficam disponíveis para tod-o log emitido daqui para baixo
        // na mesma thread, incluindo logs do SpeechToTextClient
        MDC.put("fileName",      file.getOriginalFilename());
        MDC.put("fileSizeBytes", String.valueOf(file.getSize()));
 
        log.info("Iniciando transcrição | size={}bytes | type={}",
                file.getSize(), file.getContentType());        
        
        // Registra tamanho do arquivo antes de processar
        metrics.recordFileSize(file.getSize());
    	
		try {
            byte[] fileBytes = file.getBytes();
			
            // ── Cache check ───────────────────────────────────────────────────
            TranscriptionResponse cached = cacheService.get(fileBytes);
            
            if (cached != null) {
                metrics.recordCacheHit();
                log.info("Retornando do cache | size={}bytes", file.getSize());
                return cached.asCached();
            }
            
            metrics.recordCacheMiss();
            
			// Timer envolve apenas a chamada ao Whisper, não a validação
			WhisperResponse whisper = metrics.recordWhisperCall(() -> client.transcribe(file));

            TranscriptionResponse response = new TranscriptionResponse(whisper.text(), file.getSize());
			
            // ── Store em cache ────────────────────────────────────────────────
            cacheService.put(fileBytes, response);
            
			metrics.recordSuccess();
			
            log.info("Transcrição concluída | chars={} | size={}bytes",
                    whisper.text().length(), file.getSize());			

			return response;

		} catch (RuntimeException ex) {
			metrics.recordError();
			log.error("Falha na transcrição | error={}", ex.getMessage());
			throw ex;
		} catch (Exception ex) {
			metrics.recordError();
			log.error("Falha ao ler bytes do arquivo | error={}", ex.getMessage());
			throw new TranscriptionException("Falha ao processar arquivo", ex);
		} finally {
			// Remove os campos de negócio — o requestId permanece (limpo pelo MdcLoggingFilter)
			MDC.remove("fileName");
			MDC.remove("fileSizeBytes");
			MDC.remove("whisperModel");
		}
	}

	// ── Validação ─────────────────────────────────────────────────────────────

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
						.formatted(contentType, ALLOWED_TYPES));
		}
	}
}