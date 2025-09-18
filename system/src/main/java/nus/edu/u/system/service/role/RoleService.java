package nus.edu.u.system.service.role;

import java.util.List;
import nus.edu.u.system.domain.vo.role.RoleListRespVO;

public interface RoleService {
    List<RoleListRespVO> listRolesExcludingAdmin();
}
