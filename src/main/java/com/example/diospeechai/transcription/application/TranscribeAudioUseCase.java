package com.example.diospeechai.transcription.application;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import com.example.diospeechai.transcription.application.command.TranscribeCommand;
import com.example.diospeechai.transcription.application.result.TranscriptionResult;
import com.example.diospeechai.transcription.domain.model.Transcription;
import com.example.diospeechai.transcription.domain.port.in.TranscribeAudioPort;
import com.example.diospeechai.transcription.domain.port.out.SpeechToTextPort;
import com.example.diospeechai.transcription.domain.port.out.TranscriptionCachePort;
import com.example.diospeechai.transcription.domain.port.out.TranscriptionEventPort;
import com.example.diospeechai.transcription.exception.TranscriptionException;
import com.example.diospeechai.transcription.metrics.TranscriptionMetrics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Caso de uso: transcrever arquivo de áudio.
 *
 * <p>
 * v10.2.0: {@code audioHash} incluído no {@link TranscriptionResult} para que o
 * controller passe o hash na resposta HTTP, permitindo ao cliente chamar
 * {@code POST /api/transcriptions/{hash}/analysis}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TranscribeAudioUseCase implements TranscribeAudioPort {

	private final SpeechToTextPort speechPort;
	private final TranscriptionCachePort cachePort;
	private final TranscriptionEventPort eventPort;
	private final TranscriptionMetrics metrics;

	@Override
	public TranscriptionResult transcribe(TranscribeCommand command) {
		validate(command);

		String audioHash = sha256(command.audioBytes());

		MDC.put("fileName", command.filename());
		MDC.put("fileSizeBytes", String.valueOf(command.fileSizeBytes()));

		log.info("Iniciando transcrição | size={}bytes", command.fileSizeBytes());
		metrics.recordFileSize(command.fileSizeBytes());

		try {
			// Cache check
			Optional<TranscriptionResult> cached = cachePort.get(audioHash);
			if (cached.isPresent()) {
				metrics.recordCacheHit();
				log.info("Cache HIT | hash={}", audioHash);
				return cached.get().asCached();
			}

			metrics.recordCacheMiss();

			// Transcrição
			String text = metrics.recordWhisperCall(() -> speechPort.transcribe(command.audioBytes()));

			// v10.2.0: passa audioHash no result
			TranscriptionResult result = new TranscriptionResult(text, command.fileSizeBytes(), audioHash);

			cachePort.put(audioHash, result);
			eventPort.publish(Transcription.of(audioHash, text, command.fileSizeBytes()));

			metrics.recordSuccess();
			log.info("Transcrição concluída | chars={} | hash={}", text.length(), audioHash);

			return result;

		} catch (RuntimeException ex) {
			metrics.recordError();
			log.error("Falha na transcrição | error={}", ex.getMessage());
			throw ex;
		} finally {
			MDC.remove("fileName");
			MDC.remove("fileSizeBytes");
			MDC.remove("whisperModel");
		}
	}

	private void validate(TranscribeCommand command) {
		if (command == null || command.audioBytes() == null || command.audioBytes().length == 0)
			throw new TranscriptionException("Arquivo vazio ou ausente");
		if (command.contentType() == null)
			throw new TranscriptionException("Content-Type do arquivo não informado");
		var allowed = java.util.Set.of("audio/wav", "audio/wave", "audio/x-wav", "audio/mpeg");
		if (!allowed.contains(command.contentType().toLowerCase()))
			throw new TranscriptionException(
					"Tipo de arquivo inválido: '%s'. Tipos aceitos: %s".formatted(command.contentType(), allowed));
	}

	private String sha256(byte[] bytes) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 não disponível", ex);
		}
	}
}