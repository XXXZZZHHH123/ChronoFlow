package nus.edu.u.system.service.group;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import nus.edu.u.system.domain.dataobject.dept.DeptDO;
import nus.edu.u.system.domain.vo.group.CreateGroupReqVO;
import nus.edu.u.system.domain.vo.group.GroupRespVO;
import nus.edu.u.system.domain.vo.group.UpdateGroupReqVO;
import nus.edu.u.system.domain.vo.user.UserProfileRespVO;

/**
 * Group service interface Handles all group-related business logic for event management
 *
 * @author Fan yazhuoting
 * @date 2025-09-09
 */
public interface GroupService{
    /**
     * Create a new group for event organization
     *
     * @param reqVO Group creation request
     * @return Created group ID
     */
    Long createGroup(CreateGroupReqVO reqVO);

    void updateGroup(UpdateGroupReqVO reqVO);

    void deleteGroup(Long id);

    void addMemberToGroup(Long groupId, Long userId);

    void removeMemberFromGroup(Long groupId, Long userId);

    List<GroupRespVO.MemberInfo> getGroupMembers(Long groupId);

    void addMembersToGroup(Long groupId, List<Long> userIds);

    void removeMembersToGroup(Long groupId, List<Long> userIds);

    List<GroupRespVO> getGroupsByEvent(Long eventId);

    List<UserProfileRespVO> getAllUserProfiles();
}
