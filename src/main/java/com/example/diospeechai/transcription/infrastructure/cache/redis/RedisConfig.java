package com.example.diospeechai.transcription.infrastructure.cache.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.example.diospeechai.transcription.application.result.TranscriptionResult;

import tools.jackson.databind.ObjectMapper;

/**
 * Configuração do RedisTemplate para o cache de transcrições.
 *
 * <p>v7.4.0: tipo alterado de {@code TranscriptionResponse} para
 * {@code TranscriptionResult} — o cache agora opera com o objeto
 * do domínio application, não com o DTO HTTP.
 *
 * <p>Serialização:
 * <ul>
 *   <li>Chave: String pura — ex: {@code transcription:a1b2...}</li>
 *   <li>Valor: JSON via Jackson — {@code TranscriptionResult}</li>
 * </ul>
 */
@Configuration
public class RedisConfig {

    @Bean
    RedisTemplate<String, TranscriptionResult> redisTemplate(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper) {

        var keySerializer   = new StringRedisSerializer();
        var valueSerializer = new JacksonJsonRedisSerializer<>(objectMapper, TranscriptionResult.class);

        RedisTemplate<String, TranscriptionResult> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashKeySerializer(keySerializer);
        template.setHashValueSerializer(valueSerializer);
        template.afterPropertiesSet();

        return template;
    }
}