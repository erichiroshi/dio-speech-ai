package com.example.diospeechai.transcription.infrastructure.cache;

import java.time.Duration;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.example.diospeechai.transcription.application.result.TranscriptionResult;
import com.example.diospeechai.transcription.domain.port.out.TranscriptionCachePort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Adapter de saída — implementa {@link TranscriptionCachePort} via Redis.
 *
 * <p>Chave Redis: {@code transcription:{sha256hex}}
 * (o hash vem calculado pelo {@code TranscribeAudioUseCase}).
 *
 * <p>TTL configurável via {@code cache.transcription.ttl-hours} (padrão: 24h).
 *
 * <p>Falhas de Redis são toleradas silenciosamente — o sistema funciona
 * sem cache, apenas sem o benefício de performance.
 *
 * <p>Para trocar Redis por outro backend (Caffeine, Memcached...):
 * criar novo adapter que implemente {@link TranscriptionCachePort}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisTranscriptionCacheAdapter implements TranscriptionCachePort {

    private static final String KEY_PREFIX = "transcription:";

    private final RedisTemplate<String, TranscriptionResult> redisTemplate;

    @Value("${cache.transcription.ttl-hours:24}")
    private long ttlHours;

    @Override
    public Optional<TranscriptionResult> get(String audioHash) {
        String key = KEY_PREFIX + audioHash;
        try {
            TranscriptionResult cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                log.info("Cache HIT | key={}", key);
            }
            return Optional.ofNullable(cached);
        } catch (Exception ex) {
            log.warn("Falha ao ler cache Redis — continuando sem cache | error={}", ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void put(String audioHash, TranscriptionResult result) {
        String key = KEY_PREFIX + audioHash;
        try {
            redisTemplate.opsForValue().set(key, result, Duration.ofHours(ttlHours));
            log.info("Cache STORE | key={} | ttl={}h", key, ttlHours);
        } catch (Exception ex) {
            log.warn("Falha ao gravar cache Redis | error={}", ex.getMessage());
        }
    }
}