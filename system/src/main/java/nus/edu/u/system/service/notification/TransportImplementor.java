package nus.edu.u.system.service.notification;


import nus.edu.u.system.domain.dto.NotificationRequestDTO;

public interface TransportImplementor {

    void process(NotificationRequestDTO notification);
}
