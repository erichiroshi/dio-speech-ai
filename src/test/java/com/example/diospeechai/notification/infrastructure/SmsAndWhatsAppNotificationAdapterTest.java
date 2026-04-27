package com.example.diospeechai.notification.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.example.diospeechai.notification.domain.model.NotificationChannel;
import com.example.diospeechai.notification.domain.model.NotificationRequest;
import com.example.diospeechai.notification.infrastructure.sms.SmsNotificationAdapter;
import com.example.diospeechai.notification.infrastructure.sms.SmsProperties;
import com.example.diospeechai.notification.infrastructure.whatsapp.WhatsAppNotificationAdapter;
import com.example.diospeechai.notification.infrastructure.whatsapp.WhatsAppProperties;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Testes unitários de {@link SmsNotificationAdapter} e {@link WhatsAppNotificationAdapter}.
 *
 * <p>Usa OkHttp {@link MockWebServer} — mesma lib já usada nos testes do Whisper.
 * Zero Spring, zero conta em provedor externo.
 */
@DisplayName("SMS e WhatsApp Notification Adapters — testes unitários")
class SmsAndWhatsAppNotificationAdapterTest {

    MockWebServer server;

    @BeforeEach
    void startServer() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void stopServer() throws Exception {
        server.shutdown();
    }

    private String baseUrl() {
        return "http://localhost:" + server.getPort();
    }

    // ── SMS ───────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("SmsNotificationAdapter")
    class SmsTests {

        SmsNotificationAdapter adapter;

        @BeforeEach
        void setUp() {
            var props = new SmsProperties(baseUrl(), "/v1/sms", "api-key-test", "DioSpeech");
            adapter = new SmsNotificationAdapter(WebClient.builder(), props);
        }

        @Test
        @DisplayName("Deve enviar SMS com payload correto")
        void shouldSendSmsWithCorrectPayload() throws Exception {
            server.enqueue(new MockResponse().setResponseCode(200));

            var request = new NotificationRequest(
                    UUID.randomUUID(), "+5567999999999",
                    NotificationChannel.SMS, "texto transcrito");

            adapter.send(request);

            RecordedRequest recorded = server.takeRequest();
            assertThat(recorded.getPath()).isEqualTo("/v1/sms");
            assertThat(recorded.getHeader("Authorization")).isEqualTo("Bearer api-key-test");
            assertThat(recorded.getBody().readUtf8())
                    .contains("+5567999999999")
                    .contains("texto transcrito");
        }

        @Test
        @DisplayName("Deve lançar exceção quando provedor SMS retorna erro")
        void shouldThrowWhenSmsProviderReturnsError() {
            server.enqueue(new MockResponse().setResponseCode(500).setBody("Internal Error"));

            var request = new NotificationRequest(
                    UUID.randomUUID(), "+5567999999999",
                    NotificationChannel.SMS, "texto");

            assertThatThrownBy(() -> adapter.send(request))
                    .isInstanceOf(WebClientResponseException.class);
        }
    }

    // ── WhatsApp ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("WhatsAppNotificationAdapter")
    class WhatsAppTests {

        WhatsAppNotificationAdapter adapter;

        @BeforeEach
        void setUp() {
            var props = new WhatsAppProperties(baseUrl(), "/v19.0/123/messages", "wa-token-test");
            adapter = new WhatsAppNotificationAdapter(WebClient.builder(), props);
        }

        @Test
        @DisplayName("Deve enviar mensagem WhatsApp com payload correto")
        void shouldSendWhatsAppWithCorrectPayload() throws Exception {
            server.enqueue(new MockResponse().setResponseCode(200));

            var request = new NotificationRequest(
                    UUID.randomUUID(), "5567999999999",
                    NotificationChannel.WHATSAPP, "texto via whatsapp");

            adapter.send(request);

            RecordedRequest recorded = server.takeRequest();
            assertThat(recorded.getPath()).isEqualTo("/v19.0/123/messages");
            assertThat(recorded.getHeader("Authorization")).isEqualTo("Bearer wa-token-test");
            String body = recorded.getBody().readUtf8();
            assertThat(body)
                    .contains("whatsapp")
                    .contains("5567999999999")
                    .contains("texto via whatsapp");
        }

        @Test
        @DisplayName("channel() deve retornar WHATSAPP")
        void shouldReturnWhatsappChannel() {
            assertThat(adapter.channel()).isEqualTo(NotificationChannel.WHATSAPP);
        }

        @Test
        @DisplayName("Deve lançar exceção quando API WhatsApp retorna erro 4xx")
        void shouldThrowWhenWhatsAppApiReturnsError() {
            server.enqueue(new MockResponse().setResponseCode(401).setBody("Unauthorized"));

            var request = new NotificationRequest(
                    UUID.randomUUID(), "5567999999999",
                    NotificationChannel.WHATSAPP, "texto");

            assertThatThrownBy(() -> adapter.send(request))
                    .isInstanceOf(WebClientResponseException.class);
        }
    }
}