package com.example.diospeechai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.example.diospeechai.transcription.dto.TranscriptionResponse;

import tools.jackson.databind.ObjectMapper;

/**
 * Configuração do RedisTemplate para o cache de transcrições.
 *
 * <p>
 * Estratégia de serialização:
 * <ul>
 * <li>Chave (key): String pura — ex: {@code transcription:a1b2c3...}</li>
 * <li>Valor (value): JSON via Jackson — {@code TranscriptionResponse}
 * serializado</li>
 * </ul>
 *
 * <p>
 * O uso de {@code JacksonJsonRedisSerializer} tipado garante que a
 * deserialização recrie corretamente o record {@code TranscriptionResponse} sem
 * necessidade de informações de tipo embutidas no JSON (diferente de
 * {@code GenericJacksonJsonRedisSerializer}).
 */
@Configuration
public class RedisConfig {

	@Bean
	RedisTemplate<String, TranscriptionResponse> redisTemplate(RedisConnectionFactory connectionFactory,
			ObjectMapper objectMapper) {

		var keySerializer = new StringRedisSerializer();
		var valueSerializer = new JacksonJsonRedisSerializer<TranscriptionResponse>(objectMapper,
				TranscriptionResponse.class);

		RedisTemplate<String, TranscriptionResponse> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);
		template.setKeySerializer(keySerializer);
		template.setValueSerializer(valueSerializer);
		template.setHashKeySerializer(keySerializer);
		template.setHashValueSerializer(valueSerializer);
		template.afterPropertiesSet();

		return template;
	}
}