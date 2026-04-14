package com.example.diospeechai.security;

/**
 * Lançada quando um token JWT é inválido, expirado ou malformado.
 * Mapeada para HTTP 401 pelo {@code GlobalExceptionHandler}.
 */
public class JwtValidationException extends RuntimeException {
 
    public JwtValidationException(String message) {
        super(message);
    }
}