package nus.edu.u.system.domain.vo.role;

import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

/**
 * @author Lu Shuwen
 * @date 2025-09-25
 */
@Data
public class RoleReqVO {

    @Size(min = 1, max = 100, message = "Role name should between 1 and 100")
    private String name;

    @Size(min = 1, max = 50, message = "Role key should between 1 and 50")
    private String key;

    private List<Long> permissions;
}
