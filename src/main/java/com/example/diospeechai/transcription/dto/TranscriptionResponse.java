package com.example.diospeechai.transcription.dto;

public record TranscriptionResponse(
	    String text,
	    Long processingTimeMs,
	    Long fileSizeBytes
	) {}