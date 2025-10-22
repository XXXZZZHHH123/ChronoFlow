package nus.edu.u.system.service.email;

import nus.edu.u.system.domain.dto.NotificationRequestDTO;
import nus.edu.u.system.enums.email.NotificationChannel;

public interface NotificationSender {
    boolean supports(NotificationChannel channel);

    String send(NotificationRequestDTO request);
}
