package com.example.diospeechai.notification.infrastructure;

import org.springframework.stereotype.Component;

import com.example.diospeechai.notification.domain.model.NotificationChannel;
import com.example.diospeechai.notification.domain.model.NotificationRequest;
import com.example.diospeechai.notification.domain.port.out.NotificationPort;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class NoOpNotificationAdapter implements NotificationPort {

	@Override
	public void send(NotificationRequest request) {
		log.debug("Notificação (no-op) | channel={} | transcriptionId={} | recipient='{}' | text='{}'",
				request.channel(), request.transcriptionId(), request.recipientContact(), request.transcribedText());
	}

	@Override
	public NotificationChannel channel() {
		return NotificationChannel.NO_OP;
	}

}
