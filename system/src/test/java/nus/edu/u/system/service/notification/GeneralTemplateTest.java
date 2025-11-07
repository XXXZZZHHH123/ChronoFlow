package nus.edu.u.system.service.notification;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import nus.edu.u.system.domain.dto.TemplateRequestDTO;
import nus.edu.u.system.domain.dto.TemplateResponseDTO;
import nus.edu.u.system.enums.email.TemplateProvider;
import nus.edu.u.system.provider.template.TemplateClient;
import nus.edu.u.system.provider.template.TemplateClientFactory;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

class GeneralTemplateTest {

    @Test
    void process_delegates_to_factory_client_and_returns_result() {
        // Arrange
        TemplateRequestDTO req = new TemplateRequestDTO();
        req.setTemplateId("invite");
        req.setVariables(Map.of("user", "Thet"));
        req.setLocale(Locale.ENGLISH);
        req.setTemplateProvider(TemplateProvider.Thymeleaf);

        TemplateResponseDTO expected =
                TemplateResponseDTO.builder()
                        .subject("Hello")
                        .body("<p>Hi</p>")
                        .attachments(List.of())
                        .build();

        AtomicReference<TemplateClient> clientRef = new AtomicReference<>();

        try (MockedConstruction<TemplateClientFactory> mocked =
                Mockito.mockConstruction(
                        TemplateClientFactory.class,
                        (mockFactory, context) -> {
                            TemplateClient client = mock(TemplateClient.class);
                            when(mockFactory.getClient(TemplateProvider.Thymeleaf))
                                    .thenReturn(client);
                            when(client.render(req)).thenReturn(expected);
                            clientRef.set(client);
                        })) {

            GeneralTemplate sut =
                    new GeneralTemplate(); // uses new TemplateClientFactory() internally

            // Act
            TemplateResponseDTO actual = sut.process(req);

            // Assert
            assertSame(expected, actual);
            TemplateClientFactory constructedFactory = mocked.constructed().get(0);
            verify(constructedFactory).getClient(TemplateProvider.Thymeleaf);
            verify(clientRef.get()).render(req);
        }
    }

    @Test
    void process_handles_null_variables() {
        // Arrange
        TemplateRequestDTO req = new TemplateRequestDTO();
        req.setTemplateId("invite");
        req.setVariables(null); // null variables path
        req.setLocale(Locale.ENGLISH);
        req.setTemplateProvider(TemplateProvider.Thymeleaf);

        TemplateResponseDTO expected =
                TemplateResponseDTO.builder()
                        .subject("Welcome")
                        .body("<p>Welcome</p>")
                        .attachments(List.of())
                        .build();

        AtomicReference<TemplateClient> clientRef = new AtomicReference<>();

        try (MockedConstruction<TemplateClientFactory> mocked =
                Mockito.mockConstruction(
                        TemplateClientFactory.class,
                        (mockFactory, context) -> {
                            TemplateClient client = mock(TemplateClient.class);
                            when(mockFactory.getClient(TemplateProvider.Thymeleaf))
                                    .thenReturn(client);
                            when(client.render(req)).thenReturn(expected);
                            clientRef.set(client);
                        })) {

            GeneralTemplate sut = new GeneralTemplate();

            // Act
            TemplateResponseDTO actual = sut.process(req);

            // Assert
            assertSame(expected, actual);
            verify(mocked.constructed().get(0)).getClient(TemplateProvider.Thymeleaf);
            verify(clientRef.get()).render(req);
        }
    }
}
