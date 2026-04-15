package com.example.diospeechai.transcription.documentation;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.diospeechai.transcription.dto.TranscriptionResponse;

@Tag(name = "Transcrição", description = "Transcrição de áudio para texto via Whisper")
public interface TranscriptionControllerDocumentation {


    @Operation(
        summary = "Transcrever arquivo de áudio",
        description = """
                Recebe um arquivo de áudio e retorna a transcrição em texto.
                
                **Cache inteligente:** o SHA-256 do conteúdo binário do arquivo é calculado
                antes de enviar ao Whisper. Se o mesmo áudio já foi transcrito (independente
                do nome do arquivo), a resposta é retornada do cache Redis em ~15ms com
                `"cached": true`. Em caso de cache miss, o Whisper processa e o resultado
                é armazenado por 24h (configurável).
                
                **Resiliência:** a chamada ao Whisper é protegida por CircuitBreaker + Retry.
                Se o Whisper estiver indisponível, retorna 503.
                """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Transcrição realizada com sucesso",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = TranscriptionResponse.class),
                examples = {
                    @ExampleObject(
                        name = "Cache miss",
                        summary = "Primeira transcrição (Whisper processou)",
                        value = """
                                {
                                  "text": "testando o áudio para a gravação e teste da API",
                                  "fileSizeBytes": 461842
                                }
                                """
                    ),
                    @ExampleObject(
                        name = "Cache hit",
                        summary = "Mesmo áudio — retornou do Redis (~15ms)",
                        value = """
                                {
                                  "text": "testando o áudio para a gravação e teste da API",
                                  "fileSizeBytes": 461842,
                                  "cached": true
                                }
                                """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Arquivo ausente ou tipo não suportado",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                examples = @ExampleObject(value = """
                        {
                          "type": "https://api.diospeechai/errors/bad-request",
                          "title": "Bad request",
                          "status": 400,
                          "detail": "Tipo de arquivo inválido: 'application/pdf'. Tipos aceitos: [audio/wav, audio/mpeg, audio/wave, audio/x-wav]",
                          "timestamp": "2026-04-11T12:00:00Z",
                          "requestId": "a3f2c1d0-7f3e-4b2a-9c1d-e8f5a6b7c8d9"
                        }
                        """)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token JWT ausente ou inválido",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                examples = @ExampleObject(value = """
                        {
                          "type": "https://api.diospeechai/errors/unauthorized",
                          "title": "Unauthorized",
                          "status": 401,
                          "detail": "Token JWT ausente ou inválido"
                        }
                        """)
            )
        ),
        @ApiResponse(
            responseCode = "503",
            description = "Serviço de transcrição indisponível (CircuitBreaker aberto)",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                examples = @ExampleObject(value = """
                        {
                          "type": "https://api.diospeechai/errors/service-unavailable",
                          "title": "Service Unavailable",
                          "status": 503,
                          "detail": "Serviço de transcrição temporariamente indisponível. Tente novamente em instantes.",
                          "requestId": "b4e3d2c1-8g4f-5c3b-0d2e-f9g6b7c8d9e0"
                        }
                        """)
            )
        )
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<TranscriptionResponse> transcribe(
            @Parameter(
                description = "Arquivo de áudio para transcrição. Formatos aceitos: WAV, MPEG. Tamanho máximo: 5MB.",
                required = true
            )
            @RequestPart("file") MultipartFile file); 
}