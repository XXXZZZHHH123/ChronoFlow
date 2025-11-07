package nus.edu.u.system.service.notification;

import nus.edu.u.system.domain.dto.NotificationRequestDTO;
import nus.edu.u.system.domain.dto.TemplateRequestDTO;
import nus.edu.u.system.domain.dto.TemplateResponseDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService extends NotificationService {

    public EmailNotificationService(
            @Qualifier("emailTransport") TransportImplementor transportImplementor,
            @Qualifier("generalTemplate") TemplateEngineImplementor templateEngineImplementor) {
        super(transportImplementor, templateEngineImplementor);
    }

    @Override
    public void send(NotificationRequestDTO dto) {

        TemplateRequestDTO templateRequestDTO =
                TemplateRequestDTO.builder()
                        .templateId(dto.getTemplateId())
                        .variables(dto.getVariables())
                        .locale(dto.getLocale())
                        .templateProvider(dto.getTemplateProvider())
                        .build();

        TemplateResponseDTO tpl = templateEngineImplementor.process(templateRequestDTO);

        NotificationRequestDTO emailReq =
                NotificationRequestDTO.builder()
                        .emailProvider(dto.getEmailProvider())
                        .to(dto.getTo())
                        .subject(tpl.getSubject())
                        .body(tpl.getBody())
                        .attachment(dto.getAttachment())
                        .build();

        transportImplementor.process(emailReq);
    }
}
