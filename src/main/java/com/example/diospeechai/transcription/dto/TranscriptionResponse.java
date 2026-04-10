package com.example.diospeechai.transcription.dto;

public record TranscriptionResponse(
	    String text,
	    Long fileSizeBytes
	) {}