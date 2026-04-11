package com.example.diospeechai.transcription.exception;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import lombok.extern.slf4j.Slf4j;

/**
 * Tratamento centralizado de exceções.
 *
 * <p>v2.2.0: logs usam campos estruturados (sem concatenação de string)
 * para que o logstash-logback-encoder os serialize como campos separados no JSON.
 * O {@code requestId} do MDC é adicionado ao ProblemDetail como propriedade,
 * permitindo que o cliente correlacione o erro com os logs do servidor.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	private static final String ERROR_BASE_URI = "https://api.diospeechai/errors/";

	@ExceptionHandler(TranscriptionException.class)
	public ProblemDetail handleTranscription(TranscriptionException ex) {
		
		log.warn("Transcription Error | message={}", ex.getMessage());
		
		return buildProblemDetail(
				HttpStatus.BAD_REQUEST,
				"Transcription Exception",
				ex.getMessage(),
				"transcription-exception"
				);
	}
	
	@ExceptionHandler(IllegalArgumentException.class)
	public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {

		log.warn("Bad request | message={}", ex.getMessage());

		return buildProblemDetail(
				HttpStatus.BAD_REQUEST, 
				"Bad request", 
				ex.getMessage(), 
				"bad-request"
				);
	}
	
	@ExceptionHandler(MissingServletRequestPartException.class)
	public ProblemDetail handleMissingServletRequestPart(MissingServletRequestPartException ex) {
		
		log.warn("Bad request | message={}", ex.getMessage());
		
		return buildProblemDetail(
				HttpStatus.BAD_REQUEST,
				"Bad request",
				"Required part 'file' is not present.",
				"bad-request"
				);
	}
	
	@ExceptionHandler(MultipartException.class)
	public ProblemDetail handleMultipart(MultipartException ex) {
		
		log.warn("Bad request | message={}", ex.getMessage());
		
		return buildProblemDetail(
				HttpStatus.BAD_REQUEST,
				"Bad request",
				"Current request is not a multipart request.",
				"bad-request"
				);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ProblemDetail handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
		
        log.warn("Method not supported | method={} | supported={}",
                ex.getMethod(), ex.getSupportedHttpMethods());	
        
		return buildProblemDetail(
				HttpStatus.METHOD_NOT_ALLOWED,
				"Method Not Allowed",
                "O método '%s' não é suportado. Métodos aceitos: %s"
                	.formatted(ex.getMethod(), ex.getSupportedHttpMethods()),
				"method-not-allowed"
                );
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ProblemDetail handleNotFound(NoResourceFoundException ex) {

        log.warn("Resource not found | uri={}", ex.getMessage());

		return buildProblemDetail(
				HttpStatus.NOT_FOUND,
				"Not Found",
				"O recurso solicitado não existe.",
				"resource-not-found"
				);
	}

	@ExceptionHandler(Exception.class)
	public ProblemDetail handleGeneric(Exception ex) {
	
	    log.error("Unexpected error | type={} | message={}",
	            ex.getClass().getSimpleName(), ex.getMessage(), ex);
	    
		return buildProblemDetail(
			    HttpStatus.INTERNAL_SERVER_ERROR,
			    "Internal Server Error",
	            "Erro inesperado. Consulte o suporte com o requestId.",
			    "internal-server-error"
			);
	}
	
	// ── helper ────────────────────────────────────────────────────────────────

	private ProblemDetail buildProblemDetail(HttpStatus status, String title, String detail, String type) {

		ProblemDetail pd = ProblemDetail.forStatus(status);

		pd.setTitle(title);
		pd.setDetail(detail);
		if (type != null) {
			pd.setType(URI.create(ERROR_BASE_URI + type));
		}
		pd.setProperty("timestamp", OffsetDateTime.now(ZoneOffset.UTC));
		
        // Inclui o requestId para correlação com os logs do servidor
        String requestId = MDC.get("requestId");
        if (requestId != null) {
            pd.setProperty("requestId", requestId);
        }
        
		return pd;
	}
}