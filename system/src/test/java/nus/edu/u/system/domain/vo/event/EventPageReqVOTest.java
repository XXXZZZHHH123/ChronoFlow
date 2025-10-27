package nus.edu.u.system.domain.vo.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class EventPageReqVOTest {

    @Test
    void defaultsAndMutatorsWork() {
        EventPageReqVO vo = new EventPageReqVO();

        assertThat(vo.getPageNo()).isEqualTo(1);
        assertThat(vo.getPageSize()).isEqualTo(10);

        LocalDateTime from = LocalDateTime.of(2025, 10, 1, 10, 0);
        LocalDateTime to = from.plusHours(2);

        vo.setPageNo(3);
        vo.setPageSize(50);
        vo.setName("Hackathon");
        vo.setStatus(1);
        vo.setOrganizerId(99L);
        vo.setLocation("Campus");
        vo.setStartTimeFrom(from);
        vo.setStartTimeTo(to);

        assertThat(vo.getPageNo()).isEqualTo(3);
        assertThat(vo.getPageSize()).isEqualTo(50);
        assertThat(vo.getName()).isEqualTo("Hackathon");
        assertThat(vo.getStatus()).isEqualTo(1);
        assertThat(vo.getOrganizerId()).isEqualTo(99L);
        assertThat(vo.getLocation()).isEqualTo("Campus");
        assertThat(vo.getStartTimeFrom()).isEqualTo(from);
        assertThat(vo.getStartTimeTo()).isEqualTo(to);
    }
}
