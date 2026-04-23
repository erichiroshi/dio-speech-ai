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
import com.example.diospeechai.transcription.metrics.TranscriptionMetrics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Caso de uso: transcrever um arquivo de áudio.
 *
 * <p>Orquestra os ports sem conhecer nenhuma tecnologia concreta:
 * <ol>
 *   <li>Calcula SHA-256 dos bytes do áudio</li>
 *   <li>{@link TranscriptionCachePort#get} — HIT: retorna com {@code cached=true}</li>
 *   <li>MISS: {@link SpeechToTextPort#transcribe} — chama o serviço de IA</li>
 *   <li>{@link TranscriptionCachePort#put} — armazena para próximas requisições</li>
 *   <li>{@link TranscriptionEventPort#publish} — notifica outros serviços</li>
 *   <li>Métricas e logs</li>
 * </ol>
 *
 * <p>Esta classe não sabe que existe WebClient, Redis, RabbitMQ ou Spring Security.
 * Toda tecnologia fica nos adapters que implementam os ports.
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

		String audioHash = sha256(command.audioBytes());

		MDC.put("fileName", command.filename());
		MDC.put("fileSizeBytes", String.valueOf(command.fileSizeBytes()));

		log.info("Iniciando transcrição | size={}bytes", command.fileSizeBytes());
		metrics.recordFileSize(command.fileSizeBytes());

		try {
			// ── Cache check ───────────────────────────────────────────────────
			Optional<TranscriptionResult> cached = cachePort.get(audioHash);
			if (cached.isPresent()) {
				metrics.recordCacheHit();
				log.info("Cache HIT | hash={}", audioHash);
				return cached.get().asCached();
			}

			metrics.recordCacheMiss();

			// ── Transcrição ───────────────────────────────────────────────────
			String text = metrics.recordWhisperCall(() -> speechPort.transcribe(command.audioBytes()));

			TranscriptionResult result = new TranscriptionResult(text, command.fileSizeBytes());

			// ── Store cache ───────────────────────────────────────────────────
			cachePort.put(audioHash, result);

			// ── Evento ────────────────────────────────────────────────────────
			eventPort.publish(Transcription.of(audioHash, text, command.fileSizeBytes()));

			metrics.recordSuccess();
			log.info("Transcrição concluída | chars={} | size={}bytes", text.length(), command.fileSizeBytes());

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

	// ── Helper ────────────────────────────────────────────────────────────────

	private String sha256(byte[] bytes) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 não disponível", ex);
		}
	}
}