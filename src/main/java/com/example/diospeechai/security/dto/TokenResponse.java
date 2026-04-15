package com.example.diospeechai.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TokenResponse(
		@Schema(description = "Token JWT — usar no header: Authorization: Bearer <token>")
		String token
) {}