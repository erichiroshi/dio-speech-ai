package com.example.diospeechai.transcription.domain.port.out;

import java.util.Optional;

import com.example.diospeechai.transcription.application.result.TranscriptionResult;

/**
 * PORT DE SAÍDA — contrato do cache de transcrições.
 *
 * <p>O domínio define o contrato. A infraestrutura implementa.
 * Hoje: {@code RedisTranscriptionCacheAdapter}.
 * Futuramente pode ser Caffeine (cache local), Memcached, ou qualquer outro
 * sem impacto no caso de uso.
 *
 * <p>A chave é o SHA-256 (hex) do conteúdo binário do áudio — dois arquivos
 * com conteúdo idêntico (independente do nome) sempre acertam o mesmo cache.
 */
public interface TranscriptionCachePort {

    /**
     * Busca uma transcrição pelo hash do áudio.
     *
     * @param audioHash SHA-256 hex do conteúdo do arquivo
     * @return {@code Optional} com o resultado cacheado, ou vazio se não houver
     */
    Optional<TranscriptionResult> get(String audioHash);

    /**
     * Armazena uma transcrição no cache.
     *
     * <p>Falhas devem ser toleradas silenciosamente — o sistema funciona
     * sem cache, apenas sem o benefício da performance.
     *
     * @param audioHash SHA-256 hex do conteúdo do arquivo
     * @param result    resultado da transcrição a ser cacheado
     */
    void put(String audioHash, TranscriptionResult result);
}