package nus.edu.u.system.domain.vo.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class TaskRespVOTest {

    @Test
    void nestedValueObjectsRetainAssignments() {
        TaskRespVO.AssignerUserVO.GroupVO assignerGroup = new TaskRespVO.AssignerUserVO.GroupVO();
        assignerGroup.setId(10L);
        assignerGroup.setName("Ops");

        TaskRespVO.AssignerUserVO assigner = new TaskRespVO.AssignerUserVO();
        assigner.setId(1L);
        assigner.setName("Lead");
        assigner.setEmail("lead@example.com");
        assigner.setPhone("12345678");
        assigner.setGroups(List.of(assignerGroup));

        TaskRespVO.AssignedUserVO.GroupVO assignedGroup = new TaskRespVO.AssignedUserVO.GroupVO();
        assignedGroup.setId(11L);
        assignedGroup.setName("Volunteers");

        TaskRespVO.AssignedUserVO assigned = new TaskRespVO.AssignedUserVO();
        assigned.setId(2L);
        assigned.setName("Member");
        assigned.setEmail("member@example.com");
        assigned.setPhone("87654321");
        assigned.setGroups(List.of(assignedGroup));

        TaskRespVO.EventVO eventVO = new TaskRespVO.EventVO();
        eventVO.setId(7L);
        eventVO.setName("Tech Day");
        eventVO.setDescription("Annual gathering");
        eventVO.setOrganizerId(99L);
        eventVO.setLocation("Hall A");
        eventVO.setStatus(1);
        eventVO.setStartTime(LocalDateTime.of(2025, 10, 1, 9, 0));
        eventVO.setEndTime(eventVO.getStartTime().plusHours(8));
        eventVO.setRemark("Welcome!");

        TaskRespVO task = new TaskRespVO();
        task.setId(20L);
        task.setEventId(7L);
        task.setName("Prepare slides");
        task.setDescription("Finalize opening deck");
        task.setStatus(0);
        task.setRemark("High priority");
        task.setStartTime(LocalDateTime.of(2025, 9, 30, 10, 0));
        task.setEndTime(task.getStartTime().plusHours(2));
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        task.setAssignerUser(assigner);
        task.setAssignedUser(assigned);
        task.setEvent(eventVO);

        assertThat(task.getId()).isEqualTo(20L);
        assertThat(task.getAssignerUser().getGroups()).containsExactly(assignerGroup);
        assertThat(task.getAssignedUser().getGroups()).containsExactly(assignedGroup);
        assertThat(task.getEvent().getName()).isEqualTo("Tech Day");
        assertThat(task.getEvent().getRemark()).isEqualTo("Welcome!");
    }
}
