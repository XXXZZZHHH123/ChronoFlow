package nus.edu.u.system.service.task;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import nus.edu.u.system.domain.dataobject.task.TaskLogDO;
import nus.edu.u.system.domain.dataobject.user.UserDO;
import nus.edu.u.system.domain.vo.auth.UserVO;
import nus.edu.u.system.domain.vo.task.TaskLogRespVO;
import nus.edu.u.system.mapper.task.TaskLogMapper;
import nus.edu.u.system.mapper.user.UserMapper;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Lu Shuwen
 * @date 2025-10-02
 */
@Service
public class TaskLogServiceImpl implements TaskLogService {

    @Resource private TaskLogMapper taskLogMapper;

    @Resource private UserMapper userMapper;

    @Override
    public boolean insertTaskLog(Long taskId, Long targetUserId, Integer action) {
        TaskLogDO taskLogDO =
                TaskLogDO.builder()
                        .taskId(taskId)
                        .targetUserId(targetUserId)
                        .action(action)
                        .build();
        return taskLogMapper.insert(taskLogDO) > 0;
    }

    @Override
    public List<TaskLogRespVO> getTaskLog(Long taskId) {
        List<TaskLogDO> taskLogList = taskLogMapper.selectList(
                new LambdaQueryWrapper<TaskLogDO>().eq(TaskLogDO::getTaskId, taskId)
        );
        if (taskLogList == null || taskLogList.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> userIdList = taskLogList.stream().map(TaskLogDO::getTargetUserId).collect(Collectors.toList());
        userIdList.addAll(taskLogList.stream().map(taskLog -> NumberUtil.parseLong(taskLog.getCreator())).toList());
        List<UserDO> userList = userMapper.selectBatchIds(userIdList);
        Map<Long, UserDO> userMap = userList.stream().collect(Collectors.toMap(UserDO::getId, user -> user));
        return taskLogList.stream()
                .map(taskLog -> {
                    UserDO targetUser = userMap.get(taskLog.getTargetUserId());
                    UserVO targetUserVO = null;
                    if (ObjectUtil.isNotNull(targetUser)) {
                        targetUserVO = new UserVO();
                        targetUserVO.setId(targetUser.getId());
                        targetUserVO.setName(targetUser.getUsername());
                        targetUserVO.setEmail(targetUser.getEmail());
                    }
                    UserDO sourceUser = userMap.get(NumberUtil.parseLong(taskLog.getCreator()));
                    UserVO sourceUserVO = new UserVO();
                    if (ObjectUtil.isNotNull(sourceUser)) {
                        sourceUserVO.setId(sourceUser.getId());
                        sourceUserVO.setName(sourceUser.getUsername());
                        sourceUserVO.setEmail(sourceUser.getEmail());
                    }
                    return TaskLogRespVO.builder()
                            .id(taskLog.getId())
                            .action(taskLog.getAction())
                            .createTime(taskLog.getCreateTime())
                            .targetUser(targetUserVO)
                            .sourceUser(sourceUserVO)
                            .build();
                })
                .toList();
    }
}
