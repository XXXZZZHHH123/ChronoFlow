package nus.edu.u.system.controller;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.common.core.domain.CommonResult;
import nus.edu.u.system.domain.vo.event.EventCreateReqVO;
import nus.edu.u.system.domain.vo.event.EventRespVO;
import nus.edu.u.system.domain.vo.event.EventUpdateReqVO;
import nus.edu.u.system.domain.vo.event.UpdateEventRespVO;
import nus.edu.u.system.service.event.EventService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/system/event")
@Validated
@Slf4j
public class EventController {
    @Resource private EventService eventService;

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

    @PatchMapping("/{id}")
    public CommonResult<UpdateEventRespVO> updateEvent(
            @PathVariable("id") Long id, @Valid @RequestBody EventUpdateReqVO req) {
        UpdateEventRespVO respVO = eventService.updateEvent(id, req);
        return CommonResult.success(respVO);
    }

    @DeleteMapping("/{id}")
    public CommonResult<Boolean> deleteEvent(@PathVariable("id") Long id) {
        return CommonResult.success(eventService.deleteEvent(id));
    }

    @PatchMapping("/restore/{id}")
    public CommonResult<Boolean> restoreEvent(@PathVariable("id") Long id) {
        return CommonResult.success(eventService.restoreEvent(id));
    }
}
