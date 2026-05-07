package com.example.diospeechai.analysis.domain.port.out;

import java.util.Optional;

import com.example.diospeechai.analysis.application.result.SummaryResult;

/**
 * PORT DE SAÍDA — contrato de armazenamento de resumos.
 *
 * <p>Implementado por {@code RedisSummaryStoreAdapter} na v10.2.0.
 * Usa o mesmo Redis já existente no projeto — chave: {@code summary:{audioHash}}.
 */
public interface SummaryStorePort {

    Optional<SummaryResult> get(String audioHash);

    void put(String audioHash, SummaryResult result);
}