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

/** Builder for {@link TaskRespVO} used by task CRUD operations. */
public final class TaskRespVOBuilder {

    private final TaskRespVO response;
    private final TaskDO task;

    private EventDO event;
    private Supplier<EventDO> eventSupplier = () -> null;
    private Function<EventDO, TaskRespVO.EventVO> eventMapper = this::toEvent;

    private UserDO assigner;
    private Supplier<UserDO> assignerSupplier = () -> null;
    private Function<UserDO, List<TaskRespVO.AssignerUserVO.GroupVO>> assignerGroupsResolver =
            user -> Collections.emptyList();

    private UserDO assignee;
    private Supplier<UserDO> assigneeSupplier = () -> null;
    private Function<UserDO, List<TaskRespVO.AssignedUserVO.GroupVO>> assigneeGroupsResolver =
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

    public TaskRespVOBuilder withEventMapper(Function<EventDO, TaskRespVO.EventVO> mapper) {
        if (mapper != null) {
            this.eventMapper = mapper;
        }
        return this;
    }

    public TaskRespVOBuilder withAssigner(UserDO assigner) {
        this.assigner = assigner;
        return this;
    }

    public TaskRespVOBuilder withAssignerSupplier(Supplier<UserDO> supplier) {
        if (supplier != null) {
            this.assignerSupplier = supplier;
        }
        return this;
    }

    public TaskRespVOBuilder withAssignerGroupsResolver(
            Function<UserDO, List<TaskRespVO.AssignerUserVO.GroupVO>> resolver) {
        if (resolver != null) {
            this.assignerGroupsResolver = resolver;
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

    public TaskRespVOBuilder withAssigneeGroupsResolver(
            Function<UserDO, List<TaskRespVO.AssignedUserVO.GroupVO>> resolver) {
        if (resolver != null) {
            this.assigneeGroupsResolver = resolver;
        }
        return this;
    }

    public TaskRespVO build() {
        EventDO eventData = event != null ? event : eventSupplier.get();
        response.setEvent(eventMapper.apply(eventData));

        UserDO assignerData = assigner != null ? assigner : assignerSupplier.get();
        if (assignerData != null) {
            response.setAssignerUser(toAssigner(assignerData));
        }

        UserDO assigneeData = assignee != null ? assignee : assigneeSupplier.get();
        if (assigneeData != null) {
            response.setAssignedUser(toAssignee(assigneeData));
        }

        return response;
    }

    private TaskRespVO.EventVO toEvent(EventDO event) {
        if (event == null) {
            return null;
        }
        TaskRespVO.EventVO eventVO = new TaskRespVO.EventVO();
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

    private TaskRespVO.AssignerUserVO toAssigner(UserDO user) {
        TaskRespVO.AssignerUserVO assignerVO = new TaskRespVO.AssignerUserVO();
        assignerVO.setId(user.getId());
        assignerVO.setName(user.getUsername());
        assignerVO.setEmail(user.getEmail());
        assignerVO.setPhone(user.getPhone());
        List<TaskRespVO.AssignerUserVO.GroupVO> groups = assignerGroupsResolver.apply(user);
        assignerVO.setGroups(groups != null ? groups : Collections.emptyList());
        return assignerVO;
    }

    private TaskRespVO.AssignedUserVO toAssignee(UserDO user) {
        TaskRespVO.AssignedUserVO assignedVO = new TaskRespVO.AssignedUserVO();
        assignedVO.setId(user.getId());
        assignedVO.setName(user.getUsername());
        assignedVO.setEmail(user.getEmail());
        assignedVO.setPhone(user.getPhone());
        List<TaskRespVO.AssignedUserVO.GroupVO> groups = assigneeGroupsResolver.apply(user);
        assignedVO.setGroups(groups != null ? groups : Collections.emptyList());
        return assignedVO;
    }
}
