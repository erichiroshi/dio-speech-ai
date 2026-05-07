package com.example.diospeechai.analysis.infrastructure.cache;

import java.time.Duration;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.example.diospeechai.analysis.application.result.SummaryResult;
import com.example.diospeechai.analysis.domain.port.out.SummaryStorePort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Adapter de saída — implementa {@link SummaryStorePort} via Redis.
 *
 * <p>Chave: {@code summary:{audioHash}} — separada da chave de transcrição
 * ({@code transcription:{audioHash}}) para evitar conflito de tipo no Redis.
 *
 * <p>{@code @Primary} substitui o {@code NoOpSummaryStoreAdapter}.
 * Falhas de Redis são toleradas silenciosamente.
 */
@Slf4j
@Primary
@Component("redisSummaryStoreAdapter")
@RequiredArgsConstructor
public class RedisSummaryStoreAdapter implements SummaryStorePort {

    private static final String KEY_PREFIX = "summary:";

    private final RedisTemplate<String, SummaryResult> summaryRedisTemplate;

    @Value("${cache.summary.ttl-hours:72}")
    private long ttlHours;

    @Override
    public Optional<SummaryResult> get(String audioHash) {
        String key = KEY_PREFIX + audioHash;
        try {
            SummaryResult cached = summaryRedisTemplate.opsForValue().get(key);
            if (cached != null) {
                log.info("Cache HIT (resumo) | key={}", key);
            }
            return Optional.ofNullable(cached);
        } catch (Exception ex) {
            log.warn("Falha ao ler cache de resumo | key={} | error={}", key, ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void put(String audioHash, SummaryResult result) {
        String key = KEY_PREFIX + audioHash;
        try {
            summaryRedisTemplate.opsForValue().set(key, result, Duration.ofHours(ttlHours));
            log.info("Cache STORE (resumo) | key={} | ttl={}h", key, ttlHours);
        } catch (Exception ex) {
            log.warn("Falha ao gravar cache de resumo | key={} | error={}", key, ex.getMessage());
        }
    }
}