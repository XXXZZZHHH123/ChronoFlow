package nus.edu.u.system.controller;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.common.core.domain.CommonResult;
import nus.edu.u.system.domain.vo.event.EventCreateReqVO;
import nus.edu.u.system.domain.vo.event.EventRespVO;
import nus.edu.u.system.domain.vo.event.EventUpdateReqVO;
import nus.edu.u.system.domain.vo.event.UpdateEventRespVO;
import nus.edu.u.system.service.event.EventService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/system/event")
@Validated
@Slf4j
public class EventController {
    @Resource
    private EventService eventService;

    @PostMapping("/create")
    public CommonResult<Long> createEvent(@Valid @RequestBody EventCreateReqVO req) {
        Long eventId = eventService.createEvent(req);
        return CommonResult.success(eventId);
    }

    @GetMapping("/{id}")
    public CommonResult<EventRespVO> getByEventId(@PathVariable("id") @NotNull Long id) {
        return CommonResult.success(eventService.getByEventId(id));
    }

    @GetMapping("/organizer/{organizerId}")
    public CommonResult<List<EventRespVO>> getByOrganizerId(
            @PathVariable("organizerId") @NotNull Long organizerId) {
        return CommonResult.success(eventService.getByOrganizerId(organizerId));
    }

    @PatchMapping("/{id}")
    public CommonResult<UpdateEventRespVO> updateEvent(
            @PathVariable("id") Long id,
            @Valid @RequestBody EventUpdateReqVO req) {
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
