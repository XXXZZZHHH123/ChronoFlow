package nus.edu.u.system.service.role;

import java.util.List;
import nus.edu.u.system.domain.vo.role.RoleReqVO;
import nus.edu.u.system.domain.vo.role.RoleRespVO;

public interface RoleService {

    /**
     * List all role in an organization
     *
     * @return List of RoleRespVO
     */
    List<RoleRespVO> listRoles();

    /**
     * Create a role with permissions
     * @param roleReqVO
     * @return
     */
    RoleRespVO createRole(RoleReqVO roleReqVO);

    /**
     * Get role by id
     * @param roleId
     * @return
     */
    RoleRespVO getRole(Long roleId);

    /**
     * Delete a role by id
     * @param roleId
     */
    void deleteRole(Long roleId);

    /**
     * Update a role by id
     * @param roleId
     * @param roleReqVO
     * @return
     */
    RoleRespVO updateRole(Long roleId, RoleReqVO roleReqVO);
}
