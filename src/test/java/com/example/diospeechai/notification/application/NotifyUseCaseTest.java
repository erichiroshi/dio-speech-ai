package com.example.diospeechai.notification.application;

import java.util.List;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários do {@link NotifyUseCase}.
 * Zero Spring, zero infra, ~10ms.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotifyUseCase — testes unitários")
class NotifyUseCaseTest {

    @Mock NotificationPort emailAdapter;
    @Mock NotificationPort smsAdapter;

    NotifyUseCase useCase;

    @BeforeEach
    void setUp() {
        when(emailAdapter.channel()).thenReturn(NotificationChannel.EMAIL);
        when(smsAdapter.channel()).thenReturn(NotificationChannel.SMS);
        useCase = new NotifyUseCase(List.of(emailAdapter, smsAdapter));
    }

    @Test
    @DisplayName("Deve delegar para o adapter EMAIL quando canal é EMAIL")
    void shouldDelegateToEmailAdapter() {
        var request = new NotificationRequest(
                UUID.randomUUID(), "user@example.com",
                NotificationChannel.EMAIL, "texto transcrito");

        useCase.notify(request);

        verify(emailAdapter).send(request);
        verify(smsAdapter, never()).send(request);
    }

    @Test
    @DisplayName("Deve delegar para o adapter SMS quando canal é SMS")
    void shouldDelegateToSmsAdapter() {
        var request = new NotificationRequest(
                UUID.randomUUID(), "+5567999999999",
                NotificationChannel.SMS, "texto transcrito");

        useCase.notify(request);

        verify(smsAdapter).send(request);
        verify(emailAdapter, never()).send(request);
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException para canal sem adapter registrado")
    void shouldThrowForUnregisteredChannel() {
        var request = new NotificationRequest(
                UUID.randomUUID(), "user",
                NotificationChannel.WHATSAPP, "texto");

        assertThatThrownBy(() -> useCase.notify(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("WHATSAPP");
    }

    @Test
    @DisplayName("Deve relançar exceção do adapter e não silenciar falhas")
    void shouldRethrowAdapterExceptions() {
        var request = new NotificationRequest(
                UUID.randomUUID(), "user@example.com",
                NotificationChannel.EMAIL, "texto");
        org.mockito.Mockito.doThrow(new RuntimeException("SMTP down"))
                .when(emailAdapter).send(request);

        assertThatThrownBy(() -> useCase.notify(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("SMTP down");
    }
}