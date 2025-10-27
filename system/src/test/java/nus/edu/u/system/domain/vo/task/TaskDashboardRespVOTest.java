package nus.edu.u.system.domain.vo.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class TaskDashboardRespVOTest {

    @Test
    void aggregatesMemberGroupsAndTasks() {
        TaskDashboardRespVO.MemberVO member = new TaskDashboardRespVO.MemberVO();
        member.setId(5L);
        member.setUsername("member01");
        member.setEmail("member@example.com");
        member.setPhone("12312312");
        member.setStatus(1);
        member.setCreateTime(LocalDateTime.of(2025, 9, 1, 10, 0));
        member.setUpdateTime(member.getCreateTime().plusDays(1));

        TaskDashboardRespVO.GroupVO.EventVO eventVO = new TaskDashboardRespVO.GroupVO.EventVO();
        eventVO.setId(9L);
        eventVO.setName("Expo");
        eventVO.setStatus(1);

        TaskDashboardRespVO.GroupVO group = new TaskDashboardRespVO.GroupVO();
        group.setId(2L);
        group.setName("Volunteers");
        group.setSort(1);
        group.setLeadUserId(5L);
        group.setRemark("Morning shift");
        group.setStatus(1);
        group.setEvent(eventVO);

        TasksRespVO task = new TasksRespVO();
        task.setId(33L);
        task.setName("Setup booth");
        task.setStatus(0);

        TaskDashboardRespVO dashboard = new TaskDashboardRespVO();
        dashboard.setMember(member);
        dashboard.setGroups(List.of(group));
        dashboard.setTasks(List.of(task));

        assertThat(dashboard.getMember().getUsername()).isEqualTo("member01");
        assertThat(dashboard.getGroups()).containsExactly(group);
        assertThat(dashboard.getGroups().get(0).getEvent().getName()).isEqualTo("Expo");
        assertThat(dashboard.getTasks()).containsExactly(task);
    }
}
