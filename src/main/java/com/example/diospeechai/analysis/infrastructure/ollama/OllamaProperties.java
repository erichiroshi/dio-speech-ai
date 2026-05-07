package com.example.diospeechai.analysis.infrastructure.ollama;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades do adapter Ollama.
 *
 * <p>Variáveis de ambiente:
 * <pre>
 *   OLLAMA_BASE_URL=http://localhost:11434
 *   OLLAMA_MODEL=llama3.2:1b
 * </pre>
 *
 * <p>Modelos recomendados por memória disponível:
 * <ul>
 *   <li>&lt; 8GB RAM: {@code llama3.2:1b} ou {@code gemma3:1b}</li>
 *   <li>8–16GB RAM: {@code llama3.2:3b} ou {@code mistral:7b}</li>
 *   <li>16GB+ RAM: {@code deepseek-r1:7b}</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "analysis.ollama")
public record OllamaProperties(
        String model
) {}