package nus.edu.u.system.service.notification;

import nus.edu.u.system.domain.dto.NotificationRequestDTO;

public abstract class NotificationService {
    protected final TransportImplementor transportImplementor;
    protected final TemplateEngineImplementor templateEngineImplementor;

    protected NotificationService(
            TransportImplementor transportImplementor,
            TemplateEngineImplementor templateEngineImplementor) {
        this.transportImplementor = transportImplementor;
        this.templateEngineImplementor = templateEngineImplementor;
    }

    public abstract void send(NotificationRequestDTO notificationRequestDTO);
}
