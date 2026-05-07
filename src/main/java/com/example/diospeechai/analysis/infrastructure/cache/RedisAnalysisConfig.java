package com.example.diospeechai.analysis.infrastructure.cache;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.example.diospeechai.analysis.application.result.SummaryResult;

import tools.jackson.databind.ObjectMapper;

/**
 * Bean RedisTemplate tipado para {@link SummaryResult}.
 *
 * <p>Bean separado do {@code RedisConfig} de transcrição para evitar
 * conflito de nome — cada domínio tem seu próprio template tipado.
 */
@Configuration
public class RedisAnalysisConfig {

    @Bean
    RedisTemplate<String, SummaryResult> summaryRedisTemplate(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper) {

        var keySerializer   = new StringRedisSerializer();
        var valueSerializer = new JacksonJsonRedisSerializer<>(objectMapper, SummaryResult.class);

        RedisTemplate<String, SummaryResult> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashKeySerializer(keySerializer);
        template.setHashValueSerializer(valueSerializer);
        template.afterPropertiesSet();

        return template;
    }
}