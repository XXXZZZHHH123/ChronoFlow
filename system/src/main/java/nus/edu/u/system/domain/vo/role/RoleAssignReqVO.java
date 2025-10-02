package nus.edu.u.system.domain.vo.role;

import java.util.List;
import lombok.Data;

/**
 * @author Lu Shuwen
 * @date 2025-09-29
 */
@Data
public class RoleAssignReqVO {

    private Long userId;

    private List<Long> roles;
}
