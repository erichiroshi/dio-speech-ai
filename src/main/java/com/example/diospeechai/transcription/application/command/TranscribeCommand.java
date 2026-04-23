package com.example.diospeechai.transcription.application.command;

/**
 * Objeto de entrada do caso de uso {@code TranscribeAudioUseCase}.
 *
 * <p>Carrega os dados do áudio sem acoplamento com {@code MultipartFile}
 * (HTTP) nem com mensagens RabbitMQ. O adapter de entrada converte
 * sua tecnologia específica para este comando antes de chamar o port.
 *
 * <p>Design: record imutável — criado uma vez, nunca modificado.
 *
 * @param audioBytes   conteúdo binário do arquivo de áudio
 * @param filename     nome original do arquivo (para logs e MDC)
 * @param fileSizeBytes tamanho em bytes (para métricas e resposta)
 */
public record TranscribeCommand(
        byte[] audioBytes,
        String filename,
        long fileSizeBytes
) {}