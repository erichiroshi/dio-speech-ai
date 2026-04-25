package com.example.diospeechai.notification.infrastructure.email;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import com.example.diospeechai.notification.domain.model.NotificationChannel;
import com.example.diospeechai.notification.domain.model.NotificationRequest;
import com.example.diospeechai.notification.domain.port.out.NotificationPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Adapter de saída — implementa {@link NotificationPort} via Spring Mail (SMTP).
 *
 * <p>Envia um e-mail simples (texto plano) com o texto transcrito para o
 * destinatário configurado.
 *
 * <p>Configuração SMTP via {@code application.yml}:
 * <pre>
 *   spring.mail.host=smtp.gmail.com
 *   spring.mail.port=587
 *   spring.mail.username=...
 *   spring.mail.password=...
 *   spring.mail.properties.mail.smtp.starttls.enable=true
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(EmailProperties.class)
public class EmailNotificationAdapter implements NotificationPort {

    private final JavaMailSender mailSender;
    private final EmailProperties emailProperties;

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void send(NotificationRequest request) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(emailProperties.from());
            message.setTo(request.recipientContact());
            message.setSubject("Transcrição concluída — " + request.transcriptionId());
            message.setText(buildBody(request));

            mailSender.send(message);

            log.info("E-mail enviado | to={} | transcriptionId={}",
                    request.recipientContact(), request.transcriptionId());

        } catch (MailException ex) {
            log.error("Falha ao enviar e-mail | to={} | error={}",
                    request.recipientContact(), ex.getMessage());
            throw ex;
        }
    }

    private String buildBody(NotificationRequest request) {
        return """
                Sua transcrição de áudio foi concluída.
                
                ID: %s
                
                Texto transcrito:
                %s
                """.formatted(request.transcriptionId(), request.transcribedText());
    }

}