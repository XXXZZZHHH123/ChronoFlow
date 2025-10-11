package nus.edu.u.system.service.group;

import static nus.edu.u.common.utils.exception.ServiceExceptionUtil.exception;
import static nus.edu.u.system.enums.ErrorCodeConstants.*;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.common.enums.CommonStatusEnum;
import nus.edu.u.system.domain.dataobject.dept.DeptDO;
import nus.edu.u.system.domain.dataobject.task.EventDO;
import nus.edu.u.system.domain.dataobject.task.TaskDO;
import nus.edu.u.system.domain.dataobject.user.UserDO;
import nus.edu.u.system.domain.dataobject.user.UserGroupDO;
import nus.edu.u.system.domain.vo.group.CreateGroupReqVO;
import nus.edu.u.system.domain.vo.group.GroupRespVO;
import nus.edu.u.system.domain.vo.group.UpdateGroupReqVO;
import nus.edu.u.system.domain.vo.user.UserProfileRespVO;
import nus.edu.u.system.enums.task.TaskStatusEnum;
import nus.edu.u.system.mapper.dept.DeptMapper;
import nus.edu.u.system.mapper.task.EventMapper;
import nus.edu.u.system.mapper.task.TaskMapper;
import nus.edu.u.system.mapper.user.UserGroupMapper;
import nus.edu.u.system.mapper.user.UserMapper;
import nus.edu.u.system.service.user.UserService;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Group service implementation Handles all group-related business logic for event management
 *
 * @author Fan yazhuoting
 * @date 2025-09-09
 */
@Service
@Slf4j
public class GroupServiceImpl implements GroupService {
    @Resource private DeptMapper deptMapper;

    @Resource private UserMapper userMapper;

    @Resource private EventMapper eventMapper;

    @Resource private UserService userService;

    @Resource private UserGroupMapper userGroupMapper;

    @Resource private TaskMapper taskMapper;

    @Override
    @Transactional
    public Long createGroup(CreateGroupReqVO reqVO) {
        // 1. Validate event exists
        EventDO event = eventMapper.selectById(reqVO.getEventId());
        if (ObjectUtil.isNull(event)) {
            throw exception(EVENT_NOT_FOUND);
        }

        // 2. Check if group name already exists in this event
        LambdaQueryWrapper<DeptDO> queryWrapper =
                new LambdaQueryWrapper<DeptDO>()
                        .eq(DeptDO::getName, reqVO.getName())
                        .eq(DeptDO::getTenantId, event.getTenantId())
                        .eq(DeptDO::getStatus, CommonStatusEnum.ENABLE.getStatus());

        DeptDO existingGroup = deptMapper.selectOne(queryWrapper);
        if (ObjectUtil.isNotNull(existingGroup)) {
            throw exception(GROUP_NAME_EXISTS);
        }

        // 3. Validate lead user if provided
        if (ObjectUtil.isNotNull(reqVO.getLeadUserId())) {
            UserDO leadUser = userMapper.selectById(reqVO.getLeadUserId());
            if (ObjectUtil.isNull(leadUser)) {
                throw exception(USER_NOT_FOUND);
            }
        }

        // 4. Create group
        DeptDO deptDO =
                DeptDO.builder()
                        .name(reqVO.getName())
                        .sort(reqVO.getSort())
                        .leadUserId(reqVO.getLeadUserId())
                        .eventId(reqVO.getEventId())
                        .remark(reqVO.getRemark())
                        .status(CommonStatusEnum.ENABLE.getStatus())
                        .build();

        //        deptMapper.insert(deptDO);
        deptMapper.insert(deptDO);
        Long groupId = deptDO.getId();

        if (groupId == null) {
            log.error("Failed to get group ID after insert");
            throw exception(GET_GROUP_ID_FAILED);
        }

        // 5. Automatically add leader as a member if leadUserId is provided
        if (ObjectUtil.isNotNull(reqVO.getLeadUserId())) {
            addMemberToGroup(deptDO.getId(), reqVO.getLeadUserId());
        }

        log.info("Created group: {} with ID: {}", reqVO.getName(), deptDO.getId());
        return deptDO.getId();
    }

