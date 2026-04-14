package com.example.diospeechai.security;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Endpoint de geração de tokens JWT — para desenvolvimento e testes.
 *
 * <p><strong>ATENÇÃO:</strong> Este controller aceita qualquer username/password
 * sem validação real. Em produção, substituir por integração com um IdP
 * (Keycloak, Auth0, etc.) ou por um repositório de usuários com senhas hashed.
 *
 * <p>Uso:
 * <pre>
 * POST /auth/token
 * { "username": "user", "password": "any" }
 *
 * → { "token": "eyJ..." }
 * </pre>
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtService jwtService;

    record TokenRequest(String username, String password) {}
    record TokenResponse(String token) {}

    @PostMapping("/token")
    public ResponseEntity<TokenResponse> token(@RequestBody TokenRequest request) {

        log.info("Token solicitado | username={}", request.username());

        // Validação simplificada — em produção, verificar credenciais no banco
        if (request.username() == null || request.username().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        String token = jwtService.generateToken(
                request.username(),
                List.of("ROLE_USER")
        );

        return ResponseEntity.ok(new TokenResponse(token));
    }
}