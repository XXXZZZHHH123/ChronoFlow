package nus.edu.u.system.controller;


import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.common.core.domain.CommonResult;
import nus.edu.u.system.domain.vo.group.CreateGroupReqVO;
import nus.edu.u.system.service.group.GroupService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static nus.edu.u.common.core.domain.CommonResult.success;

/**
 * Group management controller
 * Provides REST APIs for Event Owners to manage groups/departments
 *
 * @author Fan yazhuoting
 * @date 2025-09-09
 */

@RestController
@RequestMapping("/system/group")
@Validated
@Slf4j
public class GroupController {

    @Resource
    private GroupService groupService;

    /**
     * Create a new group
     *
     * @param reqVO Group creation request
     * @return Created group ID
     */
    @PostMapping("/create")
    public CommonResult<Long> createGroup(@RequestBody @Valid CreateGroupReqVO reqVO) {
        log.info("Creating new group: {}", reqVO.getName());
        Long groupId = groupService.createGroup(reqVO);
        return success(groupId);
    }
}
