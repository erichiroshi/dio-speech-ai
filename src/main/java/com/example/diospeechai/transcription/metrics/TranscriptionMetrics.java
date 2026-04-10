package com.example.diospeechai.transcription.metrics;

import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * Centraliza todas as métricas de negócio do fluxo de transcrição.
 *
 * <p>Métricas expostas:
 * <ul>
 *   <li>{@code transcription.requests.total} — contador total de requisições (tag: status=success|error)</li>
 *   <li>{@code transcription.whisper.duration} — timer das chamadas ao Whisper (p50/p95/p99)</li>
 *   <li>{@code transcription.file.size.bytes} — distribuição dos tamanhos de arquivo processados</li>
 * </ul>
 *
 * <p>Todas as métricas carregam a tag {@code application} herdada da configuração global
 * em {@code application.yml} (management.metrics.tags.application).
 */
@Component
public class TranscriptionMetrics {

    // ── Contadores ────────────────────────────────────────────────────────────
    private final Counter successCounter;
    private final Counter errorCounter;

    // ── Timer ─────────────────────────────────────────────────────────────────
    private final Timer whisperTimer;

    // ── Distribuição ──────────────────────────────────────────────────────────
    private final DistributionSummary fileSizeSummary;

    public TranscriptionMetrics(MeterRegistry registry) {

        this.successCounter = Counter.builder("transcription.requests.total")
                .tag("status", "success")
                .description("Total de transcrições realizadas com sucesso")
                .register(registry);

        this.errorCounter = Counter.builder("transcription.requests.total")
                .tag("status", "error")
                .description("Total de transcrições que falharam")
                .register(registry);

        this.whisperTimer = Timer.builder("transcription.whisper.duration")
                .description("Tempo de resposta das chamadas ao Whisper (Speaches)")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(registry);

        this.fileSizeSummary = DistributionSummary
                .builder("transcription.file.size.bytes")
                .description("Distribuição dos tamanhos dos arquivos de áudio enviados")
                .baseUnit("bytes")
                .register(registry);
    }

    // ── API pública ───────────────────────────────────────────────────────────

    public void recordSuccess() {
        successCounter.increment();
    }

    public void recordError() {
        errorCounter.increment();
    }

    /**
     * Executa {@code task} dentro do timer do Whisper.
     * O tempo é registrado automaticamente, mesmo em caso de exceção.
     */
    public <T> T recordWhisperCall(Supplier<T> task) {
        return whisperTimer.record(task::get);
    }

    public void recordFileSize(long sizeBytes) {
        fileSizeSummary.record(sizeBytes);
    }
}	