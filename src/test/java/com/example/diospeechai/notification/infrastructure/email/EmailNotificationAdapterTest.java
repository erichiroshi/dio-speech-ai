package com.example.diospeechai.notification.infrastructure.email;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.MailSendException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import com.example.diospeechai.notification.domain.model.NotificationChannel;
import com.example.diospeechai.notification.domain.model.NotificationRequest;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.mail.internet.MimeMessage;

/**
 * Testes de integração do {@link EmailNotificationAdapter}.
 *
 * <p>Usa GreenMail como servidor SMTP embarcado — sem conta real, sem env vars.
 * O GreenMail sobe na porta aleatória configurada via {@code @DynamicPropertySource}.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration",
    "notification.email.enabled=true",
    "notification.email.from=test@diospeechai.com",
    "notification.channel=EMAIL",
    "notification.default-recipient=recipient@example.com"
})
@DisplayName("EmailNotificationAdapter — testes de integração")
class EmailNotificationAdapterTest {

    static GreenMail greenMail;

    @DynamicPropertySource
    static void mailProperties(DynamicPropertyRegistry registry) {
    	greenMail = new GreenMail(new ServerSetup(3025, "127.0.0.1", "smtp"));
    	greenMail.start();
        registry.add("spring.mail.host",     () -> "127.0.0.1");
        registry.add("spring.mail.port",     () -> greenMail.getSmtp().getPort());
        registry.add("spring.mail.properties.mail.smtp.auth", () -> "false");
        registry.add("spring.mail.properties.mail.smtp.starttls.enable", () -> "false");
    }
    
    @AfterEach
    void tearDown() {
        greenMail.reset();
    }

    @Autowired
    EmailNotificationAdapter adapter;

    // ── Envio bem-sucedido ────────────────────────────────────────────────────

    @Test
    @DisplayName("Deve enviar e-mail com subject e body corretos")
    void shouldSendEmailWithCorrectContent() throws Exception {
        UUID id = UUID.randomUUID();
        var request = new NotificationRequest(
                id, "dest@example.com",
                NotificationChannel.EMAIL, "texto transcrito pelo whisper");
 
        adapter.send(request);
 
        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertThat(messages).hasSize(1);
 
        MimeMessage msg = messages[0];
        assertThat(msg.getAllRecipients()[0].toString()).hasToString("dest@example.com");
        assertThat(msg.getSubject()).contains("Transcrição concluída");
        assertThat(msg.getSubject()).contains(id.toString());
        assertThat(msg.getContent().toString()).contains("texto transcrito pelo whisper");
        assertThat(msg.getContent().toString()).contains(id.toString());
    }

    @Test
    @DisplayName("Deve lançar MailException quando SMTP falha")
    void shouldThrowWhenSmtpFails() {
        greenMail.stop(); // força falha SMTP
 
        var request = new NotificationRequest(
                UUID.randomUUID(), "dest@example.com",
                NotificationChannel.EMAIL, "texto");
 
        assertThatThrownBy(() -> adapter.send(request))
                .isInstanceOf(MailSendException.class);
 
        greenMail.start(); // restaura para outros testes
    }
}