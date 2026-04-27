package com.example.diospeechai.notification.infrastructure.whatsapp;

import java.util.Map;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.example.diospeechai.notification.domain.model.NotificationChannel;
import com.example.diospeechai.notification.domain.model.NotificationRequest;
import com.example.diospeechai.notification.domain.port.out.NotificationPort;

import lombok.extern.slf4j.Slf4j;

/**
 * Adapter de saída — implementa {@link NotificationPort} para WhatsApp via API REST.
 *
 * <p>Compatível com qualquer provedor de WhatsApp Business API
 * (Meta Cloud API, Twilio, Vonage, etc.).
 *
 * <p>Para trocar de provedor: alterar as variáveis de ambiente.
 * O adapter não muda — apenas o payload pode precisar de ajuste.
 */
@Slf4j
@Component
@EnableConfigurationProperties(WhatsAppProperties.class)
public class WhatsAppNotificationAdapter implements NotificationPort {

    private final WebClient webClient;
    private final WhatsAppProperties properties;

    public WhatsAppNotificationAdapter(WebClient.Builder webClientBuilder,
                                        WhatsAppProperties properties) {
        this.properties = properties;
        this.webClient  = webClientBuilder
                .baseUrl(properties.baseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.accessToken())
                .build();
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.WHATSAPP;
    }

    @Override
    public void send(NotificationRequest request) {
        // Payload compatível com Meta Cloud API (WhatsApp Business)
        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "to", request.recipientContact(),
                "type", "text",
                "text", Map.of("body",
                        "Transcrição concluída:\n" + request.transcribedText())
        );

        try {
            webClient.post()
                    .uri(properties.sendPath())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("WhatsApp enviado | to={} | transcriptionId={}",
                    request.recipientContact(), request.transcriptionId());

        } catch (WebClientResponseException ex) {
            log.error("Falha ao enviar WhatsApp | to={} | status={} | error={}",
                    request.recipientContact(), ex.getStatusCode(), ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Falha ao enviar WhatsApp | to={} | error={}",
                    request.recipientContact(), ex.getMessage());
            throw ex;
        }
    }
}