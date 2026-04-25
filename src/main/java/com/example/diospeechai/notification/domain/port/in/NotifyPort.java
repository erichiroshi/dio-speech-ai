package com.example.diospeechai.notification.domain.port.in;

import com.example.diospeechai.notification.domain.model.NotificationRequest;

public interface NotifyPort {

	void notify(NotificationRequest request);
}
