package com.example.diospeechai.analysis.application;

import org.springframework.stereotype.Service;

import com.example.diospeechai.analysis.application.command.SummarizeCommand;
import com.example.diospeechai.analysis.application.result.SummaryResult;
import com.example.diospeechai.analysis.domain.port.in.SummarizeTranscriptionPort;
import com.example.diospeechai.analysis.domain.port.out.LanguageModelPort;
import com.example.diospeechai.analysis.domain.port.out.SummaryStorePort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Caso de uso: gerar resumo de uma transcrição via LLM.
 *
 * <p>Fluxo:
 * <ol>
 *   <li>Verifica cache — se já resumido, retorna com {@code cached=true}</li>
 *   <li>Constrói prompt com o texto transcrito</li>
 *   <li>Chama {@link LanguageModelPort#generate} — Ollama localmente</li>
 *   <li>Armazena no cache via {@link SummaryStorePort}</li>
 *   <li>Retorna {@link SummaryResult}</li>
 * </ol>
 *
 * <p>Não conhece Spring AI, Ollama, Redis nem HTTP.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SummarizeTranscriptionUseCase implements SummarizeTranscriptionPort {

    private final LanguageModelPort   languageModelPort;
    private final SummaryStorePort    summaryStorePort;

    @Override
    public SummaryResult summarize(SummarizeCommand command) {
        log.info("Solicitação de resumo | hash={}", command.audioHash());

        // 1. Cache check
        return summaryStorePort.get(command.audioHash())
                .map(cached -> {
                    log.info("Resumo encontrado no cache | hash={}", command.audioHash());
                    return cached.asCached();
                })
                .orElseGet(() -> generateAndStore(command));
    }

    private SummaryResult generateAndStore(SummarizeCommand command) {
        String prompt = buildPrompt(command.transcribedText());

        log.info("Gerando resumo via LLM | hash={}", command.audioHash());
        String summary = languageModelPort.generate(prompt);

        SummaryResult result = new SummaryResult(command.audioHash(), summary, "ollama");
        summaryStorePort.put(command.audioHash(), result);

        log.info("Resumo gerado e armazenado | hash={} | chars={}",
                command.audioHash(), summary.length());
        return result;
    }

    private String buildPrompt(String transcribedText) {
        return """
                Você é um assistente especializado em resumir transcrições de áudio.
                
                Resuma o seguinte texto transcrito em 2 a 4 frases objetivas em português.
                Mantenha os pontos principais. Não adicione informações externas.
                Responda APENAS com o resumo, sem introduções ou explicações adicionais.
                
                Texto:
                %s
                """.formatted(transcribedText);
    }
}