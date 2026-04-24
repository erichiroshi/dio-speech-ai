package com.example.diospeechai.transcription;

import java.util.Base64;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

import com.example.diospeechai.transcription.adapter.in.messaging.TranscriptionRequestMessage;
import com.example.diospeechai.transcription.adapter.out.messaging.event.TranscriptionCompletedEvent;
import com.example.diospeechai.transcription.infrastructure.messaging.rabbit.RabbitMQConfig;
import com.redis.testcontainers.RedisContainer;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

/**
 * Testes de integração — v8.3.0.
 *
 * <p>Infraestrutura:
 * <ul>
 *   <li>Redis real via Testcontainers (redis:7-alpine)</li>
 *   <li>RabbitMQ real via Testcontainers (rabbitmq:3-management-alpine)</li>
 *   <li>Speaches simulado via OkHttp MockWebServer</li>
 * </ul>
 *
 * <p>Novos cenários (v8.3.0):
 * <ul>
 *   <li>HTTP cache miss → evento publicado no RabbitMQ</li>
 *   <li>HTTP cache hit → evento NÃO publicado</li>
 *   <li>Consumer RabbitMQ → processa pedido → cacheia → publica evento</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Transcription Integration Tests — v8.3.0 (RabbitMQ)")
class TranscriptionIntegrationTest {

    // ── Testcontainers ────────────────────────────────────────────────────────

    @Container
    static RedisContainer redis = new RedisContainer("redis:8.6.2-alpine");

	@SuppressWarnings("resource")
	@Container
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3-management-alpine")
            .withAdminUser("admin")
            .withAdminPassword("admin");

    static MockWebServer mockSpeaches = new MockWebServer();

    @BeforeAll
    static void startMockSpeaches() throws Exception {
        mockSpeaches.start();
    }

    @AfterAll
    static void stopMockSpeaches() throws Exception {
        mockSpeaches.shutdown();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getRedisPort);
        registry.add("whisper.base-url", () -> "http://localhost:" + mockSpeaches.getPort());
        registry.add("spring.rabbitmq.host", rabbit::getHost);
        registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbit::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbit::getAdminPassword);
        // CircuitBreaker rápido para testes
        registry.add("resilience4j.circuitbreaker.instances.whisper.slidingWindowSize",    () -> "4");
        registry.add("resilience4j.circuitbreaker.instances.whisper.minimumNumberOfCalls", () -> "4");
        registry.add("resilience4j.circuitbreaker.instances.whisper.failureRateThreshold", () -> "50");
        registry.add("resilience4j.circuitbreaker.instances.whisper.waitDurationInOpenState", () -> "2s");
        registry.add("resilience4j.retry.instances.whisper.maxAttempts", () -> "1");
    }

    // ── Injeções ──────────────────────────────────────────────────────────────

    @Autowired MockMvc mockMvc;
    @Autowired RedisTemplate<String, ?> redisTemplate;
    @Autowired CircuitBreakerRegistry circuitBreakerRegistry;
    @Autowired RabbitTemplate rabbitTemplate;

    // ── Setup ─────────────────────────────────────────────────────────────────

    @BeforeEach
    void setup() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        circuitBreakerRegistry.circuitBreaker("whisper").reset();
        mockSpeaches.setDispatcher(new okhttp3.mockwebserver.QueueDispatcher());
        // Limpar filas entre testes
        // Limpeza de fila de forma mais segura
        rabbitTemplate.execute(channel -> {
            channel.queuePurge(RabbitMQConfig.COMPLETED_QUEUE);
            return null;
        });

    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private MockMultipartFile audioFile() {
        return new MockMultipartFile("file", "audio.wav", "audio/wav",
                "fake-audio-content".getBytes());
    }

    private MockMultipartFile audioFile(String name, String content) {
        return new MockMultipartFile("file", name, "audio/wav", content.getBytes());
    }

    private void stubSpeachesSuccess(String text) {
        mockSpeaches.enqueue(new MockResponse()
                .setBody("{\"text\":\"" + text + "\"}")
                .addHeader("Content-Type", "application/json"));
    }

    private void stubSpeachesError() {
        mockSpeaches.enqueue(new MockResponse().setResponseCode(500));
    }

    // ── Testes HTTP existentes ─────────────────────────────────────────────────

    @Test
    @DisplayName("Deve retornar 400 quando Content-Type do arquivo é inválido")
    void shouldReturn400WhenInvalidContentType() throws Exception {
        var invalidFile = new MockMultipartFile("file", "doc.pdf", "application/pdf", "content".getBytes());
        mockMvc.perform(multipart("/api/transcriptions").file(invalidFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Transcription Exception"));
    }

    @Test
    @DisplayName("Deve retornar 400 quando arquivo está ausente")
    void shouldReturn400WhenFileMissing() throws Exception {
        mockMvc.perform(multipart("/api/transcriptions"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Cache miss: deve transcrever e retornar sem campo cached")
    void shouldTranscribeOnCacheMiss() throws Exception {
        stubSpeachesSuccess("audio transcrito");
        mockMvc.perform(multipart("/api/transcriptions").file(audioFile()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("audio transcrito"))
                .andExpect(jsonPath("$.cached").doesNotExist());
    }

    @Test
    @DisplayName("Cache hit: 2ª chamada retorna cached=true sem chamar Speaches")
    void shouldReturnCachedOnSecondCall() throws Exception {
        stubSpeachesSuccess("resposta do whisper");
        mockMvc.perform(multipart("/api/transcriptions").file(audioFile()))
                .andExpect(status().isOk());
        mockMvc.perform(multipart("/api/transcriptions").file(audioFile()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cached").value(true));
    }

    @Test
    @DisplayName("CircuitBreaker: deve retornar 503 após 4 falhas")
    void shouldReturn503WhenCircuitBreakerOpens() throws Exception {
        for (int i = 0; i < 4; i++) stubSpeachesError();
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(multipart("/api/transcriptions")
                    .file(audioFile("audio" + i + ".wav", "content-" + i)));
        }
        mockMvc.perform(multipart("/api/transcriptions")
                        .file(audioFile("new.wav", "new-content")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Service Unavailable"));
    }

    // ── Testes RabbitMQ v8.3.0 ─────────────────────────────────────────────────

    @Test
    @DisplayName("HTTP cache miss: deve publicar TranscriptionCompletedEvent na fila")
    void shouldPublishEventOnHttpCacheMiss() throws Exception {
        stubSpeachesSuccess("texto publicado no rabbit");

        mockMvc.perform(multipart("/api/transcriptions").file(audioFile()))
                .andExpect(status().isOk());

        // Aguarda publicação assíncrona (até 3s)
        TranscriptionCompletedEvent event = (TranscriptionCompletedEvent)
                rabbitTemplate.receiveAndConvert(RabbitMQConfig.COMPLETED_QUEUE, 3000);

        assertThat(event).isNotNull();
        assertThat(event.text()).isEqualTo("texto publicado no rabbit");
        assertThat(event.audioHash()).isNotBlank();
        assertThat(event.transcriptionId()).isNotNull();
    }

    @Test
    @DisplayName("HTTP cache hit: NÃO deve publicar evento (já transcrito antes)")
    void shouldNotPublishEventOnHttpCacheHit() throws Exception {
        stubSpeachesSuccess("texto cacheado");

        // 1ª chamada — publica evento
        mockMvc.perform(multipart("/api/transcriptions").file(audioFile()))
                .andExpect(status().isOk());
        rabbitTemplate.receiveAndConvert(RabbitMQConfig.COMPLETED_QUEUE, 2000); // consumir evento

        // 2ª chamada — cache hit, não deve publicar novo evento
        mockMvc.perform(multipart("/api/transcriptions").file(audioFile()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cached").value(true));

        Object noEvent = rabbitTemplate.receiveAndConvert(RabbitMQConfig.COMPLETED_QUEUE, 1000);
        assertThat(noEvent).isNull(); // nenhum evento publicado em hit
    }

    @Test
    @DisplayName("Consumer: deve processar pedido da fila, cachear e publicar evento")
    void shouldProcessRequestFromQueueAndPublishEvent() throws Exception {
        stubSpeachesSuccess("transcrito via fila");

        byte[] audioBytes = "fake-audio-content-queue".getBytes();
        String base64 = Base64.getEncoder().encodeToString(audioBytes);

        TranscriptionRequestMessage message = new TranscriptionRequestMessage(
                base64, "queue-audio.wav", "audio/wav",
                audioBytes.length, "req-test-001");

        // Publicar na fila de pedidos
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.REQUESTS_EXCHANGE,
                RabbitMQConfig.REQUESTS_ROUTING_KEY,
                message);

        await()
        .atMost(10, SECONDS) // Timeout máximo
        .pollInterval(500, TimeUnit.MILLISECONDS) // Frequência de checagem
        .untilAsserted(() -> {
            Object received = rabbitTemplate.receiveAndConvert(RabbitMQConfig.COMPLETED_QUEUE);
            assertThat(received).isNotNull();
            
            TranscriptionCompletedEvent event = (TranscriptionCompletedEvent) received;

        assertThat(event).isNotNull();
        assertThat(event.text()).isEqualTo("transcrito via fila");
        assertThat(event.audioHash()).isNotBlank();
        });
    }
}