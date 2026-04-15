package com.example.diospeechai.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TokenRequest(
		@Schema(description = "Nome do usuário", example = "user")
		String username,
		
		@Schema(description = "Senha (qualquer valor em dev)", example = "any")
		String password
) {}