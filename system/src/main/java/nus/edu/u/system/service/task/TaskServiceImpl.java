package nus.edu.u.system.service.task;

import static nus.edu.u.common.utils.exception.ServiceExceptionUtil.exception;
import static nus.edu.u.system.enums.ErrorCodeConstants.*;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.system.convert.task.TaskConvert;
import nus.edu.u.system.domain.dataobject.dept.PostDO;
import nus.edu.u.system.domain.dataobject.dept.UserPostDO;
import nus.edu.u.system.domain.dataobject.task.EventDO;
import nus.edu.u.system.domain.dataobject.task.TaskDO;
import nus.edu.u.system.domain.dataobject.user.UserDO;
import nus.edu.u.system.domain.vo.task.TaskCreateReqVO;
import nus.edu.u.system.domain.vo.task.TaskRespVO;
import nus.edu.u.system.enums.task.TaskStatusEnum;
import nus.edu.u.system.mapper.dept.PostMapper;
import nus.edu.u.system.mapper.dept.UserPostMapper;
import nus.edu.u.system.mapper.task.EventMapper;
import nus.edu.u.system.mapper.task.TaskMapper;
import nus.edu.u.system.mapper.user.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class TaskServiceImpl implements TaskService {

    @Resource private TaskMapper taskMapper;
    @Resource private EventMapper eventMapper;
    @Resource private UserMapper userMapper;
    @Resource private UserPostMapper userPostMapper;
    @Resource private PostMapper postMapper;

    @Override
    @Transactional
    public TaskRespVO createTask(Long eventId, TaskCreateReqVO reqVO) {
        EventDO event = eventMapper.selectById(eventId);
        if (event == null) {
            throw exception(EVENT_NOT_FOUND);
        }

        UserDO assignee = userMapper.selectById(reqVO.getAssignedUserId());
        if (assignee == null) {
            throw exception(TASK_ASSIGNEE_NOT_FOUND);
        }

        if (!Objects.equals(event.getTenantId(), assignee.getTenantId())) {
            throw exception(TASK_ASSIGNEE_TENANT_MISMATCH);
        }

        LocalDateTime start = reqVO.getStartTime();
        LocalDateTime end = reqVO.getEndTime();
        validateTimeRange(start, end);

        TaskStatusEnum statusEnum = TaskStatusEnum.fromStatusOrDefault(reqVO.getStatus());
        if (statusEnum == null) {
            throw exception(TASK_STATUS_INVALID);
        }

        TaskDO task = TaskConvert.INSTANCE.convert(reqVO);
        task.setEventId(eventId);
        task.setTenantId(event.getTenantId());
        task.setStatus(statusEnum.getStatus());
        task.setStartTime(start);
        task.setEndTime(end);
        taskMapper.insert(task);

        TaskRespVO resp = TaskConvert.INSTANCE.toRespVO(task);
        TaskRespVO.AssignedUserVO assignedUserVO = new TaskRespVO.AssignedUserVO();
        assignedUserVO.setId(assignee.getId());
        assignedUserVO.setName(assignee.getUsername());
        assignedUserVO.setPositionTitles(resolvePositionTitles(assignee.getId()));
        resp.setAssignedUser(assignedUserVO);
        return resp;
    }

    private LocalDateTime toLocalDateTime(OffsetDateTime value) {
        return value == null ? null : value.toLocalDateTime();
    }

    private void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && !start.isBefore(end)) {
            throw exception(TASK_TIME_RANGE_INVALID);
        }
    }

    private List<String> resolvePositionTitles(Long userId) {
        List<UserPostDO> userPosts =
                userPostMapper.selectList(
                        Wrappers.<UserPostDO>lambdaQuery().eq(UserPostDO::getUserId, userId));
        if (userPosts == null || userPosts.isEmpty()) {
            return List.of();
        }

        List<Long> postIds =
                userPosts.stream()
                        .map(UserPostDO::getPostId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
        if (postIds.isEmpty()) {
            return List.of();
        }

        List<PostDO> posts = postMapper.selectBatchIds(postIds);
        if (posts == null || posts.isEmpty()) {
            return List.of();
        }

        return posts.stream()
                .map(PostDO::getName)
                .filter(StringUtils::hasText)
                .toList();
    }
}
