package com.shopfast.notificationservice.dto;

import com.shopfast.notificationservice.enums.NotificationChannel;
import com.shopfast.notificationservice.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateNotificationRequestDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID userId;

    @NotBlank
    private String recipient; //email or phone

    private String subject;

    @NotBlank
    private String content;

    @NotNull
    private NotificationType type;

    @NotNull
    private NotificationChannel channel;

}
