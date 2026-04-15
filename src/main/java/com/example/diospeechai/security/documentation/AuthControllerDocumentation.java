package com.example.diospeechai.security.documentation;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.diospeechai.security.dto.TokenRequest;
import com.example.diospeechai.security.dto.TokenResponse;

@Tag(name = "Autenticação", description = "Geração de tokens JWT para desenvolvimento e testes")
public interface AuthControllerDocumentation {

	@Operation(
		summary = "Gerar token JWT", 
		description = """
			Gera um token JWT válido para uso no header `Authorization: Bearer <token>`.

			> ⚠️ **Apenas para desenvolvimento.** Aceita qualquer username/password sem validação.
			Em produção, substituir por integração com Keycloak, Auth0 ou similar.

			**Como usar no Swagger UI:**
			1. Execute esta operação para obter o token
			2. Copie o valor do campo `token`
			3. Clique no botão **Authorize** (cadeado) no topo da página
			4. Cole o token no campo `bearerAuth` e confirme
			""",
			// Endpoint público — sobrescreve o requisito de segurança global
			security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "")
	)
	@SecurityRequirements // sem requisitos de segurança — endpoint público
	@ApiResponses({
		@ApiResponse(
			responseCode = "200",
			description = "Token gerado com sucesso",
			content = @Content(
				mediaType = MediaType.APPLICATION_JSON_VALUE,
				schema = @Schema(implementation = TokenResponse.class),
				examples = @ExampleObject(value = """
						{
						  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyIiwicm9sZXMiOlsiUk9MRV9VU0VSIl0sImlhdCI6MTcxMzgzMjAwMCwiZXhwIjoxNzEzODYwODAwfQ.abc123"
						}
						""")
			)
		),
		@ApiResponse(responseCode = "400", description = "Username ausente ou vazio")
	})
	ResponseEntity<TokenResponse> token(@RequestBody TokenRequest request);

}
