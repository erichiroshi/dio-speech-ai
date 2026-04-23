package com.example.diospeechai.transcription.domain.port.in;

import com.example.diospeechai.transcription.application.command.TranscribeCommand;
import com.example.diospeechai.transcription.application.result.TranscriptionResult;

/**
 * PORT DE ENTRADA — contrato do caso de uso de transcrição.
 *
 * <p>É a interface que todos os adapters de entrada usam para acionar
 * a lógica de negócio:
 * <ul>
 *   <li>{@code TranscriptionController} (HTTP) — v7.5.0</li>
 *   <li>{@code TranscriptionRequestConsumer} (RabbitMQ) — Fase 8</li>
 * </ul>
 *
 * <p>Ambos chamam {@code transcribe(command)} sem saber como o outro funciona.
 * O caso de uso não sabe de onde veio o pedido.
 */
public interface TranscribeAudioPort {

    /**
     * Executa o fluxo completo de transcrição:
     * validação → cache → IA → cache put → evento → métricas.
     *
     * @param command dados do áudio a transcrever
     * @return resultado com texto e metadados
     */
    TranscriptionResult transcribe(TranscribeCommand command);
}