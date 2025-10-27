package nus.edu.u.system.domain.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class TaskActionDTOTest {

    @Test
    void builderRetainsTemporalFieldsAndFiles() {
        LocalDateTime start = LocalDateTime.of(2025, 10, 1, 9, 0);
        LocalDateTime end = start.plusHours(2);

        MockMultipartFile file =
                new MockMultipartFile("files", "plan.txt", "text/plain", "plan".getBytes());

        TaskActionDTO dto =
                TaskActionDTO.builder()
                        .name("Prepare venue")
                        .description("Arrange seats")
                        .startTime(start)
                        .endTime(end)
                        .targetUserId(15L)
                        .files(List.of(file))
                        .eventStartTime(start.minusDays(1))
                        .eventEndTime(end.plusDays(1))
                        .remark("Need projector")
                        .build();

        assertThat(dto.getName()).isEqualTo("Prepare venue");
        assertThat(dto.getDescription()).isEqualTo("Arrange seats");
        assertThat(dto.getStartTime()).isEqualTo(start);
        assertThat(dto.getEndTime()).isEqualTo(end);
        assertThat(dto.getTargetUserId()).isEqualTo(15L);
        assertThat(dto.getFiles()).containsExactly(file);
        assertThat(dto.getEventStartTime()).isEqualTo(start.minusDays(1));
        assertThat(dto.getEventEndTime()).isEqualTo(end.plusDays(1));
        assertThat(dto.getRemark()).isEqualTo("Need projector");
    }
}
