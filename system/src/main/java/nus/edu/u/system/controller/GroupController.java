package nus.edu.u.system.controller;

import static nus.edu.u.common.core.domain.CommonResult.success;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import nus.edu.u.common.core.domain.CommonResult;
import nus.edu.u.system.domain.vo.group.AddMembersReqVO;
import nus.edu.u.system.domain.vo.group.CreateGroupReqVO;
import nus.edu.u.system.domain.vo.group.GroupRespVO;
import nus.edu.u.system.domain.vo.group.UpdateGroupReqVO;
import nus.edu.u.system.domain.vo.user.UserProfileRespVO;
import nus.edu.u.system.service.group.GroupService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Group management controller Provides REST APIs for Event Owners to manage groups/departments
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

    /**
     * Update an existing group
     *
     * @param reqVO Group update request
     * @return Success indicator
     */
    @PutMapping("/update")
    public CommonResult<Boolean> updateGroup(@RequestBody @Valid UpdateGroupReqVO reqVO) {
        log.info("Updating group ID: {}", reqVO.getId());
        groupService.updateGroup(reqVO);
        return success(true);
    }

    /**
     * Delete a group (soft delete by changing status)
     *
     * @param id Group ID
     * @return Success indicator
     */
    @DeleteMapping("/delete/{id}")
    public CommonResult<Boolean> deleteGroup(@PathVariable("id") Long id) {
        log.info("Deleting group ID: {}", id);
        groupService.deleteGroup(id);
        return success(true);
    }

    /**
     * Add member to group
     *
     * @param groupId Group ID
     * @param userId  User ID
     * @return Success indicator
     */
    @PostMapping("/{groupId}/members/{userId}")
    public CommonResult<Boolean> addMember(@PathVariable("groupId") Long groupId, @PathVariable("userId") Long userId) {
        log.info("Adding user {} to group {}", userId, groupId);
        groupService.addMemberToGroup(groupId, userId);
        return success(true);
    }

    /**
     * Remove member from group
     *
     * @param groupId Group ID
     * @param userId  User ID
     * @return Success indicator
     */
    @DeleteMapping("/{groupId}/members/{userId}")
    public CommonResult<Boolean> removeMember(@PathVariable("groupId") Long groupId, @PathVariable("userId") Long userId) {
        log.info("Removing user {} from group {}", userId, groupId);
        groupService.removeMemberFromGroup(groupId, userId);
        return success(true);
    }

    /**
     * Get all members in a group
     *
     * @param groupId Group ID
     * @return List of group members
     */
    @GetMapping("/{groupId}/members")
    public CommonResult<List<GroupRespVO.MemberInfo>> getGroupMembers(@PathVariable("groupId") Long groupId) {
        List<GroupRespVO.MemberInfo> members = groupService.getGroupMembers(groupId);
        return success(members);
    }

    /**
     * Add multiple members to a group
     *
     * @param groupId Group ID
     * @param reqVO   List of user IDs
     * @return Success indicator
     */
    @PostMapping("{groupId}/members/batch")
    public CommonResult<Boolean> addMembers(@PathVariable("groupId") Long groupId, @RequestBody @Valid AddMembersReqVO reqVO) {
        log.info("Adding users {} to group {}", reqVO.getUserIds(), groupId);
        groupService.addMembersToGroup(groupId, reqVO.getUserIds());
        return success(true);
    }

    /**
     * Remove multiple members from a group
     *
     * @param groupId Group ID
     * @param userIds List of user IDs
     * @return Success indicator
     */
    @DeleteMapping("{groupId}/members/batch")
    public CommonResult<Boolean> deleteMembers(@PathVariable("groupId") Long groupId, @RequestBody List<Long> userIds) {
        log.info("Deleting users {} from group {}", userIds, groupId);
        groupService.removeMembersToGroup(groupId, userIds);
        return success(true);
    }

    /**
     * Get all groups by event ID
     *
     * @param eventId Event ID
     * @return List of groups
     */
    @GetMapping("/list")
    public CommonResult<List<GroupRespVO>> getGroupsByEvent(@RequestParam("eventId") Long eventId) {
        log.info("Getting groups for event ID: {}", eventId);
        List<GroupRespVO> groups = groupService.getGroupsByEvent(eventId);
        return success(groups);
    }

    @GetMapping("/users")
    public CommonResult<List<UserProfileRespVO>> getAllUserProfiles() {
        return CommonResult.success(groupService.getAllUserProfiles());
    }


}
