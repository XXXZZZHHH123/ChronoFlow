package nus.edu.u.system.service.task.action;

import static nus.edu.u.common.utils.exception.ServiceExceptionUtil.exception;
import static nus.edu.u.system.enums.ErrorCodeConstants.*;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import nus.edu.u.system.domain.dataobject.task.TaskDO;
import nus.edu.u.system.domain.dto.TaskActionDTO;
import nus.edu.u.system.domain.vo.file.FileUploadReqVO;
import nus.edu.u.system.enums.task.TaskActionEnum;
import nus.edu.u.system.enums.task.TaskStatusEnum;
import nus.edu.u.system.mapper.task.TaskMapper;
import nus.edu.u.system.service.file.FileStorageService;
import nus.edu.u.system.service.task.TaskLogService;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author Lu Shuwen
 * @date 2025-10-02
 */
@Component
public abstract class AbstractTaskStrategy implements TaskStrategy {

    @Resource protected TaskMapper taskMapper;

    @Resource protected TaskLogService taskLogService;

    @Resource protected FileStorageService fileStorageService;

    public abstract void execute(TaskDO task, TaskActionDTO taskActionDTO, Object... params);

    public abstract TaskActionEnum getType();

    public void validateTimeRange(
            TaskDO task,
            LocalDateTime taskStart,
            LocalDateTime taskEnd,
            LocalDateTime eventStart,
            LocalDateTime eventEnd) {
        if (taskStart != null && taskEnd != null && !taskStart.isBefore(taskEnd)) {
            throw exception(TASK_TIME_RANGE_INVALID);
        }
        if (taskStart != null && eventStart != null && taskStart.isBefore(eventStart)) {
            throw exception(TASK_TIME_OUTSIDE_EVENT);
        }
        if (taskEnd != null && eventEnd != null && taskEnd.isAfter(eventEnd)) {
            task.setStatus(TaskStatusEnum.DELAYED.getStatus());
            taskMapper.updateById(task);
            throw exception(TASK_TIME_OUTSIDE_EVENT);
        }
    }

    protected void uploadFiles(Long taskLogId, Long eventId, List<MultipartFile> files) {
        if (CollectionUtil.isEmpty(files)) {
            return;
        }
        if (ObjectUtil.isNull(taskLogId) || ObjectUtil.isNull(eventId)) {
            throw exception(TASK_LOG_FILE_FAILED);
        }
        FileUploadReqVO reqVO = new FileUploadReqVO();
        reqVO.setTaskLogId(taskLogId);
        reqVO.setEventId(eventId);
        reqVO.setFiles(files);
        fileStorageService.uploadToTaskLog(reqVO);
    }
}
