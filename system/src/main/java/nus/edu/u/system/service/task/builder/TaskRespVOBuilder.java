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

/** Builder for {@link TaskRespVO} aggregating event and user details. */
public final class TaskRespVOBuilder {

    private final TaskRespVO response;
    private final TaskDO task;

    private EventDO event;
    private Supplier<EventDO> eventSupplier = () -> null;

    private UserDO assignee;
    private Supplier<UserDO> assigneeSupplier = () -> null;

    private UserDO assigner;
    private Supplier<UserDO> assignerSupplier = () -> null;

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

    public TaskRespVOBuilder withGroupResolver(
            Function<UserDO, List<TaskRespVO.AssignedUserVO.GroupVO>> resolver) {
        if (resolver != null) {
            this.groupResolver = resolver;
        }
        return this;
    }

    public TaskRespVO build() {

        UserDO assigneeData = assignee != null ? assignee : assigneeSupplier.get();
        response.setAssignedUser(toAssignedUser(assigneeData));

        UserDO assignerData = assigner != null ? assigner : assignerSupplier.get();
        response.setAssignerUser(toAssignerUser(assignerData));

        return response;
    }

    private TaskRespVO.AssignedUserVO toAssignedUser(UserDO user) {
        if (user == null) {
            return null;
        }
        TaskRespVO.AssignedUserVO assignedUserVO = new TaskRespVO.AssignedUserVO();
        assignedUserVO.setId(user.getId());
        assignedUserVO.setName(user.getUsername());
        assignedUserVO.setEmail(user.getEmail());
        assignedUserVO.setPhone(user.getPhone());
        List<TaskRespVO.AssignedUserVO.GroupVO> groups =
                groupResolver != null ? groupResolver.apply(user) : Collections.emptyList();
        assignedUserVO.setGroups(groups != null ? groups : Collections.emptyList());
        return assignedUserVO;
    }

    private TaskRespVO.AssignerUserVO toAssignerUser(UserDO user) {
        if (user == null) {
            return null;
        }
        TaskRespVO.AssignerUserVO assignerUserVO = new TaskRespVO.AssignerUserVO();
        assignerUserVO.setId(user.getId());
        assignerUserVO.setName(user.getUsername());
        assignerUserVO.setEmail(user.getEmail());
        assignerUserVO.setPhone(user.getPhone());
        return assignerUserVO;
    }
}
