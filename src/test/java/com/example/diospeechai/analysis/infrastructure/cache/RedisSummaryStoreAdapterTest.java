package com.example.diospeechai.analysis.infrastructure.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.diospeechai.analysis.application.result.SummaryResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários do {@link RedisSummaryStoreAdapter}.
 * Zero Spring, zero Redis real — Mockito puro.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RedisSummaryStoreAdapter — testes unitários")
class RedisSummaryStoreAdapterTest {

    @Mock RedisTemplate<String, SummaryResult> redisTemplate;
    @Mock ValueOperations<String, SummaryResult> valueOps;

    RedisSummaryStoreAdapter adapter;

    private static final String HASH   = "abc123";
    private static final String KEY    = "summary:" + HASH;
    private static final SummaryResult RESULT =
            new SummaryResult(HASH, "resumo do texto", "llama3.2:3b");

    @BeforeEach
    void setUp() {
        adapter = new RedisSummaryStoreAdapter(redisTemplate);
        ReflectionTestUtils.setField(adapter, "ttlHours", 72L);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    @DisplayName("get: deve retornar Optional com resumo quando chave existe")
    void shouldReturnCachedSummaryOnHit() {
        when(valueOps.get(KEY)).thenReturn(RESULT);
        assertThat(adapter.get(HASH)).isPresent()
                .hasValueSatisfying(r -> assertThat(r.summary()).isEqualTo("resumo do texto"));
    }

    @Test
    @DisplayName("get: deve retornar Optional vazio quando chave não existe")
    void shouldReturnEmptyOnMiss() {
        when(valueOps.get(KEY)).thenReturn(null);
        assertThat(adapter.get(HASH)).isEmpty();
    }

    @Test
    @DisplayName("get: deve retornar Optional vazio e não lançar quando Redis falha")
    void shouldReturnEmptyWhenRedisGetFails() {
        when(valueOps.get(anyString())).thenThrow(new RuntimeException("connection refused"));
        assertThat(adapter.get(HASH)).isEmpty();
    }

    @Test
    @DisplayName("put: deve armazenar com TTL configurado")
    void shouldStoreWithTtl() {
        adapter.put(HASH, RESULT);
        verify(valueOps).set(eq(KEY), eq(RESULT), any());
    }

    @Test
    @DisplayName("put: não deve lançar quando Redis falha")
    void shouldNotThrowWhenRedisSetFails() {
        doThrow(new RuntimeException("timeout")).when(valueOps).set(anyString(), any(), any());
        assertThatNoException()
        .isThrownBy(() -> adapter.put(HASH, RESULT)); // não lança
    }
}