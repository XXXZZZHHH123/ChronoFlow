package nus.edu.u.system.service.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import nus.edu.u.system.domain.dto.NotificationRequestDTO;
import nus.edu.u.system.domain.dto.TemplateRequestDTO;
import nus.edu.u.system.domain.dto.TemplateResponseDTO;
import nus.edu.u.system.enums.email.EmailProvider;
import nus.edu.u.system.enums.email.TemplateProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EmailNotificationServiceTest {

    @Test
    void send_buildsTemplateRequest_mapsResponse_andDelegatesToTransport() {
        // mocks
        TransportImplementor transport = mock(TransportImplementor.class);
        TemplateEngineImplementor template = mock(TemplateEngineImplementor.class);

        // SUT
        EmailNotificationService svc = new EmailNotificationService(transport, template);

        // input dto
        NotificationRequestDTO in = NotificationRequestDTO.builder()
                .emailProvider(EmailProvider.AWS_SES)
                .to("user@example.com")
                .templateId("invite-email")
                .templateProvider(TemplateProvider.Thymeleaf)
                .variables(Map.of("name", "Thet"))
                .locale(Locale.ENGLISH)
                .attachment(List.of()) // or whatever your type is
                .build();

        // template response (stub)
        TemplateResponseDTO tpl = mock(TemplateResponseDTO.class);
        when(tpl.getSubject()).thenReturn("Subject from template");
        when(tpl.getBody()).thenReturn("<p>Hello</p>");
        when(template.process(any(TemplateRequestDTO.class))).thenReturn(tpl);

        // act
        svc.send(in);

        // verify template request built correctly
        ArgumentCaptor<TemplateRequestDTO> tplReqCap = ArgumentCaptor.forClass(TemplateRequestDTO.class);
        verify(template).process(tplReqCap.capture());
        TemplateRequestDTO sentTplReq = tplReqCap.getValue();
        assertEquals("invite-email", sentTplReq.getTemplateId());
        assertEquals(TemplateProvider.Thymeleaf, sentTplReq.getTemplateProvider());
        assertEquals(Locale.ENGLISH, sentTplReq.getLocale());
        assertEquals("Thet", sentTplReq.getVariables().get("name"));

        // verify transport called with mapped email request
        ArgumentCaptor<NotificationRequestDTO> outCap = ArgumentCaptor.forClass(NotificationRequestDTO.class);
        verify(transport).process(outCap.capture());
        NotificationRequestDTO sentEmail = outCap.getValue();

        assertEquals(EmailProvider.AWS_SES, sentEmail.getEmailProvider());
        assertEquals("user@example.com", sentEmail.getTo());
        assertEquals("Subject from template", sentEmail.getSubject());
        assertEquals("<p>Hello</p>", sentEmail.getBody());
        assertEquals(in.getAttachment(), sentEmail.getAttachment()); // passthrough
    }

    @Test
    void send_works_whenVariablesAreNull() {
        TransportImplementor transport = mock(TransportImplementor.class);
        TemplateEngineImplementor template = mock(TemplateEngineImplementor.class);
        EmailNotificationService svc = new EmailNotificationService(transport, template);

        NotificationRequestDTO in = NotificationRequestDTO.builder()
                .emailProvider(EmailProvider.AWS_SES)
                .to("user@example.com")
                .templateId("invite-email")
                .templateProvider(TemplateProvider.Thymeleaf)
                .variables(null) // null vars path
                .locale(Locale.ENGLISH)
                .attachment(List.of())
                .build();

        TemplateResponseDTO tpl = mock(TemplateResponseDTO.class);
        when(tpl.getSubject()).thenReturn("S");
        when(tpl.getBody()).thenReturn("B");
        when(template.process(any(TemplateRequestDTO.class))).thenReturn(tpl);

        svc.send(in);

        // still delegates
        verify(template).process(any(TemplateRequestDTO.class));
        verify(transport).process(any(NotificationRequestDTO.class));
    }
}