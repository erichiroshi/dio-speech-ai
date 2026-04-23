package com.example.diospeechai.transcription.domain.port.out;

/**
 * PORT DE SAÍDA — contrato do serviço de transcrição de áudio (IA).
 *
 * <p>O domínio define esta interface. A infraestrutura a implementa.
 * Hoje: {@code WhisperAdapter}. Amanhã: {@code OpenAiAdapter}, {@code AssemblyAiAdapter}
 * ou qualquer outro — sem tocar uma linha do domínio.
 *
 * <p>Contrato: recebe o conteúdo binário do áudio e retorna o texto transcrito.
 * Detalhes de HTTP, WebClient, retry e circuit breaker ficam exclusivamente
 * no adapter que implementa esta interface.
 */
public interface SpeechToTextPort {

    /**
     * Transcreve o conteúdo de áudio para texto.
     *
     * @param audioBytes conteúdo binário do arquivo de áudio
     * @return texto transcrito
     * @throws com.example.diospeechai.transcription.exception.TranscriptionException
     *         se a transcrição falhar por erro de I/O
     * @throws com.example.diospeechai.transcription.exception.ServiceUnavailableException
     *         se o serviço estiver indisponível (CircuitBreaker aberto)
     */
    String transcribe(byte[] audioBytes);
}