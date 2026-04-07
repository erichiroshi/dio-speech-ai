package com.example.diospeechai.transcription.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.diospeechai.transcription.dto.TranscriptionResponse;
import com.example.diospeechai.transcription.service.TranscriptionService;

@RestController
@RequestMapping("/api/transcriptions")
public class TranscriptionController {

	private final TranscriptionService service;

	public TranscriptionController(TranscriptionService service) {
		this.service = service;
	}

	@PostMapping
	public TranscriptionResponse transcribe(@RequestPart("file") MultipartFile file) {

		return service.transcribe(file);
	}

}