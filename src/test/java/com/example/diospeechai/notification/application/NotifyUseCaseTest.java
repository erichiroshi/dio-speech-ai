package com.example.diospeechai.notification.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.diospeechai.notification.domain.model.NotificationChannel;
import com.example.diospeechai.notification.domain.model.NotificationRequest;
import com.example.diospeechai.notification.domain.port.out.NotificationPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotifyUseCase — testes unitários")
class NotifyUseCaseTest {

    @Mock NotificationPort emailAdapter;
    @Mock NotificationPort smsAdapter;
    @Mock NotificationFactory factory;

    private NotifyUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new NotifyUseCase(factory);
    }

    @Test
    @DisplayName("Deve delegar para o adapter EMAIL quando canal é EMAIL")
    void shouldDelegateToEmailAdapter() {
        NotificationRequest request = new NotificationRequest(
                UUID.randomUUID(),
                "user@example.com",
                NotificationChannel.EMAIL,
                "texto transcrito");

        when(factory.get(NotificationChannel.EMAIL))
                .thenReturn(emailAdapter);

        useCase.notify(request);

        verify(factory).get(NotificationChannel.EMAIL);
        verify(emailAdapter).send(request);
        verify(smsAdapter, never()).send(request);
    }

    @Test
    @DisplayName("Deve delegar para o adapter SMS quando canal é SMS")
    void shouldDelegateToSmsAdapter() {
        NotificationRequest request = new NotificationRequest(
                UUID.randomUUID(),
                "+5567999999999",
                NotificationChannel.SMS,
                "texto transcrito");

        when(factory.get(NotificationChannel.SMS))
                .thenReturn(smsAdapter);

        useCase.notify(request);

        verify(factory).get(NotificationChannel.SMS);
        verify(smsAdapter).send(request);
        verify(emailAdapter, never()).send(request);
    }

    @Test
    @DisplayName("Deve lançar exceção para canal sem adapter registrado")
    void shouldThrowForUnregisteredChannel() {
        NotificationRequest request = new NotificationRequest(
                UUID.randomUUID(),
                "user",
                NotificationChannel.WHATSAPP,
                "texto");

        when(factory.get(NotificationChannel.WHATSAPP))
                .thenThrow(new IllegalArgumentException(
                        "Canal de notificação sem adapter registrado: WHATSAPP"));

        assertThatThrownBy(() -> useCase.notify(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("WHATSAPP");

        verify(factory).get(NotificationChannel.WHATSAPP);
    }

    @Test
    @DisplayName("Deve relançar exceção do adapter e não silenciar falhas")
    void shouldRethrowAdapterExceptions() {
        NotificationRequest request = new NotificationRequest(
                UUID.randomUUID(),
                "user@example.com",
                NotificationChannel.EMAIL,
                "texto");

        when(factory.get(NotificationChannel.EMAIL))
                .thenReturn(emailAdapter);

        doThrow(new RuntimeException("SMTP down"))
                .when(emailAdapter)
                .send(request);

        assertThatThrownBy(() -> useCase.notify(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("SMTP down");

        verify(factory).get(NotificationChannel.EMAIL);
        verify(emailAdapter).send(request);
    }
}