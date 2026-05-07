package com.example.diospeechai.analysis.domain.port.out;

/**
 * PORT DE SAÍDA — contrato do modelo de linguagem.
 *
 * <p>O domínio define o contrato. A infraestrutura implementa.
 * Hoje: {@code OllamaLanguageModelAdapter} via Spring AI.
 * Amanhã: OpenAI, Gemini, Anthropic — sem mudar o domínio.
 */
public interface LanguageModelPort {

    /**
     * Envia um prompt ao LLM e retorna a resposta em texto.
     *
     * @param prompt instrução completa para o modelo
     * @return texto gerado pelo LLM
     * @throws com.example.diospeechai.analysis.exception.AnalysisUnavailableException
     *         se o serviço de LLM estiver indisponível
     */
    String generate(String prompt);
}