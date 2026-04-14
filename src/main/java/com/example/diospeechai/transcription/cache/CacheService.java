package com.example.diospeechai.transcription.cache;

import com.example.diospeechai.transcription.dto.TranscriptionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

/**
 * Gerencia o cache de transcrições no Redis.
 *
 * <p>Estratégia: o hash SHA-256 do conteúdo binário do arquivo de áudio
 * é usado como chave de cache. Arquivos idênticos (mesmo conteúdo, independente
 * do nome) sempre produzem o mesmo hash e reutilizam a transcrição cacheada.
 *
 * <p>Chave Redis: {@code transcription:{sha256hex}}
 * TTL padrão: 24 horas (configurável via {@code cache.transcription.ttl-hours})
 *
 * <p>Falhas de Redis são toleradas silenciosamente — se o cache estiver
 * indisponível, a requisição prossegue normalmente chamando o Whisper.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private static final String KEY_PREFIX = "transcription:";

    private final RedisTemplate<String, TranscriptionResponse> redisTemplate;

    @Value("${cache.transcription.ttl-hours:24}")
    private long ttlHours;

    /**
     * Calcula o SHA-256 do conteúdo do arquivo e verifica no Redis.
     *
     * @return a resposta cacheada, ou {@code null} se não houver cache (miss)
     */
    public TranscriptionResponse get(byte[] fileBytes) {
        String key = buildKey(fileBytes);
        try {
            TranscriptionResponse cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                log.info("Cache HIT | key={}", key);
            }
            return cached;
        } catch (Exception ex) {
            log.warn("Falha ao ler cache Redis — continuando sem cache | error={}", ex.getMessage());
            return null;
        }
    }

    /**
     * Armazena a transcrição no Redis com o TTL configurado.
     * Falhas são toleradas silenciosamente.
     */
    public void put(byte[] fileBytes, TranscriptionResponse response) {
        String key = buildKey(fileBytes);
        try {
            redisTemplate.opsForValue().set(key, response, Duration.ofHours(ttlHours));
            log.info("Cache STORE | key={} | ttl={}h", key, ttlHours);
        } catch (Exception ex) {
            log.warn("Falha ao gravar cache Redis — transcrição não cacheada | error={}", ex.getMessage());
        }
    }

    /** Computa a chave Redis a partir do SHA-256 do conteúdo do arquivo. */
    private String buildKey(byte[] fileBytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(fileBytes);
            return KEY_PREFIX + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            // SHA-256 é garantido pela JVM spec — nunca deve ocorrer
            throw new IllegalStateException("SHA-256 não disponível", ex);
        }
    }
}