package nus.edu.u.system.service.task.action;

import nus.edu.u.system.domain.dataobject.task.TaskDO;
import nus.edu.u.system.domain.dto.TaskActionDTO;
import nus.edu.u.system.enums.task.TaskActionEnum;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * task action strategy class
 *
 * @author Lu Shuwen
 * @date 2025-10-02
 */
public interface TaskStrategy {

    void execute(TaskDO task, TaskActionDTO taskActionDTO, Object... params);

    TaskActionEnum getType();
}
