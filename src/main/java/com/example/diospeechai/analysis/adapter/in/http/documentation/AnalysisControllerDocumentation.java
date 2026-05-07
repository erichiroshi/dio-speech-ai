package com.example.diospeechai.analysis.adapter.in.http.documentation;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.diospeechai.analysis.adapter.in.http.dto.SummaryResponse;

@Tag(name = "Análise", description = "Análise inteligente de transcrições via LLM local (Ollama)")
public interface AnalysisControllerDocumentation {

    @Operation(
        summary = "Gerar resumo da transcrição",
        description = """
                Gera um resumo em 2 a 4 frases para a transcrição identificada pelo audioHash.
                
                **Cache:** se o resumo já foi gerado anteriormente, retorna do Redis com
                `"cached": true` sem chamar o LLM novamente.
                
                **LLM local:** o resumo é gerado pelo Ollama rodando localmente.
                Sem API key, sem custo, 100% offline.
                
                **Performance:** primeira chamada leva 2–30s dependendo do modelo e hardware.
                Chamadas subsequentes retornam do cache em ~15ms.
                """
    )
    @ApiResponse(responseCode = "200", description = "Resumo gerado ou retornado do cache",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = SummaryResponse.class),
            examples = {
                @ExampleObject(name = "Gerado agora", value = """
                        {
                          "audioHash": "a1b2c3...",
                          "summary": "O áudio discute o planejamento do projeto.",
                          "model": "llama3.2:3b"
                        }"""),
                @ExampleObject(name = "Cache hit", value = """
                        {
                          "audioHash": "a1b2c3...",
                          "summary": "O áudio discute o planejamento do projeto.",
                          "model": "llama3.2:3b",
                          "cached": true
                        }""")
            }))
    @ApiResponse(responseCode = "404", description = "Transcrição não encontrada no cache")
    @ApiResponse(responseCode = "503", description = "Ollama indisponível")
    @PostMapping(value = "/{audioHash}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<SummaryResponse> summarize(
            @Parameter(description = "SHA-256 do áudio (retornado por POST /api/transcriptions)",
                       required = true, example = "a1b2c3d4...")
            @PathVariable String audioHash);
}