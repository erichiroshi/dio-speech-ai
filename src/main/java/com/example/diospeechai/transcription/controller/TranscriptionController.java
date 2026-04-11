package com.example.diospeechai.transcription.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.diospeechai.transcription.dto.TranscriptionResponse;
import com.example.diospeechai.transcription.service.TranscriptionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/transcriptions")
@RequiredArgsConstructor
public class TranscriptionController {

	private final TranscriptionService service;

	@PostMapping
	public ResponseEntity<TranscriptionResponse> transcribe(@RequestPart("file") MultipartFile file) {
		return ResponseEntity.ok(service.transcribe(file));
	}
}