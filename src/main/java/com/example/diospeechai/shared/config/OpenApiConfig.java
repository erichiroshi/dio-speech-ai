package com.example.diospeechai.shared.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

/**
 * Configuração da especificação OpenAPI 3.1 via SpringDoc.
 *
 * <p>Define:
 * <ul>
 *   <li>Metadados da API (título, versão, descrição, contato, licença)</li>
 *   <li>Servidores (dev e prod)</li>
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

    @Value("${spring.application.name}")
    private String appName;

    @Bean
    OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(servers());
    }

    private Info apiInfo() {
        return new Info()
                .title(appName + "API")
                .version("5.1.0")
                .description("""
                        API REST de transcrição de áudio com Whisper via Speaches.
                        
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
                        .description("Desenvolvimento local")
        );
    }
}