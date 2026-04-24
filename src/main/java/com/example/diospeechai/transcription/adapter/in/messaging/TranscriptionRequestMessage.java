package com.example.diospeechai.transcription.adapter.in.messaging;

/**
 * DTO da mensagem recebida na fila {@code transcription.requests}.
 *
 * <p>Representa um pedido de transcrição enviado por outro serviço via RabbitMQ.
 * O áudio é codificado em Base64 para compatibilidade com JSON sobre AMQP.
 *
 * <p>O consumer converte este DTO para {@code TranscribeCommand} antes de
 * chamar o caso de uso — o domínio nunca vê este objeto.
 *
 * @param audioBase64    conteúdo binário do áudio codificado em Base64
 * @param filename       nome do arquivo (para logs e MDC)
 * @param contentType    MIME type do áudio (ex: audio/wav, audio/mpeg)
 * @param fileSizeBytes  tamanho em bytes
 * @param requestId      ID de rastreabilidade do pedido externo (opcional)
 */
public record TranscriptionRequestMessage(
        String audioBase64,
        String filename,
        String contentType,
        long   fileSizeBytes,
        String requestId
) {}