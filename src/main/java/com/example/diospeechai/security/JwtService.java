package com.example.diospeechai.security;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Serviço de criação e validação de tokens JWT assinados com HMAC-SHA256 (HS256).
 *
 * <p>Estrutura do token:
 * <ul>
 *   <li>{@code sub} — username</li>
 *   <li>{@code roles} — lista de papéis (ex: ["ROLE_USER"])</li>
 *   <li>{@code iat} — emitido em</li>
 *   <li>{@code exp} — expira em (iat + expirationHours)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties properties;

    /**
     * Gera um token JWT assinado para o usuário informado.
     *
     * @param username nome do usuário (subject)
     * @param roles    lista de papéis, ex: {@code List.of("ROLE_USER")}
     * @return token JWT serializado (compact form: header.payload.signature)
     */
    public String generateToken(String username, List<String> roles) {
        try {
            Instant now = Instant.now();
            Instant expiry = now.plusSeconds(properties.expirationHours() * 3600);

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(username)
                    .claim("roles", roles)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(expiry))
                    .build();

            JWSSigner signer = new MACSigner(properties.secret().getBytes());
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(signer);

            return jwt.serialize();

        } catch (JOSEException ex) {
            throw new IllegalStateException("Falha ao gerar token JWT", ex);
        }
    }

    /**
     * Valida a assinatura e a expiração do token.
     *
     * @return {@link JWTClaimsSet} se válido
     * @throws JwtValidationException se inválido, expirado ou malformado
     */
    public JWTClaimsSet validateToken(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);

            JWSVerifier verifier = new MACVerifier(properties.secret().getBytes());
            if (!jwt.verify(verifier)) {
                throw new JwtValidationException("Assinatura JWT inválida");
            }

            JWTClaimsSet claims = jwt.getJWTClaimsSet();

            if (claims.getExpirationTime().before(new Date())) {
                throw new JwtValidationException("Token JWT expirado");
            }

            return claims;

        } catch (ParseException _) {
            throw new JwtValidationException("Token JWT malformado");
        } catch (JOSEException _) {
            throw new JwtValidationException("Erro ao verificar assinatura JWT");
        }
    }
}