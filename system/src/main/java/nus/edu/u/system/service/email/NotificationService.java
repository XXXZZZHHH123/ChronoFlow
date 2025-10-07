package nus.edu.u.system.service.email;

import nus.edu.u.system.domain.dto.NotificationRequestDTO;

public interface NotificationService {
    String send(NotificationRequestDTO request);
}
