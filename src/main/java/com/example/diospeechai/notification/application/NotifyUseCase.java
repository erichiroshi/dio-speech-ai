package com.example.diospeechai.notification.application;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.diospeechai.notification.domain.model.NotificationChannel;
import com.example.diospeechai.notification.domain.model.NotificationRequest;
import com.example.diospeechai.notification.domain.port.in.NotifyPort;
import com.example.diospeechai.notification.domain.port.out.NotificationPort;

import lombok.extern.slf4j.Slf4j;

/**
 * Caso de uso: enviar notificação pelo canal solicitado.
 *
 * <p>O Spring injeta todos os beans que implementam {@link NotificationPort}.
 * O use case monta um mapa {@code canal → adapter} na construção e delega
 * para o adapter correto em tempo de execução.
 *
 * <p>Para adicionar um novo canal basta criar um novo adapter que implemente
 * {@link NotificationPort}. O use case não precisa ser alterado.
 */
@Slf4j
@Service
public class NotifyUseCase implements NotifyPort {

    private final Map<NotificationChannel, NotificationPort> adapters;

    public NotifyUseCase(List<NotificationPort> ports) {
        this.adapters = ports.stream()
                .collect(Collectors.toMap(NotificationPort::channel, Function.identity()));
    }

    /**
     * Envia a notificação pelo canal indicado no request.
     *
     * @param request dados da notificação
     * @throws IllegalArgumentException se o canal não tiver adapter registrado
     */
    @Override
    public void notify(NotificationRequest request) {
        NotificationPort adapter = adapters.get(request.channel());

        if (adapter == null) {
            log.warn("Canal de notificação sem adapter registrado | channel={}",
                    request.channel());
            throw new IllegalArgumentException(
                    "Canal não suportado: " + request.channel());
        }

        log.info("Enviando notificação | channel={} | transcriptionId={}",
                request.channel(), request.transcriptionId());

        try {
            adapter.send(request);
            log.info("Notificação enviada | channel={} | transcriptionId={}",
                    request.channel(), request.transcriptionId());
        } catch (Exception ex) {
            log.error("Falha ao enviar notificação | channel={} | transcriptionId={} | error={}",
                    request.channel(), request.transcriptionId(), ex.getMessage());
            throw ex;
        }
    }
}