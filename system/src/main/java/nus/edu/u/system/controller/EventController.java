package nus.edu.u.system.controller;

import static nus.edu.u.common.constant.PermissionConstants.*;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.common.core.domain.CommonResult;
import nus.edu.u.system.domain.vo.event.*;
import nus.edu.u.system.domain.vo.task.TaskCreateReqVO;
import nus.edu.u.system.domain.vo.task.TaskRespVO;
import nus.edu.u.system.domain.vo.task.TaskUpdateReqVO;
import nus.edu.u.system.service.event.EventService;
import nus.edu.u.system.service.task.TaskService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/system/events")
@Validated
@Slf4j
public class EventController {

    @Resource private EventService eventService;

    @Resource private TaskService taskService;

    @SaCheckPermission(CREATE_EVENT)
    @PostMapping()
    public CommonResult<EventRespVO> createEvent(@Valid @RequestBody EventCreateReqVO req) {
        Long organizerId = StpUtil.getLoginIdAsLong();
        req.setOrganizerId(organizerId);
        EventRespVO resp = eventService.createEvent(req);
        return CommonResult.success(resp);
    }

    @GetMapping("/{id}")
    public CommonResult<EventRespVO> getByEventId(@PathVariable("id") @NotNull Long id) {
        return CommonResult.success(eventService.getByEventId(id));
    }

    @GetMapping()
    public CommonResult<List<EventRespVO>> getByOrganizerId() {
        Long organizerId = StpUtil.getLoginIdAsLong();
        return CommonResult.success(eventService.getByOrganizerId(organizerId));
    }

    @SaCheckPermission(CREATE_TASK)
    @PostMapping("/{eventId}/tasks")
    public CommonResult<TaskRespVO> createTask(
            @PathVariable("eventId") @NotNull Long eventId,
            @Valid @RequestBody TaskCreateReqVO reqVO) {
        TaskRespVO resp = taskService.createTask(eventId, reqVO);
        CommonResult<TaskRespVO> result = CommonResult.success(resp);
        result.setMsg("task created successfully");
        return result;
    }

    @GetMapping("/{eventId}/tasks")
    public CommonResult<List<TaskRespVO>> listTasksByEvent(
            @PathVariable("eventId") @NotNull Long eventId) {
        List<TaskRespVO> resp = taskService.listTasksByEvent(eventId);
        CommonResult<List<TaskRespVO>> result = CommonResult.success(resp);
        result.setMsg("Tasks retrieved successfully");
        return result;
    }

    @GetMapping("/{eventId}/tasks/{taskId}")
    public CommonResult<TaskRespVO> getTask(
            @PathVariable("eventId") @NotNull Long eventId,
            @PathVariable("taskId") @NotNull Long taskId) {
        TaskRespVO resp = taskService.getTask(eventId, taskId);
        CommonResult<TaskRespVO> result = CommonResult.success(resp);
        result.setMsg("task retrieved successfully");
        return result;
    }

    @SaCheckPermission(UPDATE_TASK)
    @PatchMapping("/{eventId}/tasks/{taskId}")
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
    @DeleteMapping("/{eventId}/tasks/{taskId}")
    public CommonResult<Void> deleteTask(
            @PathVariable("eventId") @NotNull Long eventId,
            @PathVariable("taskId") @NotNull Long taskId) {
        taskService.deleteTask(eventId, taskId);
        CommonResult<Void> result = CommonResult.success(null);
        result.setMsg("task deleted successfully");
        return result;
    }

    @SaCheckPermission(UPDATE_EVENT)
    @PatchMapping("/{id}")
    public CommonResult<UpdateEventRespVO> updateEvent(
            @PathVariable("id") Long id, @Valid @RequestBody EventUpdateReqVO req) {
        UpdateEventRespVO respVO = eventService.updateEvent(id, req);
        return CommonResult.success(respVO);
    }

    @SaCheckPermission(DELETE_EVENT)
    @DeleteMapping("/{id}")
    public CommonResult<Boolean> deleteEvent(@PathVariable("id") Long id) {
        return CommonResult.success(eventService.deleteEvent(id));
    }

    @SaCheckPermission(UPDATE_EVENT)
    @PatchMapping("/restore/{id}")
    public CommonResult<Boolean> restoreEvent(@PathVariable("id") Long id) {
        return CommonResult.success(eventService.restoreEvent(id));
    }

    @SaCheckPermission(ASSIGN_TASK)
    @GetMapping("/{eventId}/assignable-member")
    public CommonResult<List<EventGroupRespVO>> assignableMember(
            @PathVariable("eventId") Long eventId) {
        return CommonResult.success(eventService.assignableMember(eventId));
    }
}
