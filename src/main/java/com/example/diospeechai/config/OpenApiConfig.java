package com.example.diospeechai.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuração da especificação OpenAPI 3.1 via SpringDoc.
 *
 * <p>Define:
 * <ul>
 *   <li>Metadados da API (título, versão, descrição, contato, licença)</li>
 *   <li>Servidores (dev e prod)</li>
 *   <li>Esquema de segurança JWT Bearer — aparece no botão "Authorize" do Swagger UI</li>
 *   <li>Requisito de segurança global — todas as operações exigem o token por padrão</li>
 * </ul>
 *
 * <p>O Swagger UI fica disponível em:
 * <ul>
 *   <li>Dev: <a href="http://localhost:8080/swagger-ui.html">http://localhost:8080/swagger-ui.html</a></li>
 *   <li>Spec JSON: <a href="http://localhost:8080/v3/api-docs">http://localhost:8080/v3/api-docs</a></li>
 * </ul>
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Value("${spring.application.name}")
    private String appName;

    @Bean
    OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(servers())
                .components(securityComponents())
                // Requisito global: todas as operações exigem JWT por padrão
                // Operações públicas sobrescrevem isso com @SecurityRequirements({})
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }

    private Info apiInfo() {
        return new Info()
                .title("dio-speech-ai API")
                .version("4.1.0")
                .description("""
                        API REST de transcrição de áudio com Whisper via Speaches.
                        
                        ## Autenticação
                        
                        Obtenha um token JWT via `POST /auth/token` e clique em **Authorize** \
                        para autenticar as requisições no Swagger UI.
                        
                        ```
                        POST /auth/token
                        { "username": "user", "password": "any" }
                        ```
                        
                        ## Cache inteligente
                        
                        O SHA-256 do conteúdo do arquivo é usado como chave de cache. \
                        Arquivos idênticos (independente do nome) retornam da cache Redis em ~15ms. \
                        A resposta inclui `"cached": true` quando servida do cache.
                        
                        ## Observabilidade
                        
                        - Métricas: `GET /actuator/prometheus`
                        - Health (incl. CircuitBreaker): `GET /actuator/health`
                        - Tracing: Zipkin em `http://localhost:9411`
                        """)
                .contact(new Contact()
                        .name("Eric Hiroshi")
                        .url("https://www.linkedin.com/in/eric-hiroshi/")
                        .email("erichiroshi@hotmail.com"))
                .license(new License()
                        .name("MIT")
                        .url("https://github.com/erichiroshi/dio-speech-ai/blob/main/LICENSE"));
    }

    private List<Server> servers() {
        return List.of(
                new Server()
                        .url("http://localhost:8080")
                        .description("Desenvolvimento local"),
                new Server()
                        .url("http://localhost:8080")
                        .description("Produção (Docker Compose)")
        );
    }

    private Components securityComponents() {
        return new Components()
                .addSecuritySchemes(SECURITY_SCHEME_NAME,
                        new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("""
                                        Token JWT obtido via `POST /auth/token`.
                                        
                                        Informe apenas o token, sem o prefixo "Bearer ".
                                        O prefixo é adicionado automaticamente.
                                        """));
    }
}