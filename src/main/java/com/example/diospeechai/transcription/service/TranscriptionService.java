package com.example.diospeechai.transcription.service;

import java.util.Set;

import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.diospeechai.transcription.dto.TranscriptionResponse;
import com.example.diospeechai.transcription.dto.WhisperResponse;
import com.example.diospeechai.transcription.exception.TranscriptionException;
import com.example.diospeechai.transcription.metrics.TranscriptionMetrics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Orquestra o fluxo de transcrição: validação → MDC → métricas → Whisper → resposta.
 *
 * <p>Campos MDC populados por este service (complementam os do MdcLoggingFilter):
 * <ul>
 *   <li>{@code fileName} — nome original do arquivo de áudio</li>
 *   <li>{@code fileSizeBytes} — tamanho em bytes</li>
 *   <li>{@code whisperModel} — modelo utilizado (do SpeechToTextClient)</li>
 * </ul>
 *
 * <p>Esses campos aparecem como campos de primeiro nível no JSON de log em produção,
 * permitindo queries como: {@code fileName="audio.wav" AND status="success"}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
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
			// Timer envolve apenas a chamada ao Whisper, não a validação
			WhisperResponse whisper = metrics.recordWhisperCall(() -> client.transcribe(file));

			metrics.recordSuccess();
			
            log.info("Transcrição concluída | chars={} | size={}bytes",
                    whisper.text().length(), file.getSize());			

			return new TranscriptionResponse(whisper.text(), file.getSize());

		} catch (RuntimeException ex) {
			
			metrics.recordError();
			
            log.error("Falha na transcrição | error={}", ex.getMessage());
            
			throw ex;
			
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