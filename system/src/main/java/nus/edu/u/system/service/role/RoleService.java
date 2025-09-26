package nus.edu.u.system.service.role;

import java.util.List;
import nus.edu.u.system.domain.vo.role.RoleReqVO;
import nus.edu.u.system.domain.vo.role.RoleRespVO;

public interface RoleService {
    List<RoleRespVO> listRoles();

    RoleRespVO createRole(RoleReqVO roleReqVO);

    RoleRespVO getRole(Long roleId);

    void deleteRole(Long roleId);

    RoleRespVO updateRole(Long roleId, RoleReqVO roleReqVO);
}
