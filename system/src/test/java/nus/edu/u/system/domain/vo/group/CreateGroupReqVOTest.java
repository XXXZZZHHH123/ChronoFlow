package nus.edu.u.system.domain.vo.group;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CreateGroupReqVOTest {

    @Test
    void defaultSortIsZeroAndFieldsMutable() {
        CreateGroupReqVO vo = new CreateGroupReqVO();
        assertThat(vo.getSort()).isZero();

        vo.setName("Volunteers");
        vo.setEventId(77L);
        vo.setLeadUserId(9L);
        vo.setRemark("Morning shift");
        vo.setSort(5);

        assertThat(vo.getName()).isEqualTo("Volunteers");
        assertThat(vo.getEventId()).isEqualTo(77L);
        assertThat(vo.getLeadUserId()).isEqualTo(9L);
        assertThat(vo.getRemark()).isEqualTo("Morning shift");
        assertThat(vo.getSort()).isEqualTo(5);
    }
}
