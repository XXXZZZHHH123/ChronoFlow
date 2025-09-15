package nus.edu.u.system.service.group;

import static nus.edu.u.common.utils.exception.ServiceExceptionUtil.exception;
import static nus.edu.u.system.enums.ErrorCodeConstants.*;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import nus.edu.u.common.enums.CommonStatusEnum;
import nus.edu.u.system.domain.dataobject.dept.DeptDO;
import nus.edu.u.system.domain.dataobject.task.EventDO;
import nus.edu.u.system.domain.dataobject.user.UserDO;
import nus.edu.u.system.domain.vo.group.CreateGroupReqVO;
import nus.edu.u.system.domain.vo.group.GroupRespVO;
import nus.edu.u.system.domain.vo.group.UpdateGroupReqVO;
import nus.edu.u.system.mapper.dept.DeptMapper;
import nus.edu.u.system.mapper.task.EventMapper;
import nus.edu.u.system.mapper.user.UserMapper;
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

    @Override
    @Transactional
    public Long createGroup(CreateGroupReqVO reqVO) {
        // 1. Validate event exists
        EventDO event = eventMapper.selectById(reqVO.getEventId());
        if (ObjectUtil.isNull(event)) {
            throw exception(GROUP_NOT_FOUND);
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

        // 4. Validate if event is existed
        EventDO eventDO = eventMapper.selectById(reqVO.getEventId());
        if (ObjectUtil.isNull(eventDO)) {
            throw exception(EVENT_NOT_FOUND);
        }

        // 5. Create group
        DeptDO deptDO =
                DeptDO.builder()
                        .name(reqVO.getName())
                        .sort(reqVO.getSort())
                        .leadUserId(reqVO.getLeadUserId())
                        .phone(reqVO.getPhone())
                        .email(reqVO.getEmail())
                        .eventId(reqVO.getEventId())
                        .remark(reqVO.getRemark())
                        .status(CommonStatusEnum.ENABLE.getStatus())
                        .build();

        deptMapper.insert(deptDO);

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
                LambdaUpdateWrapper<UserDO> updateWrapper =
                        new LambdaUpdateWrapper<UserDO>()
                                .eq(UserDO::getId, leaderId)
                                .set(UserDO::getDeptId, null);
                userMapper.update(null, updateWrapper);
                log.info("Removed leader {} from group {}", leaderId, id);
            }
        }

        // Soft delete by updating status
        LambdaUpdateWrapper<DeptDO> updateWrapper =
                new LambdaUpdateWrapper<DeptDO>()
                        .eq(DeptDO::getId, id)
                        .set(DeptDO::getStatus, CommonStatusEnum.DISABLE.getStatus());

        deptMapper.update(null, updateWrapper);
        log.info("Soft deleted group ID: {}", id);
    }

    @Override
    @Transactional
    public void addMemberToGroup(@Param("groupId") Long groupId, @Param("userId") Long userId) {
        // 1. Validate group exists
        DeptDO group = deptMapper.selectById(groupId);
        if (ObjectUtil.isNull(group)) {
            throw exception(GROUP_NOT_FOUND);
        }

        // 2. Validate user exists
        UserDO user = userMapper.selectById(userId);
        if (ObjectUtil.isNull(user)) {
            throw exception(USER_NOT_FOUND);
        }

        // 3. Check if user is already a member of the group
        if (ObjectUtil.equal(user.getDeptId(), groupId)) {
            throw exception(GROUP_MEMBER_ALREADY_EXISTS);
        }

        // 4. Check if user is disabled
        if (!CommonStatusEnum.isEnable(user.getStatus())) {
            throw exception(USER_STATUS_INVALID);
        }

        // 5. Update user's department
        LambdaUpdateWrapper<UserDO> updateWrapper =
                new LambdaUpdateWrapper<UserDO>()
                        .eq(UserDO::getId, userId)
                        .set(UserDO::getDeptId, groupId);

        userMapper.update(null, updateWrapper);
        log.info("Added user {} to group {}", userId, groupId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void removeMemberFromGroup(Long groupId, Long userId) {
        // 1. Validate user exists and is in the group
        UserDO user = userMapper.selectById(userId);
        if (ObjectUtil.isNull(user) || !groupId.equals(user.getDeptId())) {
            throw exception(USER_NOT_FOUND);
        }

        // 2. Check if user is the group leader
        DeptDO group = deptMapper.selectById(groupId);
        if (ObjectUtil.isNotNull(group) && ObjectUtil.equal(group.getLeadUserId(), userId)) {
            throw exception(CANNOT_REMOVE_GROUP_LEADER);
        }

        // 3. Remove user from group by setting deptId to null
        LambdaUpdateWrapper<UserDO> updateWrapper =
                new LambdaUpdateWrapper<UserDO>()
                        .eq(UserDO::getId, userId)
                        .set(UserDO::getDeptId, null);

        userMapper.update(null, updateWrapper);
        log.info("Removed user {} from group {}", userId, groupId);
    }

    @Override
    public List<GroupRespVO.MemberInfo> getGroupMembers(Long groupId) {
        LambdaQueryWrapper<UserDO> queryWrapper =
                new LambdaQueryWrapper<UserDO>()
                        .eq(UserDO::getDeptId, groupId)
                        .eq(UserDO::getStatus, CommonStatusEnum.ENABLE.getStatus());

        List<UserDO> members = userMapper.selectList(queryWrapper);

        return members.stream()
                .map(
                        user ->
                                GroupRespVO.MemberInfo.builder()
                                        .userId(user.getId())
                                        .username(user.getUsername())
                                        .email(user.getEmail())
                                        .phone(user.getPhone())
                                        .build())
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

        GroupService proxy = (GroupService) AopContext.currentProxy();

        for (Long userId : userIds) {
            try {
                proxy.removeMemberFromGroup(groupId, userId);
                successIds.add(userId);
            } catch (Exception e) {
                failedIds.add(userId);
            }
        }

        log.info(
                "success to remove members from group: groupId={}, success={}, failed={}",
                groupId,
                successIds.size(),
                failedIds.size());

        if (!failedIds.isEmpty()) {
            log.warn("failed to remove members from group: failedUserIds={}", failedIds);
        }
    }
}
