package com.example.diospeechai.notification.application;

import org.springframework.stereotype.Service;

import com.example.diospeechai.notification.domain.model.NotificationRequest;
import com.example.diospeechai.notification.domain.port.in.NotifyPort;
import com.example.diospeechai.notification.domain.port.out.NotificationPort;

import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class NotifyUseCase implements NotifyPort {
	
	private final NotificationFactory factory;

    /**
     * Envia a notificação pelo canal indicado no request.
     *
     * @param request dados da notificação
     * @throws IllegalArgumentException se o canal não tiver adapter registrado
     */
    @Override
    public void notify(NotificationRequest request) {
        NotificationPort notificationAdapter = factory.get(request.channel());

        log.info("Enviando notificação | channel={} | transcriptionId={}",
                request.channel(), request.transcriptionId());

        try {
            notificationAdapter.send(request);
            log.info("Notificação enviada | channel={} | transcriptionId={}",
                    request.channel(), request.transcriptionId());
        } catch (Exception ex) {
            log.error("Falha ao enviar notificação | channel={} | transcriptionId={} | error={}",
                    request.channel(), request.transcriptionId(), ex.getMessage());
            throw ex;
        }
    }
}