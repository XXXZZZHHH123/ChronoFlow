package nus.edu.u.system.domain.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import nus.edu.u.system.enums.email.NotificationChannel;
import org.junit.jupiter.api.Test;

class NotificationRequestDTOTest {

    @Test
    void builderPopulatesRecordFields() {
        AttachmentDTO attachment =
                new AttachmentDTO(
                        "agenda.pdf", "application/pdf", new byte[] {1, 2, 3}, null, false, null);

        NotificationRequestDTO dto =
                NotificationRequestDTO.builder()
                        .channel(NotificationChannel.EMAIL)
                        .to("member@example.com")
                        .templateId("event-invite")
                        .variables(Map.of("eventName", "Tech Day"))
                        .locale(Locale.ENGLISH)
                        .attachments(List.of(attachment))
                        .build();

        assertThat(dto.channel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(dto.to()).isEqualTo("member@example.com");
        assertThat(dto.templateId()).isEqualTo("event-invite");
        assertThat(dto.variables()).containsEntry("eventName", "Tech Day");
        assertThat(dto.locale()).isEqualTo(Locale.ENGLISH);
        assertThat(dto.attachments()).containsExactly(attachment);
    }
}
