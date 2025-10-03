package nus.edu.u.system.controller.user;

import static nus.edu.u.common.core.domain.CommonResult.success;
import static nus.edu.u.common.utils.exception.ServiceExceptionUtil.exception;
import static nus.edu.u.system.enums.ErrorCodeConstants.USER_NOTFOUND;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.common.core.domain.CommonResult;
import nus.edu.u.system.domain.dto.RoleDTO;
import nus.edu.u.system.domain.dto.UserRoleDTO;
import nus.edu.u.system.domain.vo.member.MemberProfileRespVO;
import nus.edu.u.system.service.user.UserService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Member dashboard endpoints.
 */
@RestController
@RequestMapping("/system/member")
@Validated
@Slf4j
public class MemberDashboardController {

    @Resource private UserService userService;

    /**
     * 個人基本資料（含暱稱、角色、聯絡方式等）。
     */
    @GetMapping("/profile")
    public CommonResult<MemberProfileRespVO> getProfile() {
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("Fetching member profile for userId={}", userId);

        UserRoleDTO userRoleDTO = userService.selectUserWithRole(userId);
        if (userRoleDTO == null) {
            throw exception(USER_NOTFOUND);
        }

        MemberProfileRespVO respVO = new MemberProfileRespVO();
        respVO.setId(userRoleDTO.getUserId());
        respVO.setUsername(userRoleDTO.getUsername());
        respVO.setEmail(userRoleDTO.getEmail());
        respVO.setPhone(userRoleDTO.getPhone());
        respVO.setStatus(userRoleDTO.getStatus());
        respVO.setTenantId(userRoleDTO.getTenantId());

        List<RoleDTO> roles = userRoleDTO.getRoles();
        if (roles != null && !roles.isEmpty()) {
            respVO.setRoles(
                    roles.stream()
                            .map(
                                    role -> {
                                        MemberProfileRespVO.RoleVO roleVO =
                                                new MemberProfileRespVO.RoleVO();
                                        roleVO.setId(role.getId());
                                        roleVO.setName(role.getName());
                                        roleVO.setKey(role.getRoleKey());
                                        return roleVO;
                                    })
                            .collect(Collectors.toList()));
        } else {
            respVO.setRoles(Collections.emptyList());
        }

        return success(respVO);
    }

    /**
     * 會員所屬群組列表。
     */
    @GetMapping("/groups")
    public CommonResult<List<Map<String, Object>>> listMemberGroups() {
        log.info("Listing groups for the current member");
        // TODO implement member group retrieval.
        return success(Collections.emptyList());
    }

    /**
     * 會員參與的事件（可依狀態或時間範圍過濾）。
     */
    @GetMapping("/events")
    public CommonResult<List<Map<String, Object>>> listMemberEvents(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {
        log.info(
                "Listing events for current member. status={}, startDate={}, endDate={}",
                status,
                startDate,
                endDate);
        // TODO implement event retrieval.
        return success(Collections.emptyList());
    }

    /**
     * 指定事件的詳細資訊（含參與角色、任務摘要等）。
     */
    @GetMapping("/events/{eventId}")
    public CommonResult<Map<String, Object>> getEventDetail(
            @PathVariable("eventId") Long eventId) {
        log.info("Fetching event detail for member. eventId={}", eventId);
        // TODO implement event detail retrieval.
        return success(Collections.emptyMap());
    }

    /**
     * 指定事件底下屬於該會員的任務清單。
     */
    @GetMapping("/events/{eventId}/tasks")
    public CommonResult<List<Map<String, Object>>> listTasksByEvent(
            @PathVariable("eventId") Long eventId) {
        log.info("Listing tasks for member within event. eventId={}", eventId);
        // TODO implement event task retrieval.
        return success(Collections.emptyList());
    }

    /**
     * 會員的任務/待辦列表。
     */
    @GetMapping("/tasks")
    public CommonResult<List<Map<String, Object>>> listMemberTasks(
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "dueBefore", required = false) String dueBefore,
            @RequestParam(value = "dueAfter", required = false) String dueAfter) {
        log.info(
                "Listing tasks for current member. status={}, dueBefore={}, dueAfter={}",
                status,
                dueBefore,
                dueAfter);
        // TODO implement task retrieval.
        return success(Collections.emptyList());
    }

    /**
     * 任務日曆視圖（按月或自訂範圍）。
     */
    @GetMapping("/tasks/calendar")
    public CommonResult<List<Map<String, Object>>> getTaskCalendar(
            @RequestParam(value = "month", required = false) String month) {
        log.info("Fetching task calendar for current member. month={}", month);
        // TODO implement task calendar retrieval.
        return success(Collections.emptyList());
    }

    /**
     * 參與記錄統計（完成事件/任務等）。
     */
    @GetMapping("/stats")
    public CommonResult<Map<String, Object>> getMemberStats() {
        log.info("Fetching member participation statistics");
        // TODO implement statistics aggregation.
        return success(Collections.emptyMap());
    }
}
