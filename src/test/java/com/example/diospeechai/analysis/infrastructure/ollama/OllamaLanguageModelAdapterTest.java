package com.example.diospeechai.analysis.infrastructure.ollama;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.diospeechai.analysis.exception.AnalysisUnavailableException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OllamaLanguageModelAdapter — testes unitários")
class OllamaLanguageModelAdapterTest {

    @Mock
    private OllamaChatModel ollamaChatModel;

    @Mock
    private OllamaProperties properties;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec responseSpec;

    private OllamaLanguageModelAdapter adapter;

    @BeforeEach
    void setUp() {
        when(properties.model()).thenReturn("llama3.2:1b");

        adapter = new OllamaLanguageModelAdapter(ollamaChatModel, properties);

        // sobrescreve o chatClient criado internamente
        ReflectionTestUtils.setField(adapter, "chatClient", chatClient);
    }

    @Test
    @DisplayName("Deve gerar resposta com sucesso")
    void shouldGenerateResponseSuccessfully() {

        String prompt = "Explique arquitetura hexagonal";
        String expected = "Arquitetura hexagonal desacopla domínio de infraestrutura.";

        when(chatClient.prompt()).thenReturn(requestSpec);

        when(requestSpec.options(any(OllamaChatOptions.Builder.class)))
                .thenReturn(requestSpec);

        when(requestSpec.user(prompt))
                .thenReturn(requestSpec);

        when(requestSpec.call())
                .thenReturn(responseSpec);

        when(responseSpec.content())
                .thenReturn(expected);

        String result = adapter.generate(prompt);

        assertThat(result).isEqualTo(expected);

        verify(chatClient).prompt();

        verify(requestSpec).options(any(OllamaChatOptions.Builder.class));
        verify(requestSpec).user(prompt);
        verify(requestSpec).call();

        verify(responseSpec).content();
    }

    @Test
    @DisplayName("Deve lançar AnalysisUnavailableException quando Ollama falha")
    void shouldThrowAnalysisUnavailableExceptionWhenOllamaFails() {

        String prompt = "teste";

        when(chatClient.prompt()).thenReturn(requestSpec);

        when(requestSpec.options(any(OllamaChatOptions.Builder.class)))
                .thenReturn(requestSpec);

        when(requestSpec.user(prompt))
                .thenReturn(requestSpec);

        when(requestSpec.call())
                .thenThrow(new RuntimeException("Ollama offline"));

        assertThatThrownBy(() -> adapter.generate(prompt))
                .isInstanceOf(AnalysisUnavailableException.class)
                .hasMessageContaining("Serviço de análise temporariamente indisponível")
                .hasCauseInstanceOf(RuntimeException.class);

        verify(chatClient).prompt();

        verify(requestSpec).options(any(OllamaChatOptions.Builder.class));
        verify(requestSpec).user(prompt);
        verify(requestSpec).call();
    }
}