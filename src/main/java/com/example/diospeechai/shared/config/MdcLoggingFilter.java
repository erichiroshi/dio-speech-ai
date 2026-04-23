package com.example.diospeechai.shared.config;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtro que popula o MDC (Mapped Diagnostic Context) no início de cada
 * requisição HTTP e garante a limpeza no bloco finally.
 *
 * <p>Campos injetados no MDC:
 * <ul>
 *   <li>{@code requestId} — UUID aleatório único por requisição</li>
 *   <li>{@code httpMethod} — GET, POST, etc.</li>
 *   <li>{@code requestUri} — path da requisição</li>
 * </ul>
 *
 * <p>Campos de negócio (fileName, fileSizeBytes, whisperModel) são
 * adicionados pelo {@link com.example.diospeechai.transcription.service.TranscriptionService}
 * durante o processamento de cada transcrição.
 *
 * <p>O {@code requestId} é propagado na resposta via header {@code X-Request-Id},
 * permitindo que clientes rastreiem uma requisição pelos logs.
 */
@Component
@Order(1)
public class MdcLoggingFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String requestId = UUID.randomUUID().toString();

        try {
            MDC.put("requestId",  requestId);
            MDC.put("httpMethod", request.getMethod());
            MDC.put("requestUri", request.getRequestURI());

            // Propaga o requestId na resposta para rastreabilidade pelo cliente
            response.setHeader(REQUEST_ID_HEADER, requestId);

            filterChain.doFilter(request, response);

        } finally {
            // CRÍTICO: MDC usa ThreadLocal — deve ser limpo para evitar
            // vazamento entre requisições em pools de threads
            MDC.clear();
        }
    }
}