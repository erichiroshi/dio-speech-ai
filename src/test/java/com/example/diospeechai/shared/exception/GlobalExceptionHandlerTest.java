package com.example.diospeechai.shared.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.diospeechai.transcription.adapter.in.http.TranscriptionController;
import com.example.diospeechai.transcription.domain.port.in.TranscribeAudioPort;
import com.example.diospeechai.transcription.exception.ServiceUnavailableException;
import com.example.diospeechai.transcription.exception.TranscriptionException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes unitários do {@link GlobalExceptionHandler}.
 *
 * <p>Usa {@code @WebMvcTest} para subir apenas a camada web,
 * sem Redis, RabbitMQ ou Testcontainers. Cada teste verifica
 * que o handler correto é acionado e retorna o ProblemDetail esperado.
 */
@WebMvcTest(TranscriptionController.class)
@DisplayName("GlobalExceptionHandler — testes unitários")
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    TranscribeAudioPort transcribePort;

    // ── TranscriptionException → 400 ─────────────────────────────────────────

    @Test
    @DisplayName("TranscriptionException deve retornar 400 com title correto")
    void shouldReturn400OnTranscriptionException() throws Exception {
        when(transcribePort.transcribe(any()))
                .thenThrow(new TranscriptionException("Tipo de arquivo inválido"));

        mockMvc.perform(multipart("/api/transcriptions")
                        .file("file", "content".getBytes())
                        .contentType("multipart/form-data"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Transcription Exception"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Tipo de arquivo inválido"))
                .andExpect(jsonPath("$.type").value("https://api.diospeechai/errors/transcription-exception"));
    }

    // ── ServiceUnavailableException → 503 ────────────────────────────────────

    @Test
    @DisplayName("ServiceUnavailableException deve retornar 503")
    void shouldReturn503OnServiceUnavailableException() throws Exception {
        when(transcribePort.transcribe(any()))
                .thenThrow(new ServiceUnavailableException("Whisper indisponível"));

        mockMvc.perform(multipart("/api/transcriptions")
                        .file("file", "content".getBytes())
                        .contentType("multipart/form-data"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Service Unavailable"))
                .andExpect(jsonPath("$.status").value(503));
    }

    // ── MissingServletRequestPartException → 400 ────────────────────────────

    @Test
    @DisplayName("Arquivo ausente deve retornar 400 com mensagem de campo obrigatório")
    void shouldReturn400WhenFileMissing() throws Exception {
        mockMvc.perform(multipart("/api/transcriptions"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requisição Inválida"))
                .andExpect(jsonPath("$.detail").value("O campo 'file' é obrigatório."));
    }

    // ── HttpRequestMethodNotSupportedException → 405 ────────────────────────

    @Test
    @DisplayName("GET em endpoint POST deve retornar 405")
    void shouldReturn405OnWrongMethod() throws Exception {
        mockMvc.perform(get("/api/transcriptions"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.title").value("Método Não Permitido"))
                .andExpect(jsonPath("$.status").value(405));
    }

    // ── NoResourceFoundException → 404 ───────────────────────────────────────

    @Test
    @DisplayName("Endpoint inexistente deve retornar 404")
    void shouldReturn404OnUnknownPath() throws Exception {
        mockMvc.perform(get("/nao-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Não Encontrado"))
                .andExpect(jsonPath("$.status").value(404));
    }

    // ── Exception genérica → 500 ──────────────────────────────────────────────

    @Test
    @DisplayName("Exception inesperada deve retornar 500")
    void shouldReturn500OnUnexpectedException() throws Exception {
        when(transcribePort.transcribe(any()))
                .thenThrow(new RuntimeException("erro inesperado"));

        mockMvc.perform(multipart("/api/transcriptions")
                        .file("file", "content".getBytes())
                        .contentType("multipart/form-data"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Erro Interno"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.type").value("https://api.diospeechai/errors/internal-server-error"));
    }

    // ── IllegalArgumentException → 400 ───────────────────────────────────────

    @Test
    @DisplayName("IllegalArgumentException deve retornar 400")
    void shouldReturn400OnIllegalArgument() throws Exception {
        when(transcribePort.transcribe(any()))
                .thenThrow(new IllegalArgumentException("argumento inválido"));

        mockMvc.perform(multipart("/api/transcriptions")
                        .file("file", "content".getBytes())
                        .contentType("multipart/form-data"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requisição Inválida"))
                .andExpect(jsonPath("$.status").value(400));
    }

    // ── ProblemDetail — campos obrigatórios ───────────────────────────────────

    @Test
    @DisplayName("ProblemDetail deve conter timestamp e type em todos os erros")
    void shouldIncludeTimestampAndTypeInAllErrors() throws Exception {
        mockMvc.perform(get("/nao-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.type").exists());
    }
}