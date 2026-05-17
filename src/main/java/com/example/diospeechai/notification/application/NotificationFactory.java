package com.example.diospeechai.notification.application;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.example.diospeechai.notification.domain.model.NotificationChannel;
import com.example.diospeechai.notification.domain.port.out.NotificationPort;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class NotificationFactory {

	private final Map<NotificationChannel, NotificationPort> adapters;

	public NotificationFactory(List<NotificationPort> ports) { 
		this.adapters = ports
				.stream()
				.collect(
						Collectors.toMap(
								NotificationPort::channel,
								Function.identity())); }

	public NotificationPort get(NotificationChannel channel) {
		NotificationPort notificationAdapter = adapters.get(channel);
		if (notificationAdapter == null) {
            log.warn("Canal de notificação sem adapter registrado | channel={}", channel);
			throw new IllegalArgumentException("Canal de notificação sem adapter registrado: " + channel);
		}
		
		return notificationAdapter;
	}
}
