package com.example.diospeechai.transcription;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.redis.testcontainers.RedisContainer;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração do fluxo completo de transcrição — v7.6.0.
 *
 * <p>Infraestrutura:
 * <ul>
 *   <li>Redis real via Testcontainers (redis:7-alpine)</li>
 *   <li>Speaches simulado via OkHttp MockWebServer</li>
 * </ul>
 *
 * <p>Cenários cobertos:
 * <ul>
 *   <li>Content-Type inválido → 400</li>
 *   <li>Arquivo ausente → 400</li>
 *   <li>Cache miss → Speaches chamado → 200 (cached ausente)</li>
 *   <li>Cache hit → Speaches NÃO chamado → 200 (cached=true)</li>
 *   <li>CircuitBreaker abre → 503</li>
 * </ul>
 *
 * <p>v7.6.0: sem JWT/Security — endpoint público.
 * Pacotes atualizados para arquitetura hexagonal (adapter/in/http/).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Transcription Integration Tests — v7.6.0 (Hexagonal)")
class TranscriptionIntegrationTest {

    // ── Testcontainers ────────────────────────────────────────────────────────

    @Container
    static RedisContainer redis = new RedisContainer("redis:7-alpine");

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
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("whisper.base-url", () -> "http://localhost:" + mockSpeaches.getPort());
        // CircuitBreaker com janela menor para abrir rápido nos testes
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

    // ── Setup ─────────────────────────────────────────────────────────────────

    @BeforeEach
    void setup() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        circuitBreakerRegistry.circuitBreaker("whisper").reset();
        mockSpeaches.setDispatcher(new okhttp3.mockwebserver.QueueDispatcher());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private MockMultipartFile audioFile() {
        return new MockMultipartFile("file", "audio.wav", "audio/wav",
                "fake-audio-content".getBytes());
    }

    private void stubSpeachesSuccess(String text) {
        mockSpeaches.enqueue(new MockResponse()
                .setBody("{\"text\":\"" + text + "\"}")
                .addHeader("Content-Type", "application/json"));
    }

    private void stubSpeachesError() {
        mockSpeaches.enqueue(new MockResponse().setResponseCode(500).setBody("Internal Server Error"));
    }

    // ── Testes de validação ───────────────────────────────────────────────────

    @Test
    @DisplayName("Deve retornar 400 quando Content-Type do arquivo é inválido")
    void shouldReturn400WhenInvalidContentType() throws Exception {
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", "content".getBytes());

        mockMvc.perform(multipart("/api/transcriptions").file(invalidFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Transcription Exception"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Deve retornar 400 quando arquivo está ausente")
    void shouldReturn400WhenFileMissing() throws Exception {
        mockMvc.perform(multipart("/api/transcriptions"))
                .andExpect(status().isBadRequest());
    }

    // ── Testes de cache ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Cache miss: deve chamar Speaches e retornar transcrição sem campo cached")
    void shouldTranscribeOnCacheMiss() throws Exception {
        stubSpeachesSuccess("audio transcrito com sucesso");

        mockMvc.perform(multipart("/api/transcriptions").file(audioFile()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("audio transcrito com sucesso"))
                .andExpect(jsonPath("$.fileSizeBytes").exists())
                .andExpect(jsonPath("$.cached").doesNotExist());
    }

    @Test
    @DisplayName("Cache hit: 2ª chamada com mesmo arquivo retorna cached=true sem chamar Speaches")
    void shouldReturnCachedOnSecondCall() throws Exception {
        stubSpeachesSuccess("resposta do whisper");

        // 1ª chamada — miss, Speaches chamado
        mockMvc.perform(multipart("/api/transcriptions").file(audioFile()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cached").doesNotExist());

        // 2ª chamada — hit, Speaches NÃO chamado
        mockMvc.perform(multipart("/api/transcriptions").file(audioFile()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cached").value(true));
    }

    // ── Testes de resiliência ─────────────────────────────────────────────────

    @Test
    @DisplayName("CircuitBreaker: deve retornar 503 após 4 falhas consecutivas")
    void shouldReturn503WhenCircuitBreakerOpens() throws Exception {
        for (int i = 0; i < 4; i++) stubSpeachesError();

        for (int i = 0; i < 4; i++) {
            MockMultipartFile f = new MockMultipartFile(
                    "file", "audio" + i + ".wav", "audio/wav", ("content-" + i).getBytes());
            mockMvc.perform(multipart("/api/transcriptions").file(f));
        }

        mockMvc.perform(multipart("/api/transcriptions")
                        .file(new MockMultipartFile("file", "new.wav", "audio/wav", "new".getBytes())))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Service Unavailable"));
    }
}