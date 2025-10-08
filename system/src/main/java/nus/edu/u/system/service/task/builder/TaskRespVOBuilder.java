package nus.edu.u.system.service.task.builder;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import nus.edu.u.system.convert.task.TaskConvert;
import nus.edu.u.system.domain.dataobject.task.EventDO;
import nus.edu.u.system.domain.dataobject.task.TaskDO;
import nus.edu.u.system.domain.dataobject.user.UserDO;
import nus.edu.u.system.domain.vo.task.TaskRespVO;

/** Builder for {@link TaskRespVO} aggregating event and assignee details. */
public final class TaskRespVOBuilder {

    private final TaskRespVO response;
    private final TaskDO task;

    private EventDO event;
    private Supplier<EventDO> eventSupplier = () -> null;
    private Function<EventDO, TaskRespVO.EventSummaryVO> eventMapper = this::toEventSummary;

    private UserDO assignee;
    private Supplier<UserDO> assigneeSupplier = () -> null;
    private Function<UserDO, List<TaskRespVO.AssignedUserVO.GroupVO>> groupResolver =
            user -> Collections.emptyList();

    private TaskRespVOBuilder(TaskDO task) {
        this.task = task;
        this.response = TaskConvert.INSTANCE.toRespVO(task);
    }

    public static TaskRespVOBuilder from(TaskDO task) {
        return new TaskRespVOBuilder(task);
    }

    public TaskRespVOBuilder withEvent(EventDO event) {
        this.event = event;
        return this;
    }

    public TaskRespVOBuilder withEventSupplier(Supplier<EventDO> supplier) {
        if (supplier != null) {
            this.eventSupplier = supplier;
        }
        return this;
    }

    public TaskRespVOBuilder withEventMapper(Function<EventDO, TaskRespVO.EventSummaryVO> mapper) {
        if (mapper != null) {
            this.eventMapper = mapper;
        }
        return this;
    }

    public TaskRespVOBuilder withAssignee(UserDO assignee) {
        this.assignee = assignee;
        return this;
    }

    public TaskRespVOBuilder withAssigneeSupplier(Supplier<UserDO> supplier) {
        if (supplier != null) {
            this.assigneeSupplier = supplier;
        }
        return this;
    }

    public TaskRespVOBuilder withGroupResolver(
            Function<UserDO, List<TaskRespVO.AssignedUserVO.GroupVO>> resolver) {
        if (resolver != null) {
            this.groupResolver = resolver;
        }
        return this;
    }

    public TaskRespVO build() {
        EventDO eventData = event != null ? event : eventSupplier.get();
        response.setEvent(eventMapper.apply(eventData));

        UserDO userData = assignee != null ? assignee : assigneeSupplier.get();
        if (userData != null) {
            response.setAssignedUser(toAssignedUser(userData));
        } else {
            response.setAssignedUser(null);
        }
        return response;
    }

    private TaskRespVO.EventSummaryVO toEventSummary(EventDO event) {
        if (event == null) {
            return null;
        }
        TaskRespVO.EventSummaryVO eventVO = new TaskRespVO.EventSummaryVO();
        eventVO.setId(event.getId());
        eventVO.setName(event.getName());
        eventVO.setDescription(event.getDescription());
        eventVO.setOrganizerId(event.getUserId());
        eventVO.setLocation(event.getLocation());
        eventVO.setStatus(event.getStatus());
        eventVO.setStartTime(event.getStartTime());
        eventVO.setEndTime(event.getEndTime());
        eventVO.setRemark(event.getRemark());
        return eventVO;
    }

    private TaskRespVO.AssignedUserVO toAssignedUser(UserDO user) {
        TaskRespVO.AssignedUserVO assignedUserVO = new TaskRespVO.AssignedUserVO();
        assignedUserVO.setId(user.getId());
        assignedUserVO.setName(user.getUsername());
        assignedUserVO.setEmail(user.getEmail());
        assignedUserVO.setPhone(user.getPhone());
        List<TaskRespVO.AssignedUserVO.GroupVO> groups = groupResolver.apply(user);
        assignedUserVO.setGroups(groups != null ? groups : Collections.emptyList());
        return assignedUserVO;
    }
}