    @Override
    @Transactional
    public void updateGroup(UpdateGroupReqVO reqVO) {
        // 1. Check if group exists
        DeptDO existingGroup = deptMapper.selectById(reqVO.getId());
        if (ObjectUtil.isNull(existingGroup)) {
            throw exception(GROUP_NOT_FOUND);
        }

        // 2. Validate lead user if provided
        if (ObjectUtil.isNotNull(reqVO.getLeadUserId())) {
            UserDO leadUser = userMapper.selectById(reqVO.getLeadUserId());
            if (ObjectUtil.isNull(leadUser)) {
                throw exception(USER_NOT_FOUND);
            }
        }

        // 3. Update group
        DeptDO updateDept = new DeptDO();
        BeanUtil.copyProperties(reqVO, updateDept);

        deptMapper.updateById(updateDept);
        log.info("Updated group ID: {}", reqVO.getId());
    }

    @Override
    @Transactional
    public void deleteGroup(Long id) {
        DeptDO existingGroup = deptMapper.selectById(id);
        if (ObjectUtil.isNull(existingGroup)) {
            throw exception(GROUP_NOT_FOUND);
        }

        List<GroupRespVO.MemberInfo> groupMembers = getGroupMembers(id);

        if (!ObjectUtil.isEmpty(groupMembers)) {
            Long leaderId = existingGroup.getLeadUserId();
            List<Long> normalMemberIds =
                    groupMembers.stream()
                            .map(GroupRespVO.MemberInfo::getUserId)
                            .filter(userId -> !ObjectUtil.equal(userId, leaderId))
                            .collect(Collectors.toList());

            if (!ObjectUtil.isEmpty(normalMemberIds)) {
                GroupService proxy = (GroupService) AopContext.currentProxy();
                proxy.removeMembersToGroup(id, normalMemberIds);
            }

            if (ObjectUtil.isNotNull(leaderId)) {
                // Update user to remove them from the group
                UpdateWrapper<UserDO> userUpdateWrapper = new UpdateWrapper<>();
                userUpdateWrapper
                        .set("dept_id", null) // Use actual database column name
                        .eq("id", leaderId);

                userMapper.update(null, userUpdateWrapper);
                log.info("Removed leader {} from group {}", leaderId, id);
            }
        }

        // Soft delete by updating status
        UpdateWrapper<DeptDO> deptUpdateWrapper = new UpdateWrapper<>();
        deptUpdateWrapper.set("status", CommonStatusEnum.DISABLE.getStatus()).eq("id", id);

        deptMapper.update(null, deptUpdateWrapper);
        log.info("Soft deleted group ID: {}", id);
    }

