package com.example.diospeechai.analysis.application.command;

/**
 * Input do caso de uso de sumarização.
 *
 * <p>Carrega os dados necessários sem acoplamento com {@code HttpServletRequest}
 * ou qualquer objeto HTTP.
 *
 * @param audioHash        SHA-256 do áudio (chave de correlação)
 * @param transcribedText  texto transcrito a ser resumido
 */
public record SummarizeCommand(
        String audioHash,
        String transcribedText
) {}