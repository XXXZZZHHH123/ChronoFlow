package nus.edu.u.system.service.group;

import nus.edu.u.system.domain.vo.group.CreateGroupReqVO;

/**
 * Group service interface
 * Handles all group-related business logic for event management
 *
 * @author Fan yazhuoting
 * @date 2025-09-09
 */
public interface GroupService {
    /**
     * Create a new group for event organization
     *
     * @param reqVO Group creation request
     * @return Created group ID
     */
    Long createGroup(CreateGroupReqVO reqVO);
}
