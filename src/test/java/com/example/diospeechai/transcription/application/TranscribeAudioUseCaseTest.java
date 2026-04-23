package com.example.diospeechai.transcription.application;

import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.diospeechai.transcription.application.command.TranscribeCommand;
import com.example.diospeechai.transcription.application.result.TranscriptionResult;
import com.example.diospeechai.transcription.domain.model.Transcription;
import com.example.diospeechai.transcription.domain.port.out.SpeechToTextPort;
import com.example.diospeechai.transcription.domain.port.out.TranscriptionCachePort;
import com.example.diospeechai.transcription.domain.port.out.TranscriptionEventPort;
import com.example.diospeechai.transcription.exception.TranscriptionException;
import com.example.diospeechai.transcription.metrics.TranscriptionMetrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Teste unitário puro do {@link TranscribeAudioUseCase}.
 *
 * <p>Zero Spring, zero Docker, zero Redis, zero Whisper.
 * Testa a lógica de negócio isolada via mocks dos ports.
 * Executa em ~20ms.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TranscribeAudioUseCase — testes unitários")
class TranscribeAudioUseCaseTest {

    @Mock SpeechToTextPort      speechPort;
    @Mock TranscriptionCachePort cachePort;
    @Mock TranscriptionEventPort eventPort;
    @Mock TranscriptionMetrics   metrics;

    @InjectMocks TranscribeAudioUseCase useCase;

    private static final byte[] AUDIO_BYTES = "fake-audio-content".getBytes();
    private static final String FILENAME    = "audio.wav";
    private static final String CONTENT_TYPE    = "audio/wav";
    private static final long   SIZE        = AUDIO_BYTES.length;

    private TranscribeCommand command;

	@BeforeEach
    void setUp() {
        command = new TranscribeCommand(AUDIO_BYTES, FILENAME, CONTENT_TYPE, SIZE);

        // TranscriptionMetrics.recordWhisperCall() precisa executar o supplier
    }

    // ── Cache HIT ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Cache hit: deve retornar resultado cacheado sem chamar speechPort")
    void shouldReturnCachedResultOnCacheHit() {
        // Arrange
        var cached = new TranscriptionResult("texto cacheado", SIZE, false);
        when(cachePort.get(anyString())).thenReturn(Optional.of(cached));

        // Act
        TranscriptionResult result = useCase.transcribe(command);

        // Assert
        assertThat(result.text()).isEqualTo("texto cacheado");
        assertThat(result.cached()).isTrue();

        verify(speechPort, never()).transcribe(any());
        verify(cachePort, never()).put(anyString(), any());
        verify(eventPort, never()).publish(any());
        verify(metrics).recordCacheHit();
        verify(metrics, never()).recordCacheMiss();
    }

    // ── Cache MISS ────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
	@Test
    @DisplayName("Cache miss: deve chamar speechPort, armazenar cache e publicar evento")
    void shouldTranscribeAndCacheOnCacheMiss() {
        // Arrange
    	when(metrics.recordWhisperCall(any(Supplier.class)))
        .thenAnswer(inv -> inv.<Supplier<?>>getArgument(0).get());
    	when(cachePort.get(anyString())).thenReturn(Optional.empty());
        when(speechPort.transcribe(any())).thenReturn("texto transcrito");

        // Act
        TranscriptionResult result = useCase.transcribe(command);

        // Assert
        assertThat(result.text()).isEqualTo("texto transcrito");
        assertThat(result.cached()).isFalse();

        verify(speechPort).transcribe(AUDIO_BYTES);
        verify(cachePort).put(anyString(), eq(result));
        verify(eventPort).publish(any(Transcription.class));
        verify(metrics).recordCacheMiss();
        verify(metrics).recordSuccess();
        verify(metrics, never()).recordCacheHit();
    }

    // ── Mesmo conteúdo → mesmo hash ───────────────────────────────────────────

    @SuppressWarnings("unchecked")
	@Test
    @DisplayName("Dois comandos com mesmo conteúdo devem gerar o mesmo hash de cache")
    void shouldProduceSameHashForSameContent() {
        // Arrange
        var command2 = new TranscribeCommand(AUDIO_BYTES, "outro-nome.wav", CONTENT_TYPE, SIZE);
        
        when(metrics.recordWhisperCall(any(Supplier.class)))
        .thenAnswer(inv -> inv.<Supplier<?>>getArgument(0).get());
        when(cachePort.get(anyString()))
                .thenReturn(Optional.empty())           // 1ª chamada — miss
                .thenReturn(Optional.of(new TranscriptionResult("texto", SIZE, false))); // 2ª — hit
        when(speechPort.transcribe(any())).thenReturn("texto");

        // Act
        useCase.transcribe(command);   // miss — armazena
        useCase.transcribe(command2);  // hit  — mesmo hash, nome diferente

        // Assert: speechPort só foi chamado uma vez
        verify(speechPort).transcribe(any());
        verify(metrics).recordCacheHit();
    }

    // ── Falha no speechPort ───────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
	@Test
    @DisplayName("Falha no speechPort deve registrar erro e relançar a exceção")
    void shouldRecordErrorAndRethrowOnSpeechPortFailure() {
        // Arrange
    	when(metrics.recordWhisperCall(any(Supplier.class)))
        .thenAnswer(inv -> inv.<Supplier<?>>getArgument(0).get());
    	when(cachePort.get(anyString())).thenReturn(Optional.empty());
        when(speechPort.transcribe(any()))
                .thenThrow(new TranscriptionException("Falha na comunicação com Whisper"));

        // Act + Assert
        assertThatThrownBy(() -> useCase.transcribe(command))
                .isInstanceOf(TranscriptionException.class)
                .hasMessage("Falha na comunicação com Whisper");

        verify(metrics).recordError();
        verify(metrics, never()).recordSuccess();
        verify(cachePort, never()).put(anyString(), any());
        verify(eventPort, never()).publish(any());
    }

    // ── Métricas ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Deve registrar o tamanho do arquivo independente de hit ou miss")
    void shouldAlwaysRecordFileSize() {
        // Arrange — cache hit
        when(cachePort.get(anyString()))
                .thenReturn(Optional.of(new TranscriptionResult("texto", SIZE, false)));

        // Act
        useCase.transcribe(command);

        // Assert
        verify(metrics).recordFileSize(SIZE);
    }
}