    @Override
    @Transactional
    public void addMemberToGroup(Long groupId, Long userId) {
        // 1. verify if Group exist
        DeptDO group = deptMapper.selectById(groupId);
        if (ObjectUtil.isNull(group)) {
            throw exception(GROUP_NOT_FOUND);
        }

        // 2. verify if user exist
        UserDO user = userMapper.selectById(userId);
        if (ObjectUtil.isNull(user)) {
            throw exception(USER_NOT_FOUND);
        }

        // 3. verify user status
        if (!CommonStatusEnum.isEnable(user.getStatus())) {
            throw exception(USER_STATUS_INVALID);
        }

        // 4. check if this user already in other group in this Event
        Long eventId = group.getEventId();
        LambdaQueryWrapper<UserGroupDO> checkWrapper =
                new LambdaQueryWrapper<UserGroupDO>()
                        .eq(UserGroupDO::getUserId, userId)
                        .eq(UserGroupDO::getEventId, eventId);

        UserGroupDO existingRelation = userGroupMapper.selectOne(checkWrapper);

        if (ObjectUtil.isNotNull(existingRelation)) {
            // if already in other group, throw exception
            if (!existingRelation.getDeptId().equals(groupId)) {
                throw exception(USER_ALREADY_IN_OTHER_GROUP_OF_EVENT);
            }
            // if already in current Group，throw exception
            throw exception(GROUP_MEMBER_ALREADY_EXISTS);
        }

        // 5. create user-group relation
        UserGroupDO userGroup =
                UserGroupDO.builder()
                        .userId(userId)
                        .deptId(groupId)
                        .eventId(eventId)
                        .joinTime(LocalDateTime.now())
                        .build();

        userGroupMapper.insert(userGroup);
        log.info("Added user {} to group {} in event {}", userId, groupId, eventId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void removeMemberFromGroup(Long groupId, Long userId) {
        log.info("Attempting to remove user {} from group {}", userId, groupId);

        LambdaQueryWrapper<UserGroupDO> queryWrapper =
                new LambdaQueryWrapper<UserGroupDO>()
                        .eq(UserGroupDO::getUserId, userId)
                        .eq(UserGroupDO::getDeptId, groupId);

        UserGroupDO userGroup = userGroupMapper.selectOne(queryWrapper);
        if (ObjectUtil.isNull(userGroup)) {
            log.warn("User {} is not in group {}", userId, groupId);
            throw exception(USER_NOT_IN_GROUP);
        }

        log.info(
                "Found user-group relation: id={}, joinTime={}",
                userGroup.getId(),
                userGroup.getJoinTime());

        DeptDO group = deptMapper.selectById(groupId);
        log.info(
                "Group lead_user_id: {}, trying to remove user: {}", group.getLeadUserId(), userId);

        if (ObjectUtil.isNotNull(group) && ObjectUtil.equal(group.getLeadUserId(), userId)) {
            log.warn("Cannot remove group leader {} from group {}", userId, groupId);
            throw exception(CANNOT_REMOVE_GROUP_LEADER);
        }

        Long eventId = userGroup.getEventId();
        LambdaQueryWrapper<TaskDO> taskQueryWrapper =
                new LambdaQueryWrapper<TaskDO>()
                        .eq(TaskDO::getUserId, userId)
                        .eq(TaskDO::getEventId, eventId)
                        .ne(TaskDO::getStatus, TaskStatusEnum.COMPLETED.getStatus());

        long pendingTaskCount = taskMapper.selectCount(taskQueryWrapper);

        if (pendingTaskCount > 0) {
            log.warn("User {} has {} pending tasks in event {}", userId, pendingTaskCount, eventId);
            throw exception(CANNOT_REMOVE_MEMBER_WITH_PENDING_TASKS);
        }

        int deletedRows = userGroupMapper.deleteById(userGroup.getId());
        log.info("Removed user {} from group {}, affected rows: {}", userId, groupId, deletedRows);
    }

    @Override
    public List<GroupRespVO.MemberInfo> getGroupMembers(Long groupId) {
        // 通过关联表查询
        LambdaQueryWrapper<UserGroupDO> queryWrapper =
                new LambdaQueryWrapper<UserGroupDO>().eq(UserGroupDO::getDeptId, groupId);

        List<UserGroupDO> userGroups = userGroupMapper.selectList(queryWrapper);

        if (ObjectUtil.isEmpty(userGroups)) {
            return Collections.emptyList();
        }

        List<Long> userIds =
                userGroups.stream().map(UserGroupDO::getUserId).collect(Collectors.toList());

        // 批量查询用户信息
        List<UserDO> users = userMapper.selectBatchIds(userIds);

        // 只返回启用状态的用户
        return users.stream()
                .filter(user -> CommonStatusEnum.isEnable(user.getStatus()))
                .map(
                        user -> {
                            UserGroupDO userGroup =
                                    userGroups.stream()
                                            .filter(ug -> ug.getUserId().equals(user.getId()))
                                            .findFirst()
                                            .orElse(null);

                            return GroupRespVO.MemberInfo.builder()
                                    .userId(user.getId())
                                    .username(user.getUsername())
                                    .email(user.getEmail())
                                    .phone(user.getPhone())
                                    .joinTime(userGroup != null ? userGroup.getJoinTime() : null)
                                    .build();
                        })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void addMembersToGroup(Long groupId, List<Long> userIds) {
        if (ObjectUtil.isEmpty(userIds)) {
            return;
        }

        DeptDO group = deptMapper.selectById(groupId);
        if (ObjectUtil.isNull(group)) {
            throw exception(GROUP_NOT_FOUND);
        }

        List<Long> successIds = new ArrayList<>();
        List<Long> failedIds = new ArrayList<>();

        for (Long userId : userIds) {
            try {
                addMemberToGroup(groupId, userId);
                successIds.add(userId);
            } catch (Exception e) {
                failedIds.add(userId);
            }
        }

        log.info(
                "success add members to group: groupId={}, success={}, failed={}",
                groupId,
                successIds.size(),
                failedIds.size());

        if (!failedIds.isEmpty()) {
            log.warn("failed to add members to group: failedUserIds={}", failedIds);
            throw exception(ADD_MEMBERS_FAILED);
        }
    }

    @Override
    @Transactional
    public void removeMembersToGroup(Long groupId, List<Long> userIds) {
        if (ObjectUtil.isEmpty(userIds)) {
            return;
        }

        DeptDO group = deptMapper.selectById(groupId);
        if (ObjectUtil.isNull(group)) {
            throw exception(GROUP_NOT_FOUND);
        }

        List<Long> successIds = new ArrayList<>();
        List<Long> failedIds = new ArrayList<>();
        Exception firstException = null;

        GroupService proxy = (GroupService) AopContext.currentProxy();

        for (Long userId : userIds) {
            try {
                proxy.removeMemberFromGroup(groupId, userId);
                successIds.add(userId);
            } catch (Exception e) {
                log.error(
                        "Failed to remove user {} from group {}: {}",
                        userId,
                        groupId,
                        e.getMessage(),
                        e);
                failedIds.add(userId);
                if (firstException == null) {
                    firstException = e;
                }
            }
        }

        log.info(
                "success to remove members from group: groupId={}, success={}, failed={}",
                groupId,
                successIds.size(),
                failedIds.size());

        if (!failedIds.isEmpty()) {
            log.warn("failed to remove members from group: failedUserIds={}", failedIds);
            throw firstException instanceof RuntimeException
                    ? (RuntimeException) firstException
                    : new RuntimeException(firstException);
        }
    }

    @Override
    public List<GroupRespVO> getGroupsByEvent(Long eventId) {
        log.info("Service: Getting groups for event ID: {}", eventId);

        EventDO event = eventMapper.selectById(eventId);
        if (ObjectUtil.isNull(event)) {
            log.error("Event not found: {}", eventId);
            throw exception(EVENT_NOT_FOUND);
        }

        LambdaQueryWrapper<DeptDO> queryWrapper =
                new LambdaQueryWrapper<DeptDO>()
                        .eq(DeptDO::getEventId, eventId)
                        .eq(DeptDO::getDeleted, false)
                        .orderByAsc(DeptDO::getSort)
                        .orderByDesc(DeptDO::getCreateTime);

        List<DeptDO> deptList = deptMapper.selectList(queryWrapper);

        if (ObjectUtil.isEmpty(deptList)) {
            log.info("No groups found for event ID: {}", eventId);
            return Collections.emptyList();
        }

        log.info("Found {} groups for event ID: {}", deptList.size(), eventId);

        return deptList.stream()
                .map(
                        dept -> {
                            // 修改：使用 UserGroupDO 表统计成员数量
                            int memberCount =
                                    Math.toIntExact(
                                            userGroupMapper.selectCount(
                                                    new LambdaQueryWrapper<UserGroupDO>()
                                                            .eq(
                                                                    UserGroupDO::getDeptId,
                                                                    dept.getId())));

                            String leadUserName = null;
                            if (ObjectUtil.isNotNull(dept.getLeadUserId())) {
                                UserDO leader = userMapper.selectById(dept.getLeadUserId());
                                if (ObjectUtil.isNotNull(leader)) {
                                    leadUserName = leader.getUsername();
                                }
                            }

                            return GroupRespVO.builder()
                                    .id(dept.getId())
                                    .name(dept.getName())
                                    .sort(dept.getSort())
                                    .leadUserId(dept.getLeadUserId())
                                    .leadUserName(leadUserName)
                                    .remark(dept.getRemark())
                                    .status(dept.getStatus())
                                    .statusName(dept.getStatus() == 0 ? "Active" : "Inactive")
                                    .eventId(dept.getEventId())
                                    .eventName(event.getName())
                                    .memberCount(memberCount)
                                    .createTime(dept.getCreateTime())
                                    .updateTime(dept.getUpdateTime())
                                    .build();
                        })
                .collect(Collectors.toList());
    }

    @Override
    public List<UserProfileRespVO> getAllUserProfiles() {
        return userService.getEnabledUserProfiles();
    }
}
