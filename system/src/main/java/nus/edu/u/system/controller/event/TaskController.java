package nus.edu.u.system.controller.event;

import static nus.edu.u.common.constant.PermissionConstants.*;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.common.core.domain.CommonResult;
import nus.edu.u.system.domain.vo.task.*;
import nus.edu.u.system.service.task.TaskLogService;
import nus.edu.u.system.service.task.TaskService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author Lu Shuwen
 * @date 2025-10-03
 */
@RestController
@RequestMapping("/system/task")
@Validated
@Slf4j
public class TaskController {

    @Resource private TaskService taskService;

    @Resource private TaskLogService taskLogService;

    @SaCheckPermission(CREATE_TASK)
    @PostMapping("/{eventId}")
    public CommonResult<TaskRespVO> createTask(
            @PathVariable("eventId") @NotNull Long eventId,
            @Valid @RequestBody TaskCreateReqVO reqVO) {
        TaskRespVO resp = taskService.createTask(eventId, reqVO);
        CommonResult<TaskRespVO> result = CommonResult.success(resp);
        result.setMsg("task created successfully");
        return result;
    }

    @GetMapping("/{eventId}")
    public CommonResult<List<TaskRespVO>> listTasksByEvent(
            @PathVariable("eventId") @NotNull Long eventId) {
        List<TaskRespVO> resp = taskService.listTasksByEvent(eventId);
        CommonResult<List<TaskRespVO>> result = CommonResult.success(resp);
        result.setMsg("Tasks retrieved successfully");
        return result;
    }

    @GetMapping("/dashboard")
    public CommonResult<TaskDashboardRespVO> getByMemberId() {
        Long memberId = StpUtil.getLoginIdAsLong();
        TaskDashboardRespVO resp = taskService.getByMemberId(memberId);
        return CommonResult.success(resp);
    }

    @GetMapping("/{eventId}/{taskId}")
    public CommonResult<TaskRespVO> getTask(
            @PathVariable("eventId") @NotNull Long eventId,
            @PathVariable("taskId") @NotNull Long taskId) {
        TaskRespVO resp = taskService.getTask(eventId, taskId);
        CommonResult<TaskRespVO> result = CommonResult.success(resp);
        result.setMsg("task retrieved successfully");
        return result;
    }

    @SaCheckPermission(UPDATE_TASK)
    @PatchMapping("/{eventId}/{taskId}")
    public CommonResult<TaskRespVO> updateTask(
            @PathVariable("eventId") @NotNull Long eventId,
            @PathVariable("taskId") @NotNull Long taskId,
            @Valid @RequestBody TaskUpdateReqVO reqVO) {
        TaskRespVO resp = taskService.updateTask(eventId, taskId, reqVO, reqVO.getType());
        CommonResult<TaskRespVO> result = CommonResult.success(resp);
        result.setMsg("task updated successfully");
        return result;
    }

    @SaCheckPermission(DELETE_TASK)
    @DeleteMapping("/{eventId}/{taskId}")
    public CommonResult<Void> deleteTask(
            @PathVariable("eventId") @NotNull Long eventId,
            @PathVariable("taskId") @NotNull Long taskId) {
        taskService.deleteTask(eventId, taskId);
        CommonResult<Void> result = CommonResult.success(null);
        result.setMsg("task deleted successfully");
        return result;
    }

    @GetMapping("/{eventId}/log/{taskId}")
    public CommonResult<List<TaskLogRespVO>> getLog(@PathVariable("taskId") @NotNull Long taskId) {
        return CommonResult.success(taskLogService.getTaskLog(taskId));
    }
}
