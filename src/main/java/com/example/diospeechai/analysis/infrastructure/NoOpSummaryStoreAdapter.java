package com.example.diospeechai.analysis.infrastructure;

import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import com.example.diospeechai.analysis.application.result.SummaryResult;
import com.example.diospeechai.analysis.domain.port.out.SummaryStorePort;

import lombok.extern.slf4j.Slf4j;

/**
 * Implementação no-op do {@link SummaryStorePort}.
 *
 * <p>Ativo enquanto o adapter Redis não está configurado (v10.2.0).
 * Sempre retorna {@code Optional.empty()} — sem cache, LLM chamado a cada request.
 * Substituído por {@code RedisSummaryStoreAdapter} na v10.2.0 via {@code @Primary}.
 */
@Slf4j
@Component
@ConditionalOnMissingBean(name = "redisSummaryStoreAdapter")
public class NoOpSummaryStoreAdapter implements SummaryStorePort {

    @Override
    public Optional<SummaryResult> get(String audioHash) {
        log.debug("SummaryStore no-op — sem cache | hash={}", audioHash);
        return Optional.empty();
    }

    @Override
    public void put(String audioHash, SummaryResult result) {
        log.debug("SummaryStore no-op — resumo não persistido | hash={}", audioHash);
    }
}