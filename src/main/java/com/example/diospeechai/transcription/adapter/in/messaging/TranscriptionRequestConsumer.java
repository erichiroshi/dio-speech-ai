package com.example.diospeechai.transcription.adapter.in.messaging;

import java.util.Base64;

import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.example.diospeechai.transcription.application.command.TranscribeCommand;
import com.example.diospeechai.transcription.domain.port.in.TranscribeAudioPort;
import com.example.diospeechai.transcription.exception.TranscriptionException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Adapter de entrada via RabbitMQ — recebe pedidos de transcrição da fila.
 *
 * <p>Responsabilidades exclusivas deste adapter:
 * <ol>
 *   <li>Receber {@link TranscriptionRequestMessage} da fila</li>
 *   <li>Decodificar Base64 → bytes do áudio</li>
 *   <li>Converter para {@link TranscribeCommand}</li>
 *   <li>Chamar {@link TranscribeAudioPort} — exato mesmo caso de uso do HTTP</li>
 * </ol>
 *
 * <p>O resultado da transcrição é publicado automaticamente pelo
 * {@code TranscribeAudioUseCase} via {@code TranscriptionEventPort}
 * (exchange {@code transcription.events}) — sem código adicional aqui.
 *
 * <p>Em caso de falha após 3 tentativas (configuradas no {@code application.yml}),
 * a mensagem vai automaticamente para a DLQ {@code transcription.requests.dlq}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TranscriptionRequestConsumer {

    private final TranscribeAudioPort transcribePort;

    @RabbitListener(queues = "transcription.requests")
    public void onTranscriptionRequest(TranscriptionRequestMessage message) {

        String requestId = message.requestId() != null ? message.requestId() : "unknown";
        MDC.put("requestId", requestId);

        log.info("Pedido de transcrição recebido via fila | requestId={} | file={} | size={}bytes",
                requestId, message.filename(), message.fileSizeBytes());

        try {
            byte[] audioBytes = decodeBase64(message.audioBase64());
            
            TranscribeCommand command = new TranscribeCommand(
            		audioBytes,
                    message.filename(),
                    message.contentType(),
                    message.fileSizeBytes()
            );

            transcribePort.transcribe(command);

            log.info("Transcrição via fila concluída | requestId={}", requestId);

        } catch (TranscriptionException ex) {
            // Exceção de negócio — não faz sentido retentar (ex: Content-Type inválido)
            log.error("Erro de validação no pedido via fila | requestId={} | error={}",
                    requestId, ex.getMessage());
            // Lança para que o Spring AMQP envie para DLQ
            throw ex;
        } catch (Exception ex) {
            log.error("Falha ao processar pedido via fila | requestId={} | error={}",
                    requestId, ex.getMessage());
            throw ex;
        } finally {
            MDC.remove("requestId");
        }
    }

    private byte[] decodeBase64(String base64) {
        try {
            return Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException _) {
            throw new TranscriptionException("Áudio codificado em Base64 inválido");
        }
    }
}