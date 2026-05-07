package com.example.diospeechai.transcription.adapter.in.http;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.diospeechai.transcription.adapter.in.http.documentation.TranscriptionControllerDocumentation;
import com.example.diospeechai.transcription.adapter.in.http.dto.TranscriptionResponse;
import com.example.diospeechai.transcription.application.command.TranscribeCommand;
import com.example.diospeechai.transcription.application.result.TranscriptionResult;
import com.example.diospeechai.transcription.domain.port.in.TranscribeAudioPort;
import com.example.diospeechai.transcription.exception.TranscriptionException;

import lombok.RequiredArgsConstructor;

/**
 * Adapter de entrada HTTP — POST /api/transcriptions.
 *
 * <p>Responsabilidades exclusivas deste adapter:
 * <ol>
 *   <li>Extrair bytes e metadados do {@code MultipartFile}</li>
 *   <li>Converter para {@code TranscribeCommand}</li>
 *   <li>Chamar {@code TranscribeAudioPort} (interface do caso de uso)</li>
 *   <li>Converter {@code TranscriptionResult} para {@code TranscriptionResponse}</li>
 * </ol>
 *
 * <p>O controller não conhece Redis, Whisper, SHA-256 nem RabbitMQ.
 */
@RestController
@RequestMapping("/api/transcriptions")
@RequiredArgsConstructor
public class TranscriptionController implements TranscriptionControllerDocumentation {

    private final TranscribeAudioPort transcribePort;

    @PostMapping
    public ResponseEntity<TranscriptionResponse> transcribe(
            @RequestPart("file") MultipartFile file) {

        byte[] audioBytes = extractBytes(file);

        TranscribeCommand command = new TranscribeCommand(
                audioBytes,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize()
        );

        TranscriptionResult result = transcribePort.transcribe(command);

        return ResponseEntity.ok(toResponse(result));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private byte[] extractBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (Exception ex) {
            throw new TranscriptionException("Falha ao ler bytes do arquivo", ex);
        }
    }

    private TranscriptionResponse toResponse(TranscriptionResult result) {
        return result.cached()
                ? new TranscriptionResponse(result.text(), result.fileSizeBytes(), result.transcriptionHash(), true)
                : new TranscriptionResponse(result.text(), result.fileSizeBytes(), result.transcriptionHash());
    }
}