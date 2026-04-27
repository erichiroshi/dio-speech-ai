package com.example.diospeechai.notification.infrastructure.sms;

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
 * Adapter de saída — implementa {@link NotificationPort} para SMS via API REST.
 *
 * <p>Genérico: funciona com qualquer provedor que aceite JSON REST
 * (Twilio, Vonage, AWS SNS, etc.). O endpoint e credenciais são
 * configurados via {@link SmsProperties}.
 *
 * <p>Para trocar de provedor: alterar as variáveis de ambiente.
 * O adapter não muda.
 */
@Slf4j
@Component
@EnableConfigurationProperties(SmsProperties.class)
public class SmsNotificationAdapter implements NotificationPort {

    private final WebClient webClient;
    private final SmsProperties properties;

    public SmsNotificationAdapter(WebClient.Builder webClientBuilder,
                                   SmsProperties properties) {
        this.properties = properties;
        this.webClient  = webClientBuilder
                .baseUrl(properties.baseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.apiKey())
                .build();
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SMS;
    }

    @Override
    public void send(NotificationRequest request) {
        String body = "Transcrição concluída: " + request.transcribedText();

        try {
            webClient.post()
                    .uri(properties.sendPath())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of(
                            "to",   request.recipientContact(),
                            "from", properties.senderId(),
                            "text", body
                    ))
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("SMS enviado | to={} | transcriptionId={}",
                    request.recipientContact(), request.transcriptionId());

        } catch (WebClientResponseException ex) {
            log.error("Falha ao enviar SMS | to={} | status={} | error={}",
                    request.recipientContact(), ex.getStatusCode(), ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Falha ao enviar SMS | to={} | error={}",
                    request.recipientContact(), ex.getMessage());
            throw ex;
        }
    }
}