package com.example.diospeechai.transcription.infrastructure.messaging.rabbit;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.json.JsonMapper;

/**
 * Configuração de topologia do RabbitMQ.
 *
 * <p>Topologia declarada:
 * <pre>
 * Exchange de eventos (topic):
 *   transcription.events
 *     └── transcription.completed → Queue: transcription.completed
 *
 * Exchange de pedidos (direct):
 *   transcription.requests.exchange
 *     └── transcription.requests → Queue: transcription.requests
 *           x-dead-letter-exchange → transcription.dlx
 *           x-dead-letter-routing-key → transcription.requests.dlq
 *
 * Dead-Letter Exchange (direct):
 *   transcription.dlx
 *     └── transcription.requests.dlq → Queue: transcription.requests.dlq
 * </pre>
 *
 * <p>Todos os beans são declarativos — o Spring AMQP garante a criação
 * automática no broker na inicialização da aplicação.
 */
@Configuration
public class RabbitMQConfig {

    // ── Nomes ─────────────────────────────────────────────────────────────────

    public static final String EVENTS_EXCHANGE      = "transcription.events";
    public static final String COMPLETED_QUEUE      = "transcription.completed";
    public static final String COMPLETED_ROUTING_KEY = "transcription.completed";

    public static final String REQUESTS_EXCHANGE    = "transcription.requests.exchange";
    public static final String REQUESTS_QUEUE       = "transcription.requests";
    public static final String REQUESTS_ROUTING_KEY = "transcription.requests";

    public static final String DLX_EXCHANGE         = "transcription.dlx";
    public static final String DLQ_QUEUE            = "transcription.requests.dlq";
    public static final String DLQ_ROUTING_KEY      = "transcription.requests.dlq";

    // ── Exchange de eventos (topic) ───────────────────────────────────────────

    @Bean
    TopicExchange eventsExchange() {
        return new TopicExchange(EVENTS_EXCHANGE, true, false);
    }

    @Bean
    Queue completedQueue() {
        return QueueBuilder.durable(COMPLETED_QUEUE).build();
    }

    @Bean
    Binding completedBinding() {
        return BindingBuilder
                .bind(completedQueue())
                .to(eventsExchange())
                .with(COMPLETED_ROUTING_KEY);
    }

    // ── Exchange de pedidos (direct) ──────────────────────────────────────────

    @Bean
    DirectExchange requestsExchange() {
        return new DirectExchange(REQUESTS_EXCHANGE, true, false);
    }

    @Bean
    Queue requestsQueue() {
        return QueueBuilder.durable(REQUESTS_QUEUE)
                // Mensagens que falham vão para a DLQ após esgotarem as tentativas
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    Binding requestsBinding() {
        return BindingBuilder
                .bind(requestsQueue())
                .to(requestsExchange())
                .with(REQUESTS_ROUTING_KEY);
    }

    // ── Dead-Letter Exchange + Queue ──────────────────────────────────────────

    @Bean
    DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    Queue dlqQueue() {
        return QueueBuilder.durable(DLQ_QUEUE).build();
    }

    @Bean
    Binding dlqBinding() {
        return BindingBuilder
                .bind(dlqQueue())
                .to(dlxExchange())
                .with(DLQ_ROUTING_KEY);
    }

    // ── Serialização JSON ─────────────────────────────────────────────────────

    @Bean
    MessageConverter jacksonMessageConverter(JsonMapper jsonMapper) {
        return new JacksonJsonMessageConverter(jsonMapper);
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}