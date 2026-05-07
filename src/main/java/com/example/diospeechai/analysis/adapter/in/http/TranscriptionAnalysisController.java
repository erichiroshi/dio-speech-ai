package com.example.diospeechai.analysis.adapter.in.http;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.diospeechai.analysis.adapter.in.http.documentation.AnalysisControllerDocumentation;
import com.example.diospeechai.analysis.adapter.in.http.dto.SummaryResponse;
import com.example.diospeechai.analysis.application.command.SummarizeCommand;
import com.example.diospeechai.analysis.application.result.SummaryResult;
import com.example.diospeechai.analysis.domain.port.in.SummarizeTranscriptionPort;
import com.example.diospeechai.transcription.domain.port.out.TranscriptionCachePort;
import com.example.diospeechai.transcription.exception.TranscriptionException;

import lombok.RequiredArgsConstructor;

/**
 * Adapter de entrada HTTP para análise de transcrições.
 *
 * <p>Responsabilidades:
 * <ol>
 *   <li>Buscar o texto transcrito no Redis pelo {@code audioHash}</li>
 *   <li>Converter para {@code SummarizeCommand}</li>
 *   <li>Chamar {@code SummarizeTranscriptionPort}</li>
 *   <li>Converter {@code SummaryResult} → {@code SummaryResponse}</li>
 * </ol>
 *
 * <p>Reutiliza o {@link TranscriptionCachePort} já existente para buscar
 * a transcrição — sem duplicar lógica de cache.
 */
@RestController
@RequestMapping("/api/transcriptions")
@RequiredArgsConstructor
public class TranscriptionAnalysisController implements AnalysisControllerDocumentation {

    private final SummarizeTranscriptionPort summarizePort;
    private final TranscriptionCachePort     transcriptionCachePort;

    @Override
    @PostMapping("/{audioHash}/analysis")
    public ResponseEntity<SummaryResponse> summarize(@PathVariable String audioHash) {

        // Busca a transcrição existente no cache
        var transcriptionResult = transcriptionCachePort.get(audioHash)
                .orElseThrow(() -> new TranscriptionException(
                        "Transcrição não encontrada para o hash: " + audioHash +
                        ". Certifique-se de que o áudio foi transcrito antes de solicitar análise."));

        SummarizeCommand command = new SummarizeCommand(
                audioHash,
                transcriptionResult.text()
        );

        SummaryResult result = summarizePort.summarize(command);

        return ResponseEntity.ok(toResponse(result));
    }

    private SummaryResponse toResponse(SummaryResult result) {
        return result.cached()
                ? new SummaryResponse(result.audioHash(), result.summary(), result.model(), true)
                : new SummaryResponse(result.audioHash(), result.summary(), result.model());
    }
}