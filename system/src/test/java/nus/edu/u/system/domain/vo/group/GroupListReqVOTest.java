package nus.edu.u.system.domain.vo.group;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GroupListReqVOTest {

    @Test
    void defaultPaginationCanBeOverridden() {
        GroupListReqVO vo = new GroupListReqVO();

        assertThat(vo.getPageNo()).isEqualTo(1);
        assertThat(vo.getPageSize()).isEqualTo(10);

        vo.setPageNo(4);
        vo.setPageSize(25);
        vo.setName("Onsite");
        vo.setEventId(8L);
        vo.setStatus(1);
        vo.setLeadUserId(3L);

        assertThat(vo.getPageNo()).isEqualTo(4);
        assertThat(vo.getPageSize()).isEqualTo(25);
        assertThat(vo.getName()).isEqualTo("Onsite");
        assertThat(vo.getEventId()).isEqualTo(8L);
        assertThat(vo.getStatus()).isEqualTo(1);
        assertThat(vo.getLeadUserId()).isEqualTo(3L);
    }
}
