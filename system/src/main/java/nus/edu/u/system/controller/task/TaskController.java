package nus.edu.u.system.controller.task;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import nus.edu.u.common.core.domain.CommonResult;
import nus.edu.u.system.domain.vo.task.TaskCreateReqVO;
import nus.edu.u.system.domain.vo.task.TaskRespVO;
import nus.edu.u.system.service.task.TaskService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system/event/{eventId}/tasks")
@Validated
public class TaskController {

    @Resource private TaskService taskService;

    @PostMapping
    public CommonResult<TaskRespVO> createTask(
            @PathVariable("eventId") @NotNull Long eventId,
            @Valid @RequestBody TaskCreateReqVO reqVO) {
        TaskRespVO resp = taskService.createTask(eventId, reqVO);
        CommonResult<TaskRespVO> result = CommonResult.success(resp);
        result.setMsg("Task created successfully");
        return result;
    }
}
