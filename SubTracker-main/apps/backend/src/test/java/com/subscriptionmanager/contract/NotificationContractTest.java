package com.subscriptionmanager.contract;

import com.subscriptionmanager.exception.GlobalExceptionHandler;
import com.subscriptionmanager.notification.controller.NotificationController;
import com.subscriptionmanager.notification.dto.NotificationResponse;
import com.subscriptionmanager.notification.service.NotificationService;
import com.subscriptionmanager.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationContractTest {

    private NotificationService notificationService;
    private CurrentUserService currentUserService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        notificationService = Mockito.mock(NotificationService.class);
        currentUserService = Mockito.mock(CurrentUserService.class);

        NotificationController controller = new NotificationController(notificationService, currentUserService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void notificationsContract() throws Exception {
        when(currentUserService.requireCurrentUserId()).thenReturn(1L);
        when(notificationService.list(1L, 7))
                .thenReturn(List.of(
                        new NotificationResponse(
                                10L,
                                "UPCOMING_CHARGE",
                                "Netflix will charge 12.99 USD in 2 days (billing date: 2026-03-18)",
                                "2026-03-18T09:00:00Z",
                                "PENDING"
                        ),
                        new NotificationResponse(
                                11L,
                                "PRICE_CHANGE",
                                "Spotify price changed from 9.99 USD to 11.99 USD on 2026-03-18",
                                "2026-03-18T10:00:00Z",
                                "PENDING"
                        ),
                        new NotificationResponse(
                                12L,
                                "INACTIVITY",
                                "YouTube Premium has no usage signals for 14+ days (last activity: 2026-03-01)",
                                "2026-03-18T11:00:00Z",
                                "PENDING"
                        )
                ));

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].type").value("UPCOMING_CHARGE"))
                .andExpect(jsonPath("$[0].message").value("Netflix will charge 12.99 USD in 2 days (billing date: 2026-03-18)"))
                .andExpect(jsonPath("$[0].scheduledAt").value("2026-03-18T09:00:00Z"))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[1].type").value("PRICE_CHANGE"))
                .andExpect(jsonPath("$[2].type").value("INACTIVITY"));

        verify(notificationService).list(1L, 7);
    }
}
