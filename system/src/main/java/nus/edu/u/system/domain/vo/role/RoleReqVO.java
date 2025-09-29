package nus.edu.u.system.domain.vo.role;

import java.util.List;
import lombok.Data;

/**
 * @author Lu Shuwen
 * @date 2025-09-25
 */
@Data
public class RoleReqVO {

    private String name;

    private String key;

    private List<Long> permissions;
}
