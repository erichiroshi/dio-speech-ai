package com.example.diospeechai.transcription.exception;

import java.net.URI;
import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(TranscriptionException.class)
	public ProblemDetail handleBadRequest(TranscriptionException ex) {
		log.warn("Transcription Exception | msg={}", ex.getMessage());
		
		return setProblemDetail(
				HttpStatus.BAD_REQUEST,
				"Transcription Exception",
				ex.getMessage(),
				URI.create("https://api.diospeechai/errors/transcription-exception")
				);
	}
	
	@ExceptionHandler(IllegalArgumentException.class)
	public ProblemDetail handleBadRequest(IllegalArgumentException ex) {
	      log.warn("Bad request | field={}", ex.getMessage());

		    return setProblemDetail(
		    		HttpStatus.BAD_REQUEST,
		            "Bad request",
		            ex.getMessage(),
		            URI.create("https://api.diospeechai/errors/bad-request")
		    );
	}

	@ExceptionHandler(Exception.class)
	public ProblemDetail handleGeneric(Exception ex) {

		log.error("Erro interno na aplicação | msg={}", ex.getMessage());
		
		return setProblemDetail(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"INTERNAL SERVER ERROR",
				ex.getMessage(),
				URI.create("https://api.diospeechai/errors/internal-server-error")
				);
	}
	
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ProblemDetail handleInvalidSort(HttpRequestMethodNotSupportedException ex) {
		
		log.warn("Method Not Supported | msg={}", ex.getMessage());
		
		return setProblemDetail(
				HttpStatus.METHOD_NOT_ALLOWED,
				"Method Not Supported",
				"The Method '" + ex.getMethod() + "' does not supported for this endpoint. Supported methods are: " + ex.getSupportedHttpMethods(),
				URI.create("https://api.library/errors/method-not-supported")
				);
	}


	// ─────────────────────────────────────────────
	// HELPERS
	// ─────────────────────────────────────────────

	private ProblemDetail setProblemDetail(HttpStatus status, String title, String detail, URI type) {

		ProblemDetail pd = ProblemDetail.forStatus(status);

		pd.setTitle(title);
		pd.setDetail(detail);
		if (type != null) {
			pd.setType(type);
		}
		pd.setProperty("timestamp", OffsetDateTime.now());
		return pd;
	}
}