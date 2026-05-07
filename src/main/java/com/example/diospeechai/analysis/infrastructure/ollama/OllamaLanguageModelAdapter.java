package com.example.diospeechai.analysis.infrastructure.ollama;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import com.example.diospeechai.analysis.domain.port.out.LanguageModelPort;
import com.example.diospeechai.analysis.exception.AnalysisUnavailableException;

import lombok.extern.slf4j.Slf4j;

/**
 * Adapter de saída — implementa {@link LanguageModelPort} via Spring AI + Ollama.
 *
 * <p>Usa {@link ChatClient} do Spring AI — a abstração de alto nível que
 * encapsula o prompt, as opções do modelo e a chamada HTTP ao Ollama.
 *
 * <p>Para trocar de Ollama para OpenAI ou qualquer outro LLM:
 * criar {@code OpenAiLanguageModelAdapter implements LanguageModelPort} com
 * {@code @Primary}. O domínio e o use case não mudam.
 */
@Slf4j
@Component
@EnableConfigurationProperties(OllamaProperties.class)
public class OllamaLanguageModelAdapter implements LanguageModelPort {

    private final ChatClient chatClient;
    private final OllamaProperties properties;

    public OllamaLanguageModelAdapter(OllamaChatModel ollamaChatModel,
                                       OllamaProperties properties) {
        this.chatClient = ChatClient.builder(ollamaChatModel).build();
        this.properties = properties;
    }

    @Override
    public String generate(String prompt) {
        log.debug("Enviando prompt ao Ollama | model={} | promptChars={}",
                properties.model(), prompt.length());
        try {
            String response = chatClient.prompt()
                    .options(OllamaChatOptions.builder()
                            .model(properties.model())
                            )
                    .user(prompt)
                    .call()
                    .content();

            log.debug("Resposta recebida do Ollama | chars={}", response.length());
            return response;

        } catch (Exception ex) {
            log.error("Falha na chamada ao Ollama | model={} | error={}",
                    properties.model(), ex.getMessage());
            throw new AnalysisUnavailableException(
                    "Serviço de análise temporariamente indisponível. Tente novamente em instantes.", ex);
        }
    }
}