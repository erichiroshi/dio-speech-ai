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
 * <p>v10.2.0: {@code audioHash} incluído na resposta para que o cliente
 * possa chamar {@code POST /api/transcriptions/{audioHash}/analysis}
 * diretamente sem calcular o hash.
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

    private byte[] extractBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (Exception ex) {
            throw new TranscriptionException("Falha ao ler bytes do arquivo", ex);
        }
    }

    private TranscriptionResponse toResponse(TranscriptionResult result) {
        if (result.cached()) {
            // Em cache hit o audioHash já está no result
            return new TranscriptionResponse(
                    result.text(), result.fileSizeBytes(),
                    result.audioHash(), true);
        }
        return new TranscriptionResponse(
                result.text(), result.fileSizeBytes(),
                result.audioHash());
    }
}