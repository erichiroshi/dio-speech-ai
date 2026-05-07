package com.example.diospeechai.analysis.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.diospeechai.analysis.application.command.SummarizeCommand;
import com.example.diospeechai.analysis.application.result.SummaryResult;
import com.example.diospeechai.analysis.domain.port.out.LanguageModelPort;
import com.example.diospeechai.analysis.domain.port.out.SummaryStorePort;
import com.example.diospeechai.analysis.exception.AnalysisUnavailableException;

/**
 * Testes unitários do {@link SummarizeTranscriptionUseCase}.
 * Zero Spring, zero Ollama, zero Redis — ~10ms.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SummarizeTranscriptionUseCase — testes unitários")
class SummarizeTranscriptionUseCaseTest {

    @Mock LanguageModelPort languageModelPort;
    @Mock SummaryStorePort  summaryStorePort;

    @InjectMocks SummarizeTranscriptionUseCase useCase;

    private static final String HASH = "abc123hash";
    private static final String TEXT = "texto longo da transcrição do áudio";

    private SummarizeCommand command;

    @BeforeEach
    void setUp() {
        command = new SummarizeCommand(HASH, TEXT);
    }

    // ── Cache HIT ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Cache hit: deve retornar resumo cacheado sem chamar o LLM")
    void shouldReturnCachedSummaryWithoutCallingLlm() {
        var cached = new SummaryResult(HASH, "resumo cacheado", "llama3.2:3b");
        when(summaryStorePort.get(HASH)).thenReturn(Optional.of(cached));

        SummaryResult result = useCase.summarize(command);

        assertThat(result.summary()).isEqualTo("resumo cacheado");
        assertThat(result.cached()).isTrue();
        verify(languageModelPort, never()).generate(any());
        verify(summaryStorePort, never()).put(anyString(), any());
    }

    // ── Cache MISS ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Cache miss: deve chamar LLM, armazenar e retornar com cached=false")
    void shouldCallLlmAndStoreOnCacheMiss() {
        when(summaryStorePort.get(HASH)).thenReturn(Optional.empty());
        when(languageModelPort.generate(any())).thenReturn("resumo gerado pelo llm");

        SummaryResult result = useCase.summarize(command);

        assertThat(result.summary()).isEqualTo("resumo gerado pelo llm");
        assertThat(result.cached()).isFalse();
        assertThat(result.audioHash()).isEqualTo(HASH);
        verify(summaryStorePort).put(HASH, result);
    }

    // ── Prompt contém o texto ─────────────────────────────────────────────────

    @Test
    @DisplayName("O prompt enviado ao LLM deve conter o texto transcrito")
    void shouldIncludeTranscribedTextInPrompt() {
        when(summaryStorePort.get(HASH)).thenReturn(Optional.empty());
        when(languageModelPort.generate(contains(TEXT))).thenReturn("ok");

        useCase.summarize(command);

        verify(languageModelPort).generate(contains(TEXT));
    }

    // ── Falha no LLM ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Falha no LLM deve relançar AnalysisUnavailableException sem armazenar")
    void shouldRethrowWhenLlmFails() {
        when(summaryStorePort.get(HASH)).thenReturn(Optional.empty());
        when(languageModelPort.generate(any()))
                .thenThrow(new AnalysisUnavailableException("Ollama indisponível"));

        assertThatThrownBy(() -> useCase.summarize(command))
                .isInstanceOf(AnalysisUnavailableException.class)
                .hasMessage("Ollama indisponível");

        verify(summaryStorePort, never()).put(anyString(), any());
    }
}