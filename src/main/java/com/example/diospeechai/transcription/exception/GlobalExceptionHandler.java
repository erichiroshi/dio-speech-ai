package com.example.diospeechai.transcription.exception;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
	
	private static final String ERROR_BASE_URI = "https://api.diospeechai/errors/";

	@ExceptionHandler(TranscriptionException.class)
	public ProblemDetail handleTranscription(TranscriptionException ex) {
		
		log.warn("Transcription Error | message={}", ex.getMessage());
		
		return setProblemDetail(
				HttpStatus.BAD_REQUEST,
				"Transcription Exception",
				ex.getMessage(),
				"transcription-exception"
				);
	}
	
	@ExceptionHandler(IllegalArgumentException.class)
	public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
		
	      log.warn("Bad request | message={}", ex.getMessage());

		    return setProblemDetail(
		    		HttpStatus.BAD_REQUEST,
		            "Bad request",
		            "Invalid request parameters",
		            "bad-request"
		    );
	}

	@ExceptionHandler(Exception.class)
	public ProblemDetail handleGeneric(Exception ex) {

		log.error("Internal error", ex);
		
		return setProblemDetail(
			    HttpStatus.INTERNAL_SERVER_ERROR,
			    "Internal Server Error",
			    "An unexpected error occurred. Please contact support.",
			    "internal-server-error"
			);
	}
	
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ProblemDetail handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
		
		log.warn("Method Not Supported | msg={}", ex.getMessage());
		
		return setProblemDetail(
				HttpStatus.METHOD_NOT_ALLOWED,
				"Method Not Allowed",
                "The method '" + ex.getMethod() + "' is not supported. Supported methods: " + ex.getSupportedHttpMethods(),
				"method-not-allowed"
				);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ProblemDetail handleNotFound(NoResourceFoundException ex) {

		log.warn("No Resource Found | msg={}", ex.getMessage());

		return setProblemDetail(
				HttpStatus.NOT_FOUND,
				"Not Found",
				"O recurso solicitado não existe.",
				"resource-not-found"
				);
	}

	// ─────────────────────────────────────────────
	// HELPERS
	// ─────────────────────────────────────────────

	private ProblemDetail setProblemDetail(HttpStatus status, String title, String detail, String type) {

		ProblemDetail pd = ProblemDetail.forStatus(status);

		pd.setTitle(title);
		pd.setDetail(detail);
		if (type != null) {
			pd.setType(URI.create(ERROR_BASE_URI + type));
		}
		pd.setProperty("timestamp", OffsetDateTime.now(ZoneOffset.UTC));
		return pd;
	}
}