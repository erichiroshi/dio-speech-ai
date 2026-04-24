package com.example.diospeechai.transcription.infrastructure.cache;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.diospeechai.transcription.application.result.TranscriptionResult;
import com.example.diospeechai.transcription.infrastructure.cache.redis.RedisTranscriptionCacheAdapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários do {@link RedisTranscriptionCacheAdapter}.
 *
 * <p>Cobre: cache hit, cache miss, falha no get (Redis down) e falha no put.
 * Zero Spring context — Mockito puro, ~10ms por teste.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RedisTranscriptionCacheAdapter — testes unitários")
class RedisTranscriptionCacheAdapterTest {

    @Mock
    RedisTemplate<String, TranscriptionResult> redisTemplate;

    @Mock
    ValueOperations<String, TranscriptionResult> valueOps;

    RedisTranscriptionCacheAdapter adapter;

    private static final String HASH   = "abc123hash";
    private static final String KEY    = "transcription:" + HASH;
    private static final TranscriptionResult RESULT =
            new TranscriptionResult("texto transcrito", 1000L);

    @BeforeEach
    void setUp() {
        adapter = new RedisTranscriptionCacheAdapter(redisTemplate);
        // Injetar ttlHours via reflection (campo @Value)
        ReflectionTestUtils.setField(adapter, "ttlHours", 24L);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    // ── get — cache hit ───────────────────────────────────────────────────────

    @Test
    @DisplayName("get: deve retornar Optional com resultado quando chave existe no Redis")
    void shouldReturnCachedResultOnHit() {
        when(valueOps.get(KEY)).thenReturn(RESULT);

        Optional<TranscriptionResult> result = adapter.get(HASH);

        assertThat(result).isPresent();
        assertThat(result.get().text()).isEqualTo("texto transcrito");
    }

    // ── get — cache miss ──────────────────────────────────────────────────────

    @Test
    @DisplayName("get: deve retornar Optional vazio quando chave não existe")
    void shouldReturnEmptyOnMiss() {
        when(valueOps.get(KEY)).thenReturn(null);

        Optional<TranscriptionResult> result = adapter.get(HASH);

        assertThat(result).isEmpty();
    }

    // ── get — falha Redis ─────────────────────────────────────────────────────

    @Test
    @DisplayName("get: deve retornar Optional vazio e NÃO lançar exceção quando Redis falha")
    void shouldReturnEmptyAndNotThrowWhenRedisGetFails() {
        when(valueOps.get(anyString()))
                .thenThrow(new RuntimeException("Redis connection refused"));

        // Não deve lançar — falha silenciosa
        Optional<TranscriptionResult> result = adapter.get(HASH);

        assertThat(result).isEmpty();
    }

    // ── put — caminho feliz ───────────────────────────────────────────────────

    @Test
    @DisplayName("put: deve armazenar resultado com TTL configurado")
    void shouldStoreResultWithTtl() {
        adapter.put(HASH, RESULT);

        verify(valueOps).set(eq(KEY), eq(RESULT), any());
    }

    // ── put — falha Redis ─────────────────────────────────────────────────────

    @Test
    @DisplayName("put: deve NÃO lançar exceção quando Redis falha ao gravar")
    void shouldNotThrowWhenRedisSetFails() {
        doThrow(new RuntimeException("Redis timeout"))
                .when(valueOps).set(anyString(), any(), any());

        // Não deve lançar — falha silenciosa
        assertDoesNotThrow(() -> adapter.put(HASH, RESULT));
        // Se chegou aqui sem exceção, o teste passou
    }
